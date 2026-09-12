 package com.flashsale.flashsale.service;

 import com.alibaba.csp.sentinel.Entry;
 import com.alibaba.csp.sentinel.SphU;
 import com.alibaba.csp.sentinel.Tracer;
 import com.alibaba.csp.sentinel.slots.block.BlockException;
 import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
 import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
 import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
 import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
 import org.junit.jupiter.api.*;

 import java.util.ArrayList;
 import java.util.Collections;
 import java.util.concurrent.*;
 import java.util.concurrent.atomic.AtomicInteger;

 import static org.junit.jupiter.api.Assertions.*;

 /**
  * Sentinel 限流熔断测试（直接使用 Java API，不依赖 Spring 上下文）
  *
  * 场景1：超限流阈值 → 被 BlockException 拒绝（非 500）
  * 场景2：下游超时/宕机 → 错误率超阈值 → 断路器打开 → 新请求快速降级
  */
 class SentinelFlowDegradeTest {

     private static final String R_FLOW = "flash:seckill";
     private static final String R_DEGRADE = "ai:detect-cheating";

     @AfterEach
     void cleanup() {
         FlowRuleManager.loadRules(new ArrayList<>());
         DegradeRuleManager.loadRules(new ArrayList<>());
     }

     // =====================================================================
     // 场景1：限流 — 20并发，QPS=5 → ^5通过，^15被阻断
     // =====================================================================
     @Test
     void testRateLimit() throws Exception {
         FlowRuleManager.loadRules(Collections.singletonList(
                 new FlowRule(R_FLOW).setCount(5)
                         .setGrade(com.alibaba.csp.sentinel.slots.block.RuleConstant.FLOW_GRADE_QPS)));

         int N = 20;
         var p = Executors.newFixedThreadPool(N);
         var b = new CyclicBarrier(N);
         var l = new CountDownLatch(N);
         var ok = new AtomicInteger();
         var blocked = new AtomicInteger();
         var err = new AtomicInteger();

         for (int i = 0; i < N; i++) {
             p.submit(() -> {
                 try { b.await(5, TimeUnit.SECONDS); } catch (Exception e) { err.incrementAndGet(); l.countDown(); return; }
                 try (Entry e2 = SphU.entry(R_FLOW)) { ok.incrementAndGet();
                 } catch (BlockException e2) { blocked.incrementAndGet();
                 } catch (Exception e2) { err.incrementAndGet();
                 } finally { l.countDown(); }
             });
         }
         l.await(30, TimeUnit.SECONDS);
         p.shutdown();

         assertTrue(ok.get() <= 15, "QPS=5 突发20并发，通过数≤15，实际:" + ok.get());
         assertTrue(blocked.get() >= 5, "被限流≥5，实际:" + blocked.get());
         assertEquals(0, err.get(), "不应有系统错误（非 BlockException）");
         assertEquals(N, ok.get() + blocked.get() + err.get());
     }

     // =====================================================================
     // 场景2：熔断 — 6次中有4次超时（67%错误率 > 50%阈值）→ 熔断打开 → 后续6次降级
     // 注意：try-with-resources 时 Sentinel 不会自动追踪异常，
     //       必须手动调用 Tracer.traceEntry(t, entry) 才能在 exit() 前记录错误
     // =====================================================================
     @Test
     void testCircuitBreaker() throws Exception {
         DegradeRuleManager.loadRules(Collections.singletonList(
                 new DegradeRule(R_DEGRADE)
                         .setGrade(com.alibaba.csp.sentinel.slots.block.RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                         .setCount(0.5).setMinRequestAmount(5)
                         .setStatIntervalMs(10000).setTimeWindow(5)));

         // 阶段1：6次请求，其中4次失败 → 错误率 67% > 50%，触发熔断
         int phase1Fail = 0;
         for (int i = 0; i < 6; i++) {
             boolean failed = doCall(R_DEGRADE, i < 4, 50);
             if (failed) phase1Fail++;
         }

         // 阶段2：6次请求，熔断应已打开，会快速降级（不等待超时）
         int phase2Degrade = 0;
         for (int i = 0; i < 6; i++) {
             long start = System.nanoTime();
             boolean degraded = doCall(R_DEGRADE, false, 2000); // ⚠ 模拟超时2s，但熔断应快速返回
             long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
             if (degraded) phase2Degrade++;
             if (degraded) {
                 assertTrue(elapsedMs < 500,
                         "熔断降级应在 " + elapsedMs + "ms 内返回，不应等待2s超时");
             }
         }

         assertTrue(phase1Fail >= 4,
                 "阶段1至少4次应失败（含显式失败+可能触发熔断），实际:" + phase1Fail);
         assertTrue(phase2Degrade >= 3,
                 "阶段2至少3次应被熔断降级，实际:" + phase2Degrade);
     }

     // ========== 辅助 ==========

     /** 手动管理 Entry，确保 Tracer.traceEntry 在 exit() 前执行 */
     private boolean doCall(String resource, boolean fail, long sleepMs) {
         Entry entry = null;
         try {
             entry = SphU.entry(resource);
             if (sleepMs > 0) Thread.sleep(sleepMs);
             if (fail) throw new RuntimeException("模拟超时");
             return false; // 成功
         } catch (BlockException e) {
             return true; // 被熔断降级
         } catch (Throwable t) {
             // ★ 关键：必须在 entry.exit() 前追踪异常，否则 Sentinel 不会统计
             if (entry != null) Tracer.traceEntry(t, entry);
             return true; // 失败
         } finally {
             if (entry != null) entry.exit();
         }
     }
 }
