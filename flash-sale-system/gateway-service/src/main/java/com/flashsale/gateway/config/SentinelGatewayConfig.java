 package com.flashsale.gateway.config;

 import com.alibaba.csp.sentinel.adapter.gateway.common.SentinelGatewayConstants;
 import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiDefinition;
 import com.alibaba.csp.sentinel.adapter.gateway.common.api.ApiPathPredicateItem;
 import com.alibaba.csp.sentinel.adapter.gateway.common.api.GatewayApiDefinitionManager;
 import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
 import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
 import com.alibaba.csp.sentinel.slots.block.RuleConstant;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.context.annotation.Configuration;

 import javax.annotation.PostConstruct;
 import java.util.Collections;
 import java.util.HashSet;
 import java.util.Set;

 @Slf4j
 @Configuration
 public class SentinelGatewayConfig {

     @PostConstruct
     public void init() {
         // 定义 API 分组：秒杀接口
         Set<ApiDefinition> apis = new HashSet<>();
         apis.add(new ApiDefinition("flash_sale_api")
                 .setPredicateItems(Collections.singleton(
                         new ApiPathPredicateItem()
                                 .setPattern("/api/flash-sale/seckill/**")
                                 .setMatchStrategy(SentinelGatewayConstants.URL_MATCH_STRATEGY_PREFIX))));
         GatewayApiDefinitionManager.loadApiDefinitions(apis);

        // 网关级限流规则：秒杀分组 QPS=10，按自定义 API 分组匹配
        Set<GatewayFlowRule> rules = new HashSet<>();
        rules.add(new GatewayFlowRule("flash_sale_api")
                .setResourceMode(SentinelGatewayConstants.RESOURCE_MODE_CUSTOM_API_NAME)
                .setCount(10)
                .setGrade(RuleConstant.FLOW_GRADE_QPS));
         GatewayRuleManager.loadRules(rules);

        log.info("Gateway Sentinel 限流规则已加载: flash_sale_api QPS=10");
     }
 }
