 package com.flashsale.common.exception;

 import com.flashsale.common.constant.ResultCode;

 public class BusinessException extends RuntimeException {
     private final ResultCode resultCode;
 
     public BusinessException(ResultCode rc) {
         super(rc.getMessage());
         this.resultCode = rc;
     }
     public BusinessException(ResultCode rc, String message) {
         super(message);
         this.resultCode = rc;
     }
     public ResultCode getResultCode() { return resultCode; }
 }
