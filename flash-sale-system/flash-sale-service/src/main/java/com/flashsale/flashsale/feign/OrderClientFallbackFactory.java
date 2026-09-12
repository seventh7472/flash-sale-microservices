 package com.flashsale.flashsale.feign;

 import com.flashsale.common.dto.Result;
 import com.flashsale.flashsale.dto.OrderDTO;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.cloud.openfeign.FallbackFactory;
 import org.springframework.stereotype.Component;

 import java.math.BigDecimal;

 @Slf4j
 @Component
 public class OrderClientFallbackFactory implements FallbackFactory<OrderClient> {
     @Override
     public OrderClient create(Throwable cause) {
         log.warn("order-service 熔断降级: {}", cause.getMessage());
         return (u, p, a, f, q) -> { log.warn("createFlashOrder 降级"); return Result.failed("订单服务不可用"); };
     }
 }
