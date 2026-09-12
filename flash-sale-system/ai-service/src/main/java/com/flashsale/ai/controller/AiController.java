 package com.flashsale.ai.controller;

 import com.alibaba.csp.sentinel.annotation.SentinelResource;
 import com.flashsale.ai.model.CheatingReport;
 import com.flashsale.ai.model.TrafficPrediction;
 import com.flashsale.ai.service.AiService;
 import com.flashsale.common.dto.Result;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.web.bind.annotation.*;

 @Slf4j
 @RestController
 @RequestMapping("/api/ai")
 @RequiredArgsConstructor
 public class AiController {

     private final AiService aiService;

     @PostMapping("/detect-cheating")
     @SentinelResource(value = "ai:detect-cheating", fallback = "cheatingFallback")
     public Result<CheatingReport> detectCheating(@RequestParam Long userId,
                                                   @RequestParam Long activityId,
                                                   @RequestParam(defaultValue = "unknown") String clientIp) {
         CheatingReport report = aiService.detectCheating(userId, clientIp, activityId);
         log.debug("AI防作弊: userId={}, activityId={}, score={}, action={}",
                 userId, activityId, report.getRiskScore(), report.getAction());
         return Result.success(report);
     }

     public Result<CheatingReport> cheatingFallback(Long userId, Long activityId, String clientIp, Throwable t) {
         log.warn("AI防作弊Sentinel降级: userId={}", userId);
         return Result.success(CheatingReport.pass());
     }

     @PostMapping("/predict-traffic")
     @SentinelResource(value = "ai:predict-traffic", fallback = "trafficFallback")
     public Result<TrafficPrediction> predictTraffic(@RequestParam Long activityId) {
         return Result.success(aiService.predictTraffic(activityId));
     }

     public Result<TrafficPrediction> trafficFallback(Long activityId, Throwable t) {
         log.warn("AI预测Sentinel降级: activityId={}", activityId);
         return Result.success(new TrafficPrediction());
     }
 }
