 package com.flashsale.ai.service;

 import com.flashsale.ai.model.CheatingReport;
 import com.flashsale.ai.model.TrafficPrediction;

 public interface AiService {
     CheatingReport detectCheating(Long userId, String clientIp, Long activityId);
     TrafficPrediction predictTraffic(Long activityId);
 }
