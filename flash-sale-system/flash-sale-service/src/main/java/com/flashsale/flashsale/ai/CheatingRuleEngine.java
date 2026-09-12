package com.flashsale.flashsale.ai;

import com.flashsale.flashsale.config.DynamicConfig;
import com.flashsale.flashsale.model.CheatingReport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class CheatingRuleEngine {
    private static final double W_FREQ = 0.5, W_ACCOUNT = 0.4, W_REGULAR = 0.4;
    private static final double W_CONCENTRATED = 0.25;
    private static final double TH_RW_DEFAULT = 30, TH_BK_DEFAULT = 60;

    private final DynamicConfig dynamicConfig;
    private final DeepSeekService deepSeekService;

    // 无参构造保留给单元测试使用（不注入动态配置/DeepSeek 时走默认阈值与纯规则逻辑）
    public CheatingRuleEngine() {
        this.dynamicConfig = null;
        this.deepSeekService = null;
    }

    // 标注 @Autowired 后 Spring 会选用该构造，使 ai.cheating.threshold.review/block 配置与 DeepSeek 增强真正生效
    @Autowired
    public CheatingRuleEngine(DynamicConfig dynamicConfig, DeepSeekService deepSeekService) {
        this.dynamicConfig = dynamicConfig;
        this.deepSeekService = deepSeekService;
    }

    private double reviewThreshold() {
        return dynamicConfig != null ? dynamicConfig.getCheatingReviewThreshold() : TH_RW_DEFAULT;
    }

    private double blockThreshold() {
        return dynamicConfig != null ? dynamicConfig.getCheatingBlockThreshold() : TH_BK_DEFAULT;
    }

    public CheatingReport evaluate(Long userId, String ip, int freqLastSec, int accountCntOnIp, List<Long> ts, int concurrentCount) {
        double rawF = Math.min(freqLastSec * 10.0, 100.0);
        double sF = rawF * W_FREQ;
        double rawA = accountCntOnIp <= 1 ? 0 : Math.min((accountCntOnIp - 1) / 9.0 * 100.0, 100.0);
        double sA = rawA * W_ACCOUNT;
        double std = computeStdDev(ts);
        double rawR = std < 20 ? 80 : std < 100 ? 40 : 0;
        double sR = rawR * W_REGULAR;
        double rawC = concurrentCount > 50 ? 100 : concurrentCount > 20 ? 60 : concurrentCount > 10 ? 30 : 0;
        double sC = rawC * W_CONCENTRATED;
        double total = sF + sA + sR + sC;

        boolean highRisk = total >= blockThreshold();
        boolean review = !highRisk && total >= reviewThreshold();
        String action = highRisk ? "BLOCK" : review ? "REVIEW" : "PASS";
        String reason = highRisk ? String.format("总分%.1f: 高频%.0f+多账号%.0f+规律%.0f+集中%.0f",total,rawF,rawA,rawR,rawC)
                : review ? String.format("可疑(总分%.1f,集中度%.0f)",total,rawC) : String.format("正常(总分%.1f)",total);

        CheatingReport r = new CheatingReport();
        r.setHighRisk(highRisk); r.setReview(review); r.setRiskScore(total);
        r.setScoreFreq(sF); r.setScoreMultiAccount(sA); r.setScoreRegular(sR); r.setScoreConcentrated(sC);
        r.setAction(action); r.setReason(reason);
        return r;
    }

    // ─────────────── DeepSeek 增强（仅 REVIEW 时触发，可配置可降级） ───────────────

    private static final String CHEAT_SYSTEM_PROMPT =
            "你是电商秒杀系统的风控分析助手。请基于给定的规则引擎评分数据，"
            + "用一句简洁的中文说明该用户的作弊风险与处置建议，不要输出多余解释，不要输出JSON。";

    /**
     * 规则引擎打分 + DeepSeek 二次分析。
     *
     * <p>仅当规则判定为 REVIEW（可疑）时才调用 DeepSeek 生成自然语言风险说明并写入 aiAnalysis；
     * DeepSeek 未配置、被禁用或调用失败时返回 null，此时只保留规则引擎结果。
     *
     * <p>注意：秒杀下单主链路用的是 {@link #evaluate}（纯本地计算、不联网），
     * 本方法只供管理端/查询类接口使用，不会给秒杀主流程带来任何网络耗时。
     */
    public CheatingReport evaluateWithAi(Long userId, String ip, int freqLastSec, int accountCntOnIp,
                                         List<Long> ts, int concurrentCount) {
        CheatingReport report = evaluate(userId, ip, freqLastSec, accountCntOnIp, ts, concurrentCount);
        if (!report.isReview() || deepSeekService == null || !deepSeekService.isAvailable()) {
            return report;
        }
        String aiText = deepSeekService.chat(CHEAT_SYSTEM_PROMPT,
                buildCheatPrompt(userId, ip, freqLastSec, accountCntOnIp, concurrentCount, report));
        if (aiText != null && !aiText.isBlank()) {
            report.setAiAnalysis(aiText);
        }
        return report;
    }

    private String buildCheatPrompt(Long userId, String ip, int freqLastSec, int accountCntOnIp,
                                    int concurrentCount, CheatingReport r) {
        return String.format(
                "用户ID=%s，来源IP=%s；最近1秒请求数=%d，同IP账号数=%d，3秒内并发=%d；"
                        + "规则引擎总分=%.1f（高频%.0f/多账号%.0f/规律%.0f/集中%.0f），判定为REVIEW（可疑）。"
                        + "请用一句话给出风险说明与处置建议。",
                userId, ip, freqLastSec, accountCntOnIp, concurrentCount,
                r.getRiskScore(), r.getScoreFreq(), r.getScoreMultiAccount(),
                r.getScoreRegular(), r.getScoreConcentrated());
    }

    /**
     * 为已进入可疑监控列表的用户生成 DeepSeek 风险说明。
     *
     * <p>这些用户在被标记时已被判定为 REVIEW/BLOCK，此处基于其留存的风险分与规则理由直接生成自然语言说明，
     * 用于 AI 防作弊列表页"检测理由"展示。DeepSeek 未启用/未配置/调用失败时返回 null，调用方保留规则 reason 兜底。
     * 只在管理端查询接口调用，秒杀主链路不涉及。
     */
    public String analyzeSuspicious(Long userId, String ip, double riskScore, String reason) {
        if (deepSeekService == null || !deepSeekService.isAvailable()) {
            return null;
        }
        String text = deepSeekService.chat(CHEAT_SYSTEM_PROMPT, String.format(
                "用户ID=%s，来源IP=%s；规则引擎风险总分=%.1f，判定理由=%s。"
                        + "该用户已被列入秒杀可疑名单，请用一句话给出风险说明与处置建议。",
                userId, ip, riskScore, reason));
        return (text != null && !text.isBlank()) ? text : null;
    }

    private double computeStdDev(List<Long> ts) {
        // 数据点不足时无法判定规律性，返回大值视为"非规律"，避免把稀疏请求误判为脚本规律
        if (ts == null || ts.size() < 3) return 1000;
        List<Long> gaps = new ArrayList<>();
        for (int i = 1; i < ts.size(); i++) gaps.add(ts.get(i) - ts.get(i-1));
        double mean = gaps.stream().mapToLong(Long::longValue).average().orElse(0);
        double var = gaps.stream().mapToDouble(g -> Math.pow(g - mean, 2)).average().orElse(0);
        return Math.sqrt(var);
    }
}
