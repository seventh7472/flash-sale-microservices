 package com.flashsale.ai.service;

 import com.flashsale.ai.config.AiApiConfig;
 import com.flashsale.ai.model.CheatingReport;
 import com.flashsale.ai.model.TrafficPrediction;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.http.*;
 import org.springframework.stereotype.Service;
 import org.springframework.web.client.RestTemplate;

 import java.util.Collections;
 import java.util.List;
 import java.util.Map;

 @Slf4j
 @Service
 @RequiredArgsConstructor
 public class AiServiceImpl implements AiService {

     private final AiApiConfig aiApiConfig;
     private final RestTemplate restTemplate;
     private final CheatingRuleEngine ruleEngine;

     @Override
     public CheatingReport detectCheating(Long userId, String clientIp, Long activityId) {
         // 1. 规则引擎打分（不论 API Key 是否配置都执行）
         int freqLastSecond = 1;       // TODO: 从 Redis 读取实际频率
         int accountCountOnIp = 1;     // TODO: 从 Redis 读取同 IP 账号数
         List<Long> timestamps = Collections.singletonList(System.currentTimeMillis()); // TODO: 从 Redis 读取历史时间戳

         CheatingReport report = ruleEngine.evaluate(userId, clientIp,
                 freqLastSecond, accountCountOnIp, timestamps);

         // 2. DeepSeek API 辅助分析（仅在 API Key 已配置时）
         if (!"sk-your-key-here".equals(aiApiConfig.getApiKey())) {
             try {
                 enhanceWithDeepSeek(userId, activityId, report);
             } catch (Exception e) {
                 log.warn("DeepSeek 辅助分析失败，使用规则引擎结果: userId={}", userId, e);
             }
         }

         return report;
     }

     private void enhanceWithDeepSeek(Long userId, Long activityId, CheatingReport base) {
         String prompt = "你是一个电商秒杀系统防作弊检测专家。用户ID:" + userId + " 活动ID:" + activityId
                 + "。规则引擎评分:" + base.getRiskScore()
                 + "。请提供补充分析，JSON格式: {riskScore,int,reason,string,isHighRisk,bool}";

         Map<String, Object> body = Map.of(
                 "model", aiApiConfig.getModel(),
                 "messages", new Object[]{Map.of("role", "user", "content", prompt)},
                 "temperature", 0.1);
         HttpHeaders h = new HttpHeaders();
         h.setContentType(MediaType.APPLICATION_JSON);
         h.setBearerAuth(aiApiConfig.getApiKey());

         ResponseEntity<Map> resp = restTemplate.exchange(
                 aiApiConfig.getBaseUrl() + "/v1/chat/completions",
                 HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
         log.info("DeepSeek 防作弊补充分析: userId={}, status={}", userId, resp.getStatusCode());
     }

     @Override
     public TrafficPrediction predictTraffic(Long activityId) {
         if ("sk-your-key-here".equals(aiApiConfig.getApiKey())) {
             return defaultPrediction(activityId);
         }
         try {
             Map<String, Object> body = Map.of(
                     "model", aiApiConfig.getModel(),
                     "messages", new Object[]{Map.of("role", "user",
                             "content", "预测该秒杀活动的并发QPS和推荐部署实例数。")},
                     "temperature", 0.1);
             HttpHeaders h = new HttpHeaders();
             h.setContentType(MediaType.APPLICATION_JSON);
             h.setBearerAuth(aiApiConfig.getApiKey());
             restTemplate.exchange(aiApiConfig.getBaseUrl() + "/v1/chat/completions",
                     HttpMethod.POST, new HttpEntity<>(body, h), Map.class);
         } catch (Exception e) {
             log.warn("AI预测失败，使用默认值: activityId={}", activityId, e);
         }
         return defaultPrediction(activityId);
     }

     private TrafficPrediction defaultPrediction(Long activityId) {
         TrafficPrediction p = new TrafficPrediction();
         p.setActivityId(activityId);
         p.setPredictedQps(1000);
         p.setRecommendedInstances(2);
         p.setConfidence(70);
         p.setAnalysis("基于默认配置的保守预测，接入AI后可获得更精准结果");
         return p;
     }
 }
