package com.flashsale.flashsale.feign;

import com.flashsale.common.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "stock-service", path = "/api/inventory", fallbackFactory = InventoryClientFallbackFactory.class)
public interface InventoryClient {

    @PostMapping("/decrease/{activityId}")
    Result<Boolean> decreaseStock(@PathVariable("activityId") Long activityId,
                                  @RequestParam("productId") Long productId);

    @PostMapping("/release/{activityId}")
    Result<Void> releaseStock(@PathVariable("activityId") Long activityId,
                              @RequestParam("productId") Long productId,
                              @RequestParam("quantity") Integer quantity);
   @PostMapping("/warm-up")
   Result<Void> warmUpStock(@RequestParam("activityId") Long activityId, @RequestParam("stock") Integer stock);
}
