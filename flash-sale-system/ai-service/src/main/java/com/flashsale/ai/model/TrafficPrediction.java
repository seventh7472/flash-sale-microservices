 package com.flashsale.ai.model;

 import lombok.Data;

 @Data
 public class TrafficPrediction {
     private Long activityId;
     private int predictedQps;
     private int recommendedInstances;
     private int confidence;
     private String analysis;
     private String aiRawResponse;
 }
