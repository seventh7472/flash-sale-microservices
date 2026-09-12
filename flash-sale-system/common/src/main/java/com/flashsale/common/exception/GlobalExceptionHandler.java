 package com.flashsale.common.exception;

 import com.alibaba.csp.sentinel.slots.block.BlockException;
 import com.flashsale.common.constant.ResultCode;
 import com.flashsale.common.dto.Result;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.validation.BindException;
 import org.springframework.web.bind.MethodArgumentNotValidException;
 import org.springframework.web.bind.annotation.ExceptionHandler;
 import org.springframework.web.bind.annotation.RestControllerAdvice;

 import javax.validation.ConstraintViolationException;

 @Slf4j
 @RestControllerAdvice
 public class GlobalExceptionHandler {

     @ExceptionHandler(BusinessException.class)
     public Result<Void> handleBusiness(BusinessException e) {
         log.warn("业务异常: code={}, msg={}", e.getResultCode().getCode(), e.getMessage());
         return Result.error(e.getResultCode(), e.getMessage());
     }

     /** Sentinel 限流/熔断异常 */
     @ExceptionHandler(BlockException.class)
     public Result<Void> handleBlock(BlockException e) {
         log.warn("Sentinel 限流/熔断: rule={}, resource={}", e.getRule(), e.getRuleLimitApp());
         return Result.error(ResultCode.RATE_LIMITED, "请求过于频繁，请稍后重试");
     }

     @ExceptionHandler(MethodArgumentNotValidException.class)
     public Result<Void> handleValidation(MethodArgumentNotValidException e) {
         String msg = e.getBindingResult().getFieldError() != null
                 ? e.getBindingResult().getFieldError().getDefaultMessage() : "参数校验失败";
         return Result.failed(msg);
     }

     @ExceptionHandler(BindException.class)
     public Result<Void> handleBind(BindException e) {
         String msg = e.getFieldError() != null ? e.getFieldError().getDefaultMessage() : "参数绑定失败";
         return Result.failed(msg);
     }

     @ExceptionHandler(ConstraintViolationException.class)
     public Result<Void> handleConstraint(ConstraintViolationException e) {
         return Result.failed(e.getMessage());
     }

     @ExceptionHandler(Exception.class)
     public Result<Void> handleException(Exception e) {
         log.error("系统异常", e);
         return Result.failed("系统繁忙，请稍后重试");
     }
 }
