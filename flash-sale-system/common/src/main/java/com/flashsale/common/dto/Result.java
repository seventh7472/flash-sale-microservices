 package com.flashsale.common.dto;

 import com.flashsale.common.constant.ResultCode;

 public class Result<T> {
     private int code;
     private String message;
     private T data;
 
     public Result() {}
 
     public static <T> Result<T> success() {
         return result(ResultCode.SUCCESS, null);
     }
     public static <T> Result<T> success(T data) {
         return result(ResultCode.SUCCESS, data);
     }
     public static <T> Result<T> failed(String message) {
         Result<T> r = new Result<>();
         r.code = ResultCode.FAILED.getCode();
         r.message = message;
         return r;
     }
     public static <T> Result<T> error(ResultCode rc) {
         return result(rc, null);
     }
     public static <T> Result<T> error(ResultCode rc, String msg) {
         Result<T> r = new Result<>();
         r.code = rc.getCode();
         r.message = msg;
         return r;
     }
     private static <T> Result<T> result(ResultCode rc, T data) {
         Result<T> r = new Result<>();
         r.code = rc.getCode();
         r.message = rc.getMessage();
         r.data = data;
         return r;
     }
 
     public int getCode() { return code; }
     public void setCode(int code) { this.code = code; }
     public String getMessage() { return message; }
     public void setMessage(String message) { this.message = message; }
     public T getData() { return data; }
     public void setData(T data) { this.data = data; }
 }
