package com.flashsale.order.feign;

import com.flashsale.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {
    @Override
    public ProductClient create(Throwable cause) {
        log.warn("order→goods 熔断降级: {}", cause.getMessage());
        return id -> Result.failed("商品服务不可用");
    }
}