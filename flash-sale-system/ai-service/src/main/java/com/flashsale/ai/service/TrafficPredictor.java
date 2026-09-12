 package com.flashsale.ai.service;

 import org.springframework.stereotype.Component;

 import java.util.List;

 @Component
 public class TrafficPredictor {

     public enum WarningLevel { GREEN, YELLOW, ORANGE, RED }

     public static class Result {
         public final double predictedQps;
         public final double predictedPeak;
         public final String trend;
         public final WarningLevel warningLevel;
         public final int recommendedInstances;
         public final int[] recentBuckets;

         public Result(double predictedQps, double predictedPeak, String trend,
                       WarningLevel warningLevel, int recommendedInstances, int[] recentBuckets) {
             this.predictedQps = predictedQps;
             this.predictedPeak = predictedPeak;
             this.trend = trend;
             this.warningLevel = warningLevel;
             this.recommendedInstances = recommendedInstances;
             this.recentBuckets = recentBuckets;
         }
     }

     /**
      * 基于最近N分钟的请求数（次/分钟）预测流量
      *
      * @param minuteBuckets 最近N分钟的请求数列表（index 0=最旧, index n-1=最新）
      * @param qpsThreshold  Sentinel 限流阈值（QPS）
      */
     public Result predict(List<Integer> minuteBuckets, int qpsThreshold) {
         if (minuteBuckets == null || minuteBuckets.isEmpty()) {
             return new Result(0, 0, "NODATA", WarningLevel.GREEN, 1, new int[0]);
         }

         int n = minuteBuckets.size();
         int last = minuteBuckets.get(n - 1);

         // Q1 = 最近1分钟平均QPS
         double q1 = last / 60.0;

         // Q5 = 最近5分钟平均QPS
         int c5 = Math.min(5, n);
         int s5 = 0;
         for (int i = n - c5; i < n; i++) s5 += minuteBuckets.get(i);
         double q5 = s5 / (c5 * 60.0);

         // Q15 = 最近N分钟平均QPS
         int sN = 0;
         for (int v : minuteBuckets) sN += v;
         double qN = sN / (n * 60.0);

         // 加权预测QPS
         double predictedQps = q1 * 0.6 + q5 * 0.3 + qN * 0.1;

         // 趋势
         String trend;
         if (q5 > 0 && q1 > q5 * 1.2) trend = "RISING";
         else if (q5 > 0 && q1 < q5 * 0.8) trend = "FALLING";
         else trend = "STABLE";

         // 安全系数
         double sf;
         switch (trend) {
             case "RISING":  sf = 1.5; break;
             case "FALLING": sf = 1.0; break;
             default:        sf = 1.2;
         }

         double predictedPeak = predictedQps * sf;
         double ratio = qpsThreshold > 0 ? predictedPeak / qpsThreshold : 0;

         // 预警等级
         WarningLevel level;
         if (ratio >= 0.95) level = WarningLevel.RED;
         else if (ratio >= 0.80) level = WarningLevel.ORANGE;
         else if (ratio >= 0.60) level = WarningLevel.YELLOW;
         else level = WarningLevel.GREEN;

         int instances = (int) Math.ceil(predictedPeak / 50.0);
         if (instances < 1) instances = 1;

         int[] arr = minuteBuckets.stream().mapToInt(i -> i).toArray();
         return new Result(predictedQps, predictedPeak, trend, level, instances, arr);
     }
 }
