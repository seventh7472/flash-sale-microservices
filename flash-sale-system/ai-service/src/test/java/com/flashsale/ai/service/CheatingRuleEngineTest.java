 package com.flashsale.ai.service;

 import com.flashsale.ai.model.CheatingReport;
 import org.junit.jupiter.api.Test;

 import java.util.List;

 import static org.junit.jupiter.api.Assertions.*;

 /**
  * 防作弊规则引擎测试
  *
  * 打分公式（Step A 确定）：
  *   总分 = 高频×40% + 同IP多账号×30% + 规律间隔×30%
  *   < 30 → PASS   30~60 → REVIEW   > 60 → BLOCK
  *
  * 测试数据与期望值见每个测试方法的注释
  */
 class CheatingRuleEngineTest {

     private final CheatingRuleEngine engine = new CheatingRuleEngine();

     // =====================================================================
     // 场景1：正常用户 — 间隔随机、频率正常、单IP单账号
     // =====================================================================
     @Test
     void testNormalUser_NotFalselyFlagged() {
         // ── 模拟数据 ──
         // 5 次请求分布在 ~1.2 秒内，间隔不规则（人工点击特征）
         // 时间戳（ms）: 0, 200, 450, 750, 1200
         // 时间差（ms）:   200, 250, 300, 450   ← 有明显波动，非机器规律
         // 最近 1 秒内请求数: 3（200, 450, 750 → 都落在最后 1s 窗口内）
         // 同 IP 账号数: 1（正常家庭宽带）
         int freqLastSecond = 3;
         int accountCountOnIp = 1;
         List<Long> timestamps = List.of(0L, 200L, 450L, 750L, 1200L);

         // ── 期望计算 ──
         // 高频分 = min(3×10, 100) = 30 → 加权 30×0.4 = 12
         // 多账号分 = 0（仅 1 个账号） → 加权 0
         // 规律分: 间隔 [200,250,300,450], 均值=300, 方差=8750
         //         std≈93.5, 40<93.5<100 → 规律分=40 → 加权 40×0.3 = 12
         // 总分 = 12 + 0 + 12 = 24  →  < 30 → PASS
         double expectedTotal = 24.0;
         double expectedFreq = 12.0;
         double expectedAccount = 0.0;
         double expectedRegular = 12.0;

         CheatingReport report = engine.evaluate(100L, "192.168.1.1",
                 freqLastSecond, accountCountOnIp, timestamps);

         assertEquals(expectedFreq, report.getScoreFreq(), 2.0, "高频分偏差");
         assertEquals(expectedAccount, report.getScoreMultiAccount(), 1.0, "多账号分偏差");
         assertEquals(expectedRegular, report.getScoreRegular(), 2.0, "规律分偏差");
         assertEquals(expectedTotal, report.getRiskScore(), 3.0, "总分偏差");
         assertFalse(report.isHighRisk(), "正常用户不应标记为高风险");
         assertFalse(report.isReview(), "正常用户不应标记待审核");
         assertEquals("PASS", report.getAction());
     }

     // =====================================================================
     // 场景2：刷单用户 — 1秒内20次请求、15个账号共用同IP、间隔恒定50ms
     // =====================================================================
     @Test
     void testAbnormalUser_Blocked() {
         // ── 模拟数据 ──
         // 20 次请求在 1 秒内，每 50ms 一次（脚本刷单特征）
         // 最近 1 秒内请求数: 20
         // 同 IP 账号数: 15（代理池刷单）
         // 时间戳: 0,50,100,...,950（共 20 个）
         int freqLastSecond = 20;
         int accountCountOnIp = 15;
         List<Long> timestamps = List.of(
                 0L, 50L, 100L, 150L, 200L, 250L, 300L, 350L, 400L, 450L,
                 500L, 550L, 600L, 650L, 700L, 750L, 800L, 850L, 900L, 950L);

         // ── 期望计算 ──
         // 高频分 = min(20×10, 100) = 100 → 加权 100×0.4 = 40
         // 多账号分 = min((15-1)/9×100, 100) = 100 → 加权 100×0.3 = 30
         // 规律分: 间隔全 50ms, std≈0 → 规律分=80 → 加权 80×0.3 = 24
         // 总分 = 40 + 30 + 24 = 94  →  > 60 → BLOCK

         CheatingReport report = engine.evaluate(200L, "10.0.0.1",
                 freqLastSecond, accountCountOnIp, timestamps);

         assertTrue(report.getRiskScore() > 60, "总分应 > 60，实际=" + report.getRiskScore());
         assertTrue(report.isHighRisk(), "刷单用户应标记为高风险");
         assertEquals("BLOCK", report.getAction());
         assertNotNull(report.getReason(), "应包含拦截原因");
     }

     // =====================================================================
     // 场景3：边界 case — 低频率+高规律性+2个账号同IP
     // =====================================================================
     @Test
     void testBoundaryCase_Review() {
         // ── 模拟数据 ──
         // 4 次请求在 1.5 秒内（2 req/s），但间隔完全恒定（500ms）
         // 最近 1 秒内请求数: 2（最后 1s 内有 2 次）
         // 同 IP 账号数: 2（家庭多个账号，但行为异常规律）
         // 时间戳: 0, 500, 1000, 1500
         int freqLastSecond = 2;
         int accountCountOnIp = 2;
         List<Long> timestamps = List.of(0L, 500L, 1000L, 1500L);

         // ── 期望计算 ──
         // 高频分 = min(2×10, 100) = 20 → 加权 20×0.4 = 8
         // 多账号分 = min((2-1)/9×100, 100) = 11.1 → 加权 11.1×0.3 ≈ 3.3
         // 规律分: 间隔 [500,500,500], std=0 → 规律分=80 → 加权 80×0.3 = 24
         // 总分 = 8 + 3.3 + 24 = 35.3 →  30~60 → REVIEW
         double expectedTotal = 35.3;

         CheatingReport report = engine.evaluate(300L, "172.16.0.1",
                 freqLastSecond, accountCountOnIp, timestamps);

         assertEquals(expectedTotal, report.getRiskScore(), 5.0,
                 "边界 case 总分应在 ~35，实际=" + report.getRiskScore());
         assertFalse(report.isHighRisk(), "边界 case 不应标记为高风险");
         assertTrue(report.isReview(), "边界 case 应标记待审核");
         assertEquals("REVIEW", report.getAction());
     }
 }
