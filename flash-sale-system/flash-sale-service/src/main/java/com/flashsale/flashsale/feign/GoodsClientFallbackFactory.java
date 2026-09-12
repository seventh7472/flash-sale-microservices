 package com.flashsale.flashsale.feign;

 import com.flashsale.common.dto.Result;
 import lombok.extern.slf4j.Slf4j;
 import org.springframework.cloud.openfeign.FallbackFactory;
 import org.springframework.stereotype.Component;

 @Slf4j
 @Component
 public class GoodsClientFallbackFactory implements FallbackFactory<GoodsClient> {
     @Override
     public GoodsClient create(Throwable cause) {
         log.warn("goods-service 熔断降级: {}", cause.getMessage());
         return id -> { log.warn("getProduct 降级: id={}", id); return Result.failed("商品服务不可用"); };
     }
 }
