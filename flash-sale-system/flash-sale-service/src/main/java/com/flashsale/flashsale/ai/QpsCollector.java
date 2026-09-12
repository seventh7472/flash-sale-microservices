package com.flashsale.flashsale.ai;

import com.flashsale.flashsale.entity.TrafficMetrics;
import com.flashsale.flashsale.mapper.TrafficMetricsMapper;
import com.flashsale.flashsale.model.TrafficPrediction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RList;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class QpsCollector {

    private static final int MIN_HISTORY_POINTS = 10;
    private static final int TARGET_HISTORY_POINTS = 30;
    private static final int MAX_HISTORY_POINTS = 60;
    private static final int FORECAST_POINTS = 24;
    private static final int SINGLE_INSTANCE_CAPACITY = 200;
    private static final String SCALE_STATUS_KEY = "flash:sale:ai:scale:status:";
    private static final String SCALE_INSTANCES_KEY = "flash:sale:ai:scale:instances:";
    private static final String SCALE_EVENTS_KEY = "flash:sale:ai:scale:events:";
    private static final DateTimeFormatter SCALE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final TrafficMetricsMapper metricsMapper;
    private final TrafficPredictor trafficPredictor;
    private final RedissonClient redissonClient;
    private final DeepSeekService deepSeekService;

    @Value("${flash.sale.ai.scale.capacity:200}")
    private int scaleCapacity;

    // 按 "接口名称|activityId" 统计当前分钟请求数，activityId 为空表示全局指标
    private final Map<String, AtomicInteger> counter = new ConcurrentHashMap<>();

    // SeckillActivity Mapper 用于获取活动的 QPS 限流阈值
    // (直接取 activity 的 qpsLimit 字段, 需注入 SeckillActivityMapper)
    private final com.flashsale.flashsale.mapper.SeckillActivityMapper activityMapper;

    /**
     * 记录一次请求（由 QpsFilter 调用）
     */
    public void recordRequest(String apiName, Long activityId) {
        String key = apiName + "|" + (activityId == null ? "" : activityId);
        counter.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    /**
     * 当前分钟各接口/活动的实时请求计数快照（key 形如 {@code /api/flash-sale/seckill/36|36}）。
     *
     * <p>纯内存只读操作，不修改统计状态、不触发 DB/网络调用，供 AI 风控助手拼装上下文使用；
     * 秒杀主链路与既有接口行为完全不受影响。
     */
    public Map<String, Integer> currentQpsSnapshot() {
        Map<String, Integer> snapshot = new LinkedHashMap<>();
        counter.forEach((k, v) -> {
            int c = v.get();
            if (c > 0) {
                snapshot.put(k, c);
            }
        });
        return snapshot;
    }

    /**
     * 每分钟末: 1) 刷新 QPS 到 DB  2) 对进行中的活动做流量预测 + 预警
     */
    @Scheduled(cron = "0 * * * * ?")
    public void flushAndPredict() {
        Map<String, AtomicInteger> snapshot = new ConcurrentHashMap<>(counter);
        counter.clear();

        // --- 1. 刷入 DB ---
        for (Map.Entry<String, AtomicInteger> entry : snapshot.entrySet()) {
            int count = entry.getValue().get();
            if (count > 0) {
                String key = entry.getKey();
                int sep = key.lastIndexOf('|');
                String apiName = key.substring(0, sep);
                String idPart = key.substring(sep + 1);
                Long activityId = idPart.isEmpty() ? null : Long.valueOf(idPart);
                TrafficMetrics m = new TrafficMetrics();
                m.setInterfaceName(apiName);
                m.setActivityId(activityId);
                m.setQps(count);
                m.setCollectTime(LocalDateTime.now());
                metricsMapper.insert(m);
                log.debug("QPS记录 api={} activityId={} count={}", apiName, activityId, count);
            }
        }

        // --- 2. 对每个进行中的活动做预测 ---
        List<com.flashsale.flashsale.entity.SeckillActivity> activeActivities =
                activityMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.flashsale.flashsale.entity.SeckillActivity>()
                        .eq(com.flashsale.flashsale.entity.SeckillActivity::getStatus, 1)
                        .ge(com.flashsale.flashsale.entity.SeckillActivity::getEndTime, LocalDateTime.now()));

        for (com.flashsale.flashsale.entity.SeckillActivity activity : activeActivities) {
            try {
                doPredictForActivity(activity);
            } catch (Exception e) {
                log.warn("流量预测异常 activityId={}", activity.getId(), e);
            }
        }
    }

    /**
     * 对单个活动执行预测 + 预警检查
     */
    private void doPredictForActivity(com.flashsale.flashsale.entity.SeckillActivity activity) {
        Long activityId = activity.getId();
        // 取最近 N 个时间窗口的 QPS 数据
        List<Integer> recentQps = metricsMapper.findRecentQpsByActivityId(activityId, 10);
        if (recentQps == null || recentQps.isEmpty()) {
            log.debug("活动{}无历史QPS数据, 跳过预测", activityId);
            return;
        }
        // findRecentQpsByActivityId 按 collect_time DESC 返回（最新在前），
        // 而 TrafficPredictor.predict 约定 index 0=最旧、n-1=最新，这里反转为时间升序
        Collections.reverse(recentQps);

        // 从活动配置中读取 QPS 限流阈值 (默认 500)
        int qpsThreshold = activity.getQpsLimit() != null && activity.getQpsLimit() > 0
                ? activity.getQpsLimit() : 500;

        TrafficPredictor.Result result = trafficPredictor.predict(recentQps, qpsThreshold);
        double ratio = qpsThreshold > 0 ? result.predictedPeak / qpsThreshold : 0;

        // 预警日志
        if (ratio >= 0.80) {
            log.warn("【流量预警】活动{} 预测峰值{}QPS, 已达限流阈值{}的{}%, 建议扩容! " +
                            "趋势={}, 推荐实例数={}",
                    activityId, result.predictedPeak, qpsThreshold, ratio * 100,
                    result.trend, result.recommendedInstances);
        } else if (ratio >= 0.60) {
            log.info("【流量关注】活动{} 预测峰值%.1fQPS, 阈值占比%.1f%%, 趋势={}",
                    activityId, result.predictedPeak, ratio * 100, result.trend);
        } else {
            log.debug("活动{} 流量正常, 预测峰值%.1fQPS, 趋势=%s",
                    activityId, result.predictedPeak, result.trend);
        }
    }

    /** 定期清理 7 天前的流量指标，防止 traffic_metrics 无限增长 */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000L)
    public void purgeOldMetrics() {
        try {
            int rows = metricsMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<TrafficMetrics>()
                    .lt(TrafficMetrics::getCollectTime, LocalDateTime.now().minusDays(7)));
            if (rows > 0) log.info("清理 traffic_metrics 旧数据 {} 行", rows);
        } catch (Exception e) {
            log.warn("清理 traffic_metrics 失败", e);
        }
    }

    /**
     * 供管理端/API 调用的流量预测接口
     */
    public TrafficPrediction predictForActivity(Long activityId) {
        com.flashsale.flashsale.entity.SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) {
            TrafficPrediction empty = new TrafficPrediction();
            empty.setActivityId(activityId);
            empty.setWarningLevel("NODATA");
            empty.setScaleStatus("NORMAL");
            empty.setAnalysis("活动不存在或暂无流量数据");
            return empty;
        }

        int qpsThreshold = activity.getQpsLimit() != null && activity.getQpsLimit() > 0
                ? activity.getQpsLimit() : 500;

        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<TrafficMetrics> raw = metricsMapper.findRecentMetricsByActivity(activityId, since);
        boolean usedGlobalFallback = raw.isEmpty();
        if (usedGlobalFallback) {
            raw = metricsMapper.findRecentMetricsGlobal(since);
        }
        List<TrafficMetrics> history = normalizeHistory(raw, qpsThreshold);
        boolean demoDataUsed = raw.size() < MIN_HISTORY_POINTS;

        String dataSource;
        if (usedGlobalFallback && raw.isEmpty()) {
            dataSource = "该活动与全站均无流量数据，使用演示补全序列";
        } else if (usedGlobalFallback) {
            dataSource = "该活动暂无流量数据，已回退使用全站数据" + (demoDataUsed ? "（不足10条，补全演示数据）" : "");
        } else if (raw.isEmpty()) {
            dataSource = "该活动暂无流量数据，使用演示补全序列";
        } else {
            dataSource = "该活动最近24小时真实数据" + (demoDataUsed ? "（不足10条，已补全演示数据）" : "");
        }

        List<Integer> recentQps = new ArrayList<>();
        for (TrafficMetrics m : history) {
            recentQps.add(m.getQps());
        }
        double slope = linearSlope(recentQps);
        double avg = average(recentQps);
        double slopeRate = avg > 0 ? slope / avg : 0;
        String trend = slopeRate > 0.08 ? "RISING" : slopeRate < -0.08 ? "FALLING" : "STABLE";
        int last = recentQps.get(recentQps.size() - 1);
        int predictedQps = Math.max(1, (int) Math.round(last + slope * (TARGET_HISTORY_POINTS / 3.0)));
        double peakFactor = "RISING".equals(trend) ? 1.5 : "STABLE".equals(trend) ? 1.2 : 1.0;
        int predictedPeakQps = Math.max(predictedQps, (int) Math.round(predictedQps * peakFactor));

        List<Integer> forecast = new ArrayList<>();
        for (int i = 1; i <= FORECAST_POINTS; i++) {
            forecast.add(Math.max(1, (int) Math.round(last + slope * 60 * i)));
        }
        for (int v : forecast) {
            predictedPeakQps = Math.max(predictedPeakQps, v);
        }

        int effectiveCapacity = scaleCapacity > 0 ? scaleCapacity : SINGLE_INSTANCE_CAPACITY;
        int computedInstances = Math.max(1, (int) Math.ceil(last / (double) effectiveCapacity));
        Integer scaledInstances = redissonClient.<Integer>getBucket(SCALE_INSTANCES_KEY + activityId).get();
        int currentInstances = Math.max(computedInstances, scaledInstances == null ? 0 : scaledInstances);
        int recommendedInstances = Math.max(currentInstances,
                (int) Math.ceil(predictedPeakQps / (double) effectiveCapacity));
        int confidence = raw.isEmpty() ? 75 : Math.min(95, 60 + raw.size() * 2);
        if (demoDataUsed) {
            confidence = Math.min(confidence, 80);
        }

        double ratio = qpsThreshold > 0 ? (predictedPeakQps / 60.0) / qpsThreshold : 0;
        String warningLevel = ratio >= 0.95 ? "RED" : ratio >= 0.80 ? "ORANGE" : ratio >= 0.60 ? "YELLOW" : "GREEN";

        int capacityTotal = currentInstances * effectiveCapacity;
        String scaleStatus = "NORMAL";
        String scaleTime = null;
        if (predictedPeakQps > capacityTotal) {
            int targetInstances = Math.max(recommendedInstances, currentInstances + 1);
            scaleStatus = "SCALED";
            scaleTime = LocalDateTime.now().format(SCALE_TIME_FORMAT);
            simulateScale(activityId, currentInstances, targetInstances, predictedPeakQps);
        } else if (predictedPeakQps > capacityTotal * 0.8) {
            scaleStatus = "SUGGEST";
        }

        TrafficPrediction tp = new TrafficPrediction();
        tp.setActivityId(activityId);
        tp.setPredictedQps(predictedQps);
        tp.setPredictedPeakQps(predictedPeakQps);
        tp.setCurrentInstances(currentInstances);
        tp.setRecommendedInstances(recommendedInstances);
        tp.setConfidence(confidence);
        tp.setTrend(trend);
        tp.setWarningLevel(warningLevel);
        tp.setDataSource(dataSource);
        tp.setScaleStatus(scaleStatus);
        tp.setScaleTime(scaleTime);
        tp.setAnalysis(String.format("基于%s最近%d条QPS计算(真实%d条): 历史均值%.1f 趋势:%s 预测QPS:%d 预测峰值:%dQPS 当前实例数:%d 推荐实例数:%d 置信度:%d%%",
                dataSource, history.size(), raw.size(), avg, trend, predictedQps, predictedPeakQps,
                currentInstances, recommendedInstances, confidence));

        // DeepSeek 生成自然语言解读；不可用时回退到规则模板（保证 textAnalysis 始终有值）
        tp.setTextAnalysis(buildTextAnalysis(tp));

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm");
        for (int i = 0; i < history.size(); i++) {
            TrafficMetrics m = history.get(i);
            tp.getTimeSeries().add(new TrafficPrediction.TrafficPoint(
                    m.getCollectTime().format(fmt), m.getQps(), i == history.size() - 1 ? last : null));
        }
        LocalDateTime cursor = history.get(history.size() - 1).getCollectTime().plusHours(1);
        for (int v : forecast) {
            tp.getTimeSeries().add(new TrafficPrediction.TrafficPoint(cursor.format(fmt), null, v));
            cursor = cursor.plusHours(1);
        }
        return tp;
    }

    // ─────────────── DeepSeek 自然语言解读（可配置、可降级） ───────────────

    private static final String TRAFFIC_SYSTEM_PROMPT =
            "你是电商秒杀系统的容量规划助手。请基于给定的流量预测数据，"
            + "用一句简洁的中文说明未来流量趋势与扩容建议，不要输出多余解释，不要输出JSON。";

    /**
     * 生成预测结果的自然语言解读：优先 DeepSeek，不可用时回退到规则模板。
     *
     * <p>仅在管理端主动调用预测接口时执行，分钟级定时预测任务不调用，避免给后台任务引入网络耗时。
     */
    private String buildTextAnalysis(TrafficPrediction tp) {
        String fallback = ruleBasedAnalysis(tp);
        if (deepSeekService == null || !deepSeekService.isAvailable()) {
            return fallback;
        }
        String aiText = deepSeekService.chat(TRAFFIC_SYSTEM_PROMPT, buildTrafficPrompt(tp));
        return (aiText == null || aiText.isBlank()) ? fallback : aiText;
    }

    private String buildTrafficPrompt(TrafficPrediction tp) {
        return String.format(
                "活动ID=%s；历史流量趋势=%s（RISING上升/FALLING下降/STABLE平稳）；预测QPS=%d；预测峰值QPS=%d；"
                        + "当前实例数=%d；推荐实例数=%d；预警级别=%s；扩容状态=%s。"
                        + "请用一句话说明趋势并给出扩容建议。",
                tp.getActivityId(), tp.getTrend(), tp.getPredictedQps(), tp.getPredictedPeakQps(),
                tp.getCurrentInstances(), tp.getRecommendedInstances(),
                tp.getWarningLevel(), tp.getScaleStatus());
    }

    /** DeepSeek 不可用时的规则模板解读 */
    private String ruleBasedAnalysis(TrafficPrediction tp) {
        String trendText = "RISING".equals(tp.getTrend()) ? "上升"
                : "FALLING".equals(tp.getTrend()) ? "下降" : "平稳";
        StringBuilder sb = new StringBuilder();
        sb.append("未来流量呈").append(trendText).append("趋势，预测峰值 ")
                .append(tp.getPredictedPeakQps()).append(" QPS");
        if (tp.getRecommendedInstances() > tp.getCurrentInstances()) {
            sb.append("，建议扩容至 ").append(tp.getRecommendedInstances()).append(" 个实例。");
        } else {
            sb.append("，当前 ").append(tp.getCurrentInstances()).append(" 个实例可满足需求。");
        }
        return sb.toString();
    }

    private void simulateScale(Long activityId, int fromInstances, int toInstances, int peakQps) {
        String now = LocalDateTime.now().format(SCALE_TIME_FORMAT);
        String statusJson = String.format(
                "{\"scaleStatus\":\"SCALED\",\"currentInstances\":%d,\"scaleTime\":\"%s\"}",
                toInstances, now);
        redissonClient.getBucket(SCALE_STATUS_KEY + activityId).set(statusJson, 24, TimeUnit.HOURS);
        redissonClient.getBucket(SCALE_INSTANCES_KEY + activityId).set(toInstances, 24, TimeUnit.HOURS);

        RList<String> events = redissonClient.getList(SCALE_EVENTS_KEY + activityId);
        events.add(String.format(
                "[%s] 活动%s 模拟扩容: %d -> %d 实例, 触发原因: 预测峰值%dQPS 超过当前容量",
                now, activityId, fromInstances, toInstances, peakQps));
        while (events.size() > 20) {
            events.remove(0);
        }
        log.warn("【模拟扩容】活动{} 原实例数{} -> 目标实例数{}, 预测峰值{}QPS",
                activityId, fromInstances, toInstances, peakQps);
    }

    private List<TrafficMetrics> normalizeHistory(List<TrafficMetrics> raw, int qpsThreshold) {
        Map<LocalDateTime, Integer> byMinute = new TreeMap<>();
        for (TrafficMetrics m : raw) {
            if (m.getQps() == null || m.getCollectTime() == null) {
                continue;
            }
            LocalDateTime key = m.getCollectTime().withSecond(0).withNano(0);
            byMinute.merge(key, Math.max(0, m.getQps()), Integer::sum);
        }

        List<TrafficMetrics> aggregated = new ArrayList<>();
        for (Map.Entry<LocalDateTime, Integer> entry : byMinute.entrySet()) {
            TrafficMetrics m = new TrafficMetrics();
            m.setCollectTime(entry.getKey());
            m.setQps(entry.getValue());
            aggregated.add(m);
        }

        if (aggregated.size() >= MIN_HISTORY_POINTS) {
            if (aggregated.size() > MAX_HISTORY_POINTS) {
                return new ArrayList<>(aggregated.subList(aggregated.size() - MAX_HISTORY_POINTS, aggregated.size()));
            }
            return aggregated;
        }
        return completeDemoHistory(aggregated, qpsThreshold);
    }

    private List<TrafficMetrics> completeDemoHistory(List<TrafficMetrics> real, int qpsThreshold) {
        double avg = averageQps(real);
        if (avg <= 0) {
            avg = Math.max(5, qpsThreshold / 10.0);
        }

        List<TrafficMetrics> result = new ArrayList<>();
        if (real.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            for (int i = TARGET_HISTORY_POINTS - 1; i >= 0; i--) {
                result.add(demoPoint(now.minusHours(i), syntheticQps(avg, TARGET_HISTORY_POINTS - i)));
            }
            return result;
        }

        int missing = TARGET_HISTORY_POINTS - real.size();
        LocalDateTime anchor = real.get(0).getCollectTime();
        for (int i = missing; i >= 1; i--) {
            result.add(demoPoint(anchor.minusHours(i), syntheticQps(avg, missing - i + 1)));
        }
        result.addAll(real);
        return result;
    }

    private TrafficMetrics demoPoint(LocalDateTime time, int qps) {
        TrafficMetrics m = new TrafficMetrics();
        m.setCollectTime(time);
        m.setQps(qps);
        return m;
    }

    private int syntheticQps(double base, int index) {
        double wave = 0.75 + 0.5 * Math.sin(index * 1.3);
        double bump = index % 7 == 3 ? 1.35 : 1.0;
        return Math.max(1, (int) Math.round(base * wave * bump));
    }

    private double averageQps(List<TrafficMetrics> metrics) {
        if (metrics == null || metrics.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (TrafficMetrics m : metrics) {
            sum += m.getQps();
        }
        return sum / metrics.size();
    }

    private double average(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return 0;
        }
        double sum = 0;
        for (int v : values) {
            sum += v;
        }
        return sum / values.size();
    }

    private double linearSlope(List<Integer> values) {
        int n = values.size();
        if (n < 2) {
            return 0;
        }
        double sumX = 0, sumY = 0, sumXY = 0, sumXX = 0;
        for (int i = 0; i < n; i++) {
            double y = values.get(i);
            sumX += i;
            sumY += y;
            sumXY += i * y;
            sumXX += (double) i * i;
        }
        double denom = n * sumXX - sumX * sumX;
        if (denom == 0) {
            return 0;
        }
        return (n * sumXY - sumX * sumY) / denom;
    }
}
