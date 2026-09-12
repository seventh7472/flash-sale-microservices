 package com.flashsale.ai.service;

 import org.junit.jupiter.api.Test;

 import java.util.List;

 import static org.junit.jupiter.api.Assertions.*;

 class TrafficPredictorTest {

     private final TrafficPredictor predictor = new TrafficPredictor();

     @Test
     void testNormalPeak_ReasonablePrediction() {
         // 典型秒杀峰形：从600上升到4800(80QPS)后回落至600(10QPS)
         // Q1=10, Q5=30, Q15=50.7 → 预测=20.07, FALLING, 安全系数1.0, 峰值=20.07, GREEN
         List<Integer> buckets = List.of(
                 600,1200,1800,2400,3000,3600,4200,4800,
                 4200,3600,3000,2400,1800,1200,600);
         int threshold = 100;

         TrafficPredictor.Result r = predictor.predict(buckets, threshold);

         assertTrue(r.predictedQps <= 80 && r.predictedQps > 0);
         assertEquals("FALLING", r.trend);
         assertEquals(TrafficPredictor.WarningLevel.GREEN, r.warningLevel);
         assertTrue(r.recommendedInstances >= 1);
     }

     @Test
     void testRapidRise_TriggersAlert() {
         // 前10分钟平稳→后5分钟急剧拉升到9000/分钟(150QPS), 远超100阈值
         // Q1=150, Q5=100, Q1>Q5×1.2=120 → RISING
         // 安全系数1.5, 预测=125.5, 峰值=188.25, 比率=1.88 → RED
         List<Integer> buckets = List.of(
                 1200,1200,1200,1200,1200,
                 1200,1200,1200,1200,1200,
                 3000,4500,6000,7500,9000);
         int threshold = 100;

         TrafficPredictor.Result r = predictor.predict(buckets, threshold);

         assertEquals("RISING", r.trend);
         assertTrue(r.warningLevel == TrafficPredictor.WarningLevel.ORANGE
                 || r.warningLevel == TrafficPredictor.WarningLevel.RED,
                 "应触发预警, 实际=" + r.warningLevel);
     }

     @Test
     void testStableNormal_NoWarning() {
         // 稳定在20~30QPS, 远低于100阈值
         List<Integer> buckets = List.of(
                 1200,1300,1250,1400,1350,
                 1500,1450,1600,1550,1700,
                 1650,1800,1750,1700,1600);
         int threshold = 100;

         TrafficPredictor.Result r = predictor.predict(buckets, threshold);

         assertTrue(r.predictedPeak / threshold < 0.6);
         assertEquals(TrafficPredictor.WarningLevel.GREEN, r.warningLevel);
         assertEquals("STABLE", r.trend);
     }
 }
