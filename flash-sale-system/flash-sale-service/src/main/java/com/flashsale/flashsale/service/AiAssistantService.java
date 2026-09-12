package com.flashsale.flashsale.service;

import com.flashsale.flashsale.ai.DeepSeekService;
import com.flashsale.flashsale.ai.QpsCollector;
import com.flashsale.flashsale.model.ChatReply;
import com.flashsale.flashsale.model.CheatUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 风控助手：把「当前可疑用户 + 当前系统 QPS」拼成上下文，交给 DeepSeek 回答管理员问题。
 *
 * <p>设计约束：
 * <ul>
 *   <li><b>只读</b>：只读取 Redis 中已有的可疑用户缓存与内存中的 QPS 计数，不写任何业务数据；</li>
 *   <li><b>可降级</b>：DeepSeek 未启用 / 未配置 / 超时 / 鉴权失败时，返回基于同一份实时数据拼装的
 *       降级摘要，接口本身始终返回成功，不影响任何已有接口；</li>
 *   <li><b>不碰主链路</b>：秒杀下单流程完全不调用本类。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAssistantService {

    /** 与 {@code CheatMonitorService} 共用同一份 Redis 可疑用户缓存 */
    private static final String SUSPICIOUS_KEY = "cheat:suspicious";

    /** 上下文中携带的可疑用户条数上限（需求：前 5 条） */
    private static final int SUSPICIOUS_LIMIT = 5;

    /** 上下文中 QPS 明细展示的接口条数上限 */
    private static final int QPS_DETAIL_LIMIT = 8;

    /** 需求指定的 system 提示词 */
    private static final String SYSTEM_PROMPT =
            "你是秒杀系统的风控助手，基于提供的数据回答管理员问题，回答简洁，给出明确建议。";

    private final RedissonClient redisson;
    private final QpsCollector qpsCollector;
    private final DeepSeekService deepSeekService;

    /**
     * 回答管理员的一次自然语言提问。
     *
     * @return 回答正文 + 是否降级；DeepSeek 不可用时返回降级摘要，不抛出异常
     */
    public ChatReply ask(String question) {
        String q = (question == null || question.isBlank())
                ? "请分析当前系统的风控状况并给出处置建议"
                : question.trim();

        List<CheatUser> all = sortedSuspicious();
        List<CheatUser> top = all.size() > SUSPICIOUS_LIMIT
                ? new ArrayList<>(all.subList(0, SUSPICIOUS_LIMIT))
                : all;
        Map<String, Integer> qps = currentQps();

        String prompt = buildContext(top, all.size(), qps) + "\n管理员问题：" + q;
        String answer = deepSeekService == null ? null : deepSeekService.chat(SYSTEM_PROMPT, prompt);

        if (answer != null && !answer.isBlank()) {
            return new ChatReply(answer, false);
        }
        log.warn("AI 风控助手降级回答: question={}, 可疑用户={}, QPS接口数={}", q, all.size(), qps.size());
        return new ChatReply(buildFallback(top, all.size(), qps), true);
    }

    // ─────────────── 上下文数据读取（全部只读、可容错） ───────────────

    /** 读取可疑用户缓存并按风险分降序排列；异常时返回空列表，保证对话接口不因数据层问题失败 */
    private List<CheatUser> sortedSuspicious() {
        List<CheatUser> list = new ArrayList<>();
        try {
            list.addAll(redisson.<Long, CheatUser>getMapCache(SUSPICIOUS_KEY).values());
        } catch (Exception e) {
            log.warn("读取可疑用户缓存失败，按空列表处理: {}", e.getMessage());
            return list;
        }
        list.sort(Comparator.comparingInt(
                (CheatUser u) -> u.getRiskScore() == null ? 0 : u.getRiskScore()).reversed());
        return list;
    }

    /** 当前分钟实时请求计数快照（内存计数，不触发任何网络/DB 操作） */
    private Map<String, Integer> currentQps() {
        try {
            return qpsCollector == null ? new LinkedHashMap<>() : qpsCollector.currentQpsSnapshot();
        } catch (Exception e) {
            log.warn("读取实时QPS失败，按空数据处理: {}", e.getMessage());
            return new LinkedHashMap<>();
        }
    }

    // ─────────────── 提示词与降级文案拼装 ───────────────

    private String buildContext(List<CheatUser> top, int suspiciousTotal, Map<String, Integer> qps) {
        StringBuilder sb = new StringBuilder();
        sb.append("【当前可疑用户（按风险分降序，最多 ").append(SUSPICIOUS_LIMIT).append(" 条）】\n");
        if (top.isEmpty()) {
            sb.append("暂无\n");
        } else {
            for (int i = 0; i < top.size(); i++) {
                CheatUser u = top.get(i);
                sb.append(i + 1).append(". 用户ID=").append(u.getUserId())
                        .append("，IP=").append(orUnknown(u.getIp()))
                        .append("，风险分=").append(u.getRiskScore() == null ? 0 : u.getRiskScore())
                        .append("，状态=").append(Boolean.TRUE.equals(u.getBanned()) ? "已封禁" : "未封禁")
                        .append("，规则理由=").append(orUnknown(u.getReason()))
                        .append('\n');
            }
            sb.append("可疑用户总数=").append(suspiciousTotal).append('\n');
        }

        sb.append("\n【当前系统 QPS（本分钟实时请求计数）】\n");
        if (qps.isEmpty()) {
            sb.append("本分钟暂无请求\n");
        } else {
            int total = 0;
            int shown = 0;
            for (Map.Entry<String, Integer> e : qps.entrySet()) {
                int v = e.getValue() == null ? 0 : e.getValue();
                total += v;
                if (shown++ < QPS_DETAIL_LIMIT) {
                    sb.append("  ").append(e.getKey()).append(" -> ").append(v).append('\n');
                }
            }
            sb.append("本分钟请求总数=").append(total).append('\n');
        }
        return sb.toString();
    }

    /** DeepSeek 不可用时的降级文案：明确告知降级，并给出可用的实时数据摘要与建议 */
    private String buildFallback(List<CheatUser> top, int suspiciousTotal, Map<String, Integer> qps) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI 助手暂不可用（DeepSeek 未配置、未启用或调用失败），已返回降级的实时数据摘要：\n");
        if (top.isEmpty()) {
            sb.append("· 当前无可疑用户\n");
        } else {
            CheatUser first = top.get(0);
            sb.append("· 可疑用户共 ").append(suspiciousTotal).append(" 人；风险最高：用户ID=")
                    .append(first.getUserId())
                    .append("（风险分=").append(first.getRiskScore() == null ? 0 : first.getRiskScore())
                    .append("，IP=").append(orUnknown(first.getIp())).append("）\n");
        }
        int total = qps.values().stream().filter(v -> v != null).mapToInt(Integer::intValue).sum();
        sb.append("· 本分钟系统请求总数：").append(total).append('\n');
        sb.append("建议：先检查 DeepSeek api-key 配置与网络后重试；急需处置时可依据上表风险分人工封禁高危用户。");
        return sb.toString();
    }

    private String orUnknown(String value) {
        return (value == null || value.isBlank()) ? "未知" : value;
    }
}
