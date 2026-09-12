 package com.flashsale.flashsale.controller;

 import com.alibaba.csp.sentinel.annotation.SentinelResource;
 import com.flashsale.common.dto.Result;
 import com.flashsale.flashsale.ai.CheatingRuleEngine;
import com.flashsale.flashsale.ai.FeatureCollector;
 import com.flashsale.flashsale.ai.TrafficPredictor;
import com.flashsale.flashsale.ai.QpsCollector;
 import com.flashsale.flashsale.model.ChatReply;
 import com.flashsale.flashsale.model.CheatingReport;
 import com.flashsale.flashsale.model.TrafficPrediction;
import com.flashsale.flashsale.service.AiAssistantService;
 import lombok.RequiredArgsConstructor;
 import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

 @Slf4j
 @RestController
 @RequestMapping("/api/ai")
 @RequiredArgsConstructor
 public class AiController {
     private final CheatingRuleEngine ruleEngine;
     private final TrafficPredictor trafficPredictor;
    private final QpsCollector qpsCollector;
    private final FeatureCollector featureCollector;
    private final AiAssistantService aiAssistantService;

     @PostMapping("/detect-cheating")
     public Result<CheatingReport> detectCheating(@RequestParam Long userId, @RequestParam Long activityId,
                                                    @RequestParam(defaultValue = "unknown") String clientIp) {
         // 使用真实采集特征，使 REVIEW/BLOCK 判定可达；
        // DeepSeek 仅在 REVIEW 时做二次分析并自动降级（未配置/失败则只返回规则结果）
        int freq = featureCollector.getFreq(userId);
        int accounts = featureCollector.getAccountCount(clientIp);
        List<Long> timeline = featureCollector.getTimeline(userId);
        int concurrent = featureCollector.getConcurrent(activityId);
        CheatingReport r = ruleEngine.evaluateWithAi(userId, clientIp, freq, accounts, timeline, concurrent);
         return Result.success(r);
     }

    @PostMapping("/predict-traffic")
    @SentinelResource(value = "ai:predict-traffic", fallback = "trafficFallback")
    public Result<TrafficPrediction> predictTraffic(@RequestBody(required = false) Map<String, Object> body) {
        Long aid = null;
        if (body != null && body.get("activityId") != null) {
            aid = Long.valueOf(String.valueOf(body.get("activityId")));
        }
        if (aid == null) {
            return Result.failed("activityId 不能为空");
        }
        TrafficPrediction tp = qpsCollector.predictForActivity(aid);
        return Result.success(tp);
    }

    public Result<TrafficPrediction> trafficFallback(Map<String, Object> body, Throwable t) {
        log.warn("AI预测降级: body={}", body);
        return Result.success(new TrafficPrediction());
    }

    /**
     * AI 风控助手对话：服务端拼装实时上下文（当前可疑用户前 5 条 + 当前系统 QPS）后交给 DeepSeek 生成回答。
     *
     * <p>只读接口，不改动任何业务数据；DeepSeek 不可用时返回降级摘要，接口始终保持 200。
     */
    @PostMapping("/chat")
    public Result<ChatReply> chat(@RequestBody(required = false) Map<String, Object> body) {
        Object raw = body == null ? null : body.get("question");
        String question = raw == null ? null : String.valueOf(raw);
        if (question == null || question.isBlank()) {
            return Result.failed("question 不能为空");
        }
        return Result.success(aiAssistantService.ask(question));
    }
}
