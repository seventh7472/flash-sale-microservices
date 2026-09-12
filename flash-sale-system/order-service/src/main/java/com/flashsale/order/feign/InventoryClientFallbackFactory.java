package com.flashsale.order.feign;

import com.flashsale.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {
    @Override
    public InventoryClient create(Throwable cause) {
        log.warn("order→stock 熔断降级: {}", cause.getMessage());
        return (a, p, q) -> { log.warn("releaseStock 降级"); return Result.failed("库存服务不可用"); };
    }
}
