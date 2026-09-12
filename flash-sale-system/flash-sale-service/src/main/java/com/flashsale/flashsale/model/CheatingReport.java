 package com.flashsale.flashsale.model;

 import lombok.Data;

 @Data
 public class CheatingReport {
     private boolean highRisk, review;
     private double riskScore, scoreFreq, scoreMultiAccount, scoreRegular, scoreConcentrated;
     private String action, reason;
    /** DeepSeek 二次分析的自然语言风险说明；未调用或调用失败时为 null（此时仅保留规则引擎结果） */
    private String aiAnalysis;

     public static CheatingReport pass() {
         CheatingReport r = new CheatingReport();
         r.action = "PASS"; r.reason = "正常";
         return r;
     }
 }
