 package com.flashsale.ai.model;

 import lombok.Data;

 @Data
 public class CheatingReport {
     private boolean highRisk;
     private boolean review;
     private double riskScore;
     private double scoreFreq;
     private double scoreMultiAccount;
     private double scoreRegular;
     private String action;
     private String reason;

     public static CheatingReport pass() {
         CheatingReport r = new CheatingReport();
         r.riskScore = 0;
         r.action = "PASS";
         r.reason = "正常";
         return r;
     }

     public static CheatingReport pass(String reason) {
         CheatingReport r = pass();
         r.reason = reason;
         return r;
     }

     public static CheatingReport review(double score, String reason) {
         CheatingReport r = new CheatingReport();
         r.riskScore = score;
         r.review = true;
         r.action = "REVIEW";
         r.reason = reason;
         return r;
     }

     public static CheatingReport block(double score, String reason) {
         CheatingReport r = new CheatingReport();
         r.riskScore = score;
         r.highRisk = true;
         r.action = "BLOCK";
         r.reason = reason;
         return r;
     }
 }
