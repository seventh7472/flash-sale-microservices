 package com.flashsale.flashsale.config;

 import com.alibaba.csp.sentinel.slots.block.RuleConstant;
 import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
 import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
 import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
 import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
 import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
 import org.springframework.context.annotation.Configuration;

 import javax.annotation.PostConstruct;
 import java.util.ArrayList;
 import java.util.List;

 @Slf4j
 @Configuration
 @RequiredArgsConstructor
 public class SentinelRuleConfig {

    private final DynamicConfig dynamicConfig;

     @PostConstruct
     public void init() {
        // ── 限流规则：秒杀接口 QPS 取配置 flash.sale.qps.limit，默认快速失败 ──
         // ⚠ setLimitApp 返回 AbstractRule，不参与链式调用
         List<FlowRule> flowRules = new ArrayList<>();
         FlowRule flow = new FlowRule("flash:seckill");
        flow.setCount(dynamicConfig.getQpsLimit());
         flow.setGrade(RuleConstant.FLOW_GRADE_QPS);
         flowRules.add(flow);
         FlowRuleManager.loadRules(flowRules);
        log.info("Sentinel 限流已加载: flash:seckill QPS={}", dynamicConfig.getQpsLimit());

         // ── 熔断规则：AI 流量预测接口异常比 ≥ 50% 熔断 10s ──
         List<DegradeRule> degradeRules = new ArrayList<>();
         DegradeRule d = new DegradeRule("ai:predict-traffic");
         d.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
         d.setCount(0.5);
         d.setMinRequestAmount(5);
         d.setStatIntervalMs(10000);
         d.setTimeWindow(10);
         degradeRules.add(d);
         DegradeRuleManager.loadRules(degradeRules);
         log.info("Sentinel 熔断已加载: ai:predict-traffic 异常比≥50% 熔断10s");
     }
 }
