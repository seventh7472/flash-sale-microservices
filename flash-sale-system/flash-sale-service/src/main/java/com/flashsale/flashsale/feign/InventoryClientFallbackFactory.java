package com.flashsale.flashsale.feign;

import com.flashsale.common.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {
    @Override
    public InventoryClient create(Throwable cause) {
        log.warn("stock-service 熔断降级: {}", cause.getMessage());
        return new InventoryClient() {
            public Result<Boolean> decreaseStock(Long activityId, Long productId) { return Result.failed("库存服务不可用"); }
            public Result<Void> releaseStock(Long activityId, Long productId, Integer quantity) { return Result.failed("库存服务不可用"); }
            public Result<Void> warmUpStock(Long a, Integer s) { return Result.failed("库存服务不可用"); }
        };
    }
}
