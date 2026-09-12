 package com.flashsale.ai.service;

 import com.flashsale.ai.model.CheatingReport;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.stereotype.Component;

 import java.util.ArrayList;
 import java.util.List;

 /**
  * 防作弊规则引擎（无状态，纯计算）
  * 输入：请求频率 / 同IP账号数 / 请求时间戳序列
  * 输出：CheatingReport（含各维度分 + 总分 + 动作）
  *
  * 权重：高频×40% + 多账号×30% + 规律间隔×30%
  * 阈值：<30 PASS / 30~60 REVIEW / >60 BLOCK
  */
 @Slf4j
 @Component
 public class CheatingRuleEngine {

     private static final double W_FREQ = 0.4;
     private static final double W_ACCOUNT = 0.3;
     private static final double W_REGULAR = 0.3;

     private static final double THRESHOLD_REVIEW = 30;
     private static final double THRESHOLD_BLOCK = 60;

     /**
      * 评估请求是否为作弊行为
      */
     public CheatingReport evaluate(Long userId, String ip,
                                     int freqLastSecond,
                                     int accountCountOnIp,
                                     List<Long> recentTimestampsMs) {
         // 1. 高频请求分
         double rawFreq = Math.min(freqLastSecond * 10.0, 100.0);
         double scoreFreq = rawFreq * W_FREQ;

         // 2. 同IP多账号分
         double rawAccount = accountCountOnIp <= 1 ? 0
                 : Math.min((accountCountOnIp - 1) / 9.0 * 100.0, 100.0);
         double scoreAccount = rawAccount * W_ACCOUNT;

         // 3. 规律间隔分
         double stdDev = computeStdDev(recentTimestampsMs);
         double rawRegular = stdDev < 20 ? 80 : stdDev < 100 ? 40 : 0;
         double scoreRegular = rawRegular * W_REGULAR;

         // 4. 总分
         double total = scoreFreq + scoreAccount + scoreRegular;

         // 5. 判定动作
         String action;
         boolean highRisk = false;
         boolean review = false;
         String reason;

         if (total >= THRESHOLD_BLOCK) {
             highRisk = true;
             action = "BLOCK";
             reason = String.format("总分%.1f: 高频%.0f+多账号%.0f+规律%.0f",
                     total, rawFreq, rawAccount, rawRegular);
         } else if (total >= THRESHOLD_REVIEW) {
             review = true;
             action = "REVIEW";
             reason = String.format("总分%.1f(可疑): 高频%.0f+多账号%.0f+规律%.0f",
                     total, rawFreq, rawAccount, rawRegular);
         } else {
             action = "PASS";
             reason = String.format("正常(总分%.1f)", total);
         }

         CheatingReport report = new CheatingReport();
         report.setHighRisk(highRisk);
         report.setReview(review);
         report.setRiskScore(total);
         report.setScoreFreq(scoreFreq);
         report.setScoreMultiAccount(scoreAccount);
         report.setScoreRegular(scoreRegular);
         report.setAction(action);
         report.setReason(reason);

         if (log.isDebugEnabled()) {
             log.debug("防作弊评估: userId={}, ip={}, freq={}, accounts={}, std={}, total={}, action={}",
                     userId, ip, freqLastSecond, accountCountOnIp, String.format("%.1f", stdDev), total, action);
         }

         return report;
     }

     /** 计算时间间隔的标准差（毫秒） */
     private double computeStdDev(List<Long> timestampsMs) {
         if (timestampsMs == null || timestampsMs.size() < 3) return 0;

         List<Long> gaps = new ArrayList<>();
         for (int i = 1; i < timestampsMs.size(); i++) {
             gaps.add(timestampsMs.get(i) - timestampsMs.get(i - 1));
         }
         if (gaps.isEmpty()) return 0;

         double mean = gaps.stream().mapToLong(Long::longValue).average().orElse(0);
         double variance = gaps.stream()
                 .mapToDouble(g -> Math.pow(g - mean, 2))
                 .average()
                 .orElse(0);
         return Math.sqrt(variance);
     }
 }
