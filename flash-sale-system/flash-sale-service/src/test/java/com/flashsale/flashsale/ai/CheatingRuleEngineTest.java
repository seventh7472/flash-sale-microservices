package com.flashsale.flashsale.ai;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import com.flashsale.flashsale.model.CheatingReport;
import static org.junit.jupiter.api.Assertions.*;

/**
 * AI反作弊规则引擎单元测试
 * 测试四维度打分：高频、多账号、规律、集中
 */
class CheatingRuleEngineTest {

    private final CheatingRuleEngine engine = new CheatingRuleEngine();

    // ========== 场景1：正常用户行为 ==========
    @Test
    void testNormalUser_shouldPass() {
        // 正常用户：1秒内1次请求、单一IP、间隔不规律、低并发
        List<Long> tl = Arrays.asList(100L, 2300L, 4100L, 6200L, 8500L);
        CheatingReport r = engine.evaluate(1L, "192.168.1.1", 1, 1, tl, 5);
        assertFalse(r.isHighRisk(), "正常用户不应被判为高风险");
        assertFalse(r.isReview(), "正常用户不应被标记为可疑");
        assertEquals("PASS", r.getAction());
    }

    // ========== 场景2：高频异常 ==========
    @Test
    void testHighFreq_shouldBlock() {
        // 1秒内同一用户请求8次->高频得分大庅随8=8*10*0.4=32
        List<Long> tl = Arrays.asList(100L, 250L, 400L, 550L, 700L, 850L, 1000L, 1150L);
        CheatingReport r = engine.evaluate(1L, "192.168.1.1", 8, 1, tl, 5);
        assertTrue(r.isHighRisk() || r.isReview(), "高频请求应被拦截或标记");
        assertTrue(r.getRiskScore() >= 30, "高频请求总分应≥30");
    }

    // ========== 场景3：多账号同IP ==========
    @Test
    void testMultiAccount_shouldBlock() {
        // 同一IP关联5个不同账号
        List<Long> tl = Arrays.asList(100L, 500L);
        CheatingReport r = engine.evaluate(1L, "10.0.0.1", 2, 5, tl, 10);
        assertTrue(r.getScoreMultiAccount() > 0, "多账号维度得分应大于0");
    }

    // ========== 场景4：规律间隔（脚本化） ==========
    @Test
    void testRegularInterval_shouldDetect() {
        // 间隔非常规律，每次间隔500ms左右
        List<Long> tl = Arrays.asList(1000L, 1500L, 2000L, 2500L, 3000L);
        CheatingReport r = engine.evaluate(1L, "192.168.1.1", 1, 1, tl, 5);
        assertTrue(r.isHighRisk() || r.isReview(), "规律间隔应被识别");
        assertTrue(r.getScoreRegular() > 0, "规律维度得分应大于0");
    }

    // ========== 场景5：集中抢购 ==========
    @Test
    void testConcentrated_shouldBlock() {
        // 同一秒并发60人抢购
        List<Long> tl = Arrays.asList(100L, 200L, 300L);
        CheatingReport r = engine.evaluate(1L, "192.168.1.1", 3, 1, tl, 60);
        assertTrue(r.isHighRisk(), "大规模集中抢购应被拦截");
    }

    // ========== 场景6：边界情况—接近阈值但未超 ==========
    @Test
    void testBoundary_justBelowThreshold() {
        // 每秒2次请求，间隔略微有规律但不完全一致
        List<Long> tl = Arrays.asList(100L, 600L, 1100L, 1700L, 2300L);
        CheatingReport r = engine.evaluate(1L, "10.0.0.1", 2, 1, tl, 8);
        assertFalse(r.isHighRisk(), "边界情况不应被判为高风险");
        // 可能被标记为可疑或通过
        assertTrue(r.isReview() || "PASS".equals(r.getAction()));
    }

    // ========== 场景7：大庅随高频+多账号+规律 ==========
    @Test
    void testMultiDimension_shouldFirmlyBlock() {
        // 同IP关联4账号，每个账号每秒3次，间隔规律，并发30
        List<Long> tl = Arrays.asList(100L, 500L, 900L, 1300L, 1700L, 2100L, 2500L, 2900L);
        CheatingReport r = engine.evaluate(1L, "10.0.0.1", 3, 4, tl, 30);
        assertTrue(r.isHighRisk(), "多维度异常应被高风险拦截");
        assertTrue(r.getRiskScore() >= 60, "多维度异常总分应≥60");
    }

}