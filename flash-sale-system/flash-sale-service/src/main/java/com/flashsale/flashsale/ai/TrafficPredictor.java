 package com.flashsale.flashsale.ai;

 import lombok.Getter;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.stereotype.Component;
 import java.util.List;
 
 @Component
 public class TrafficPredictor {
     public enum WarningLevel { GREEN, YELLOW, ORANGE, RED }

     // 单实例容量：与 QpsCollector 共用同一配置项，避免两处口径不一致
     @Value("${flash.sale.ai.scale.capacity:200}")
     private int scaleCapacity;

     @Getter
     public static class Result {
         public final double predictedQps, predictedPeak;
         public final String trend;
         public final WarningLevel warningLevel;
         public final int recommendedInstances;
         public final int[] recentBuckets;
         public Result(double pq, double pp, String t, WarningLevel w, int ri, int[] rb) {
             predictedQps=pq; predictedPeak=pp; trend=t; warningLevel=w; recommendedInstances=ri; recentBuckets=rb;
         }
     }

     public Result predict(List<Integer> buckets, int qpsThreshold) {
         if (buckets == null || buckets.isEmpty())
             return new Result(0, 0, "NODATA", WarningLevel.GREEN, 1, new int[0]);
         int n = buckets.size(), last = buckets.get(n-1);
         double q1 = last / 60.0;
         int c5 = Math.min(5, n), s5 = 0;
         for (int i = n-c5; i < n; i++) s5 += buckets.get(i);
         double q5 = s5 / (c5 * 60.0);
         int sN = 0; for (int v : buckets) sN += v;
         double qN = sN / (n * 60.0);
         double pred = q1*0.6 + q5*0.3 + qN*0.1;
         String trend;
         if (q5 > 0 && q1 > q5*1.2) trend = "RISING";
         else if (q5 > 0 && q1 < q5*0.8) trend = "FALLING";
         else trend = "STABLE";
         double sf = "RISING".equals(trend) ? 1.5 : "FALLING".equals(trend) ? 1.0 : 1.2;
         double peak = pred * sf;
         double ratio = qpsThreshold > 0 ? peak / qpsThreshold : 0;
         WarningLevel lv = ratio >= 0.95 ? WarningLevel.RED : ratio >= 0.80 ? WarningLevel.ORANGE : ratio >= 0.60 ? WarningLevel.YELLOW : WarningLevel.GREEN;
         int capacity = Math.max(1, scaleCapacity);
         int inst = Math.max(1, (int)Math.ceil(peak / (double) capacity));
         return new Result(pred, peak, trend, lv, inst, buckets.stream().mapToInt(i->i).toArray());
     }
 }
