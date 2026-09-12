 package com.flashsale.flashsale.config;

 import lombok.Getter;
 import org.springframework.beans.factory.annotation.Value;
 import org.springframework.cloud.context.config.annotation.RefreshScope;
 import org.springframework.stereotype.Component;

 @Getter
 @RefreshScope
 @Component
 public class DynamicConfig {

     @Value("${flash.sale.qps.limit:500}")
     private int qpsLimit;

     @Value("${ai.cheating.threshold.review:30}")
     private int cheatingReviewThreshold;

     @Value("${ai.cheating.threshold.block:60}")
     private int cheatingBlockThreshold;

     @Value("${traffic.prediction.default.qps:1000}")
     private int defaultTrafficQps;
 }
