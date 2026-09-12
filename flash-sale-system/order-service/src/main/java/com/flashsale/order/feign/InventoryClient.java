package com.flashsale.order.feign;

import com.flashsale.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "stock-service", path = "/api/inventory", fallbackFactory = InventoryClientFallbackFactory.class)
public interface InventoryClient {

    @PostMapping("/release/{activityId}")
    Result<Void> releaseStock(@PathVariable("activityId") Long activityId,
                              @RequestParam("productId") Long productId,
                              @RequestParam("quantity") Integer quantity);
}
