 package com.flashsale.ai.config;

 import lombok.Data;
 import org.springframework.boot.context.properties.ConfigurationProperties;
 import org.springframework.context.annotation.Configuration;

 @Data
 @Configuration
 @ConfigurationProperties(prefix = "ai.api")
 public class AiApiConfig {
     private String baseUrl = "https://api.deepseek.com";
     private String apiKey = "sk-your-key-here";
     private String model = "deepseek-chat";
     private int timeout = 5000;
 }
