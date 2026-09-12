 package com.flashsale.common.constant;

 public enum ResultCode {
     SUCCESS(200, "操作成功"),
     FAILED(500, "操作失败"),
     VALIDATE_FAILED(400, "参数校验失败"),
     UNAUTHORIZED(401, "未授权"),
     FORBIDDEN(403, "无权限"),
     NOT_FOUND(404, "资源不存在"),
     BUSINESS_ERROR(1001, "业务异常"),
     RATE_LIMITED(1002, "请求过于频繁"),
     CHEATING_DETECTED(1003, "检测到作弊行为"),
     STOCK_NOT_ENOUGH(2001, "库存不足"),
     FLASH_SALE_NOT_STARTED(2002, "秒杀未开始"),
     FLASH_SALE_ENDED(2003, "秒杀已结束"),
     ORDER_TIMEOUT(3001, "订单超时"),
     DUPLICATE_ORDER(3002, "重复下单");
 
     private final int code;
     private final String message;
 
     ResultCode(int code, String message) {
         this.code = code;
         this.message = message;
     }
 
     public int getCode() { return code; }
     public String getMessage() { return message; }
 }
