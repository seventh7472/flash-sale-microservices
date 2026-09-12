 package com.flashsale.ai.config;

 import com.alibaba.csp.sentinel.slots.block.RuleConstant;
 import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
 import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
 import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
 import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.context.annotation.Configuration;

 import javax.annotation.PostConstruct;
 import java.util.ArrayList;
 import java.util.List;

 @Slf4j
 @Configuration
 public class SentinelRuleConfig {

     @PostConstruct
     public void init() {
         // AI 服务自身限流保护
         List<FlowRule> flowRules = new ArrayList<>();
         flowRules.add(new FlowRule("ai:detect-cheating")
                 .setCount(200)
                 .setGrade(RuleConstant.FLOW_GRADE_QPS));
         FlowRuleManager.loadRules(flowRules);

         List<DegradeRule> degradeRules = new ArrayList<>();
         degradeRules.add(new DegradeRule("ai:detect-cheating")
                 .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                 .setCount(0.5)
                 .setMinRequestAmount(5)
                 .setStatIntervalMs(10000)
                 .setTimeWindow(10));
         degradeRules.add(new DegradeRule("ai:predict-traffic")
                 .setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO)
                 .setCount(0.5)
                 .setMinRequestAmount(3)
                 .setStatIntervalMs(10000)
                 .setTimeWindow(10));
         DegradeRuleManager.loadRules(degradeRules);

         log.info("AI 服务 Sentinel 规则已加载");
     }
 }
