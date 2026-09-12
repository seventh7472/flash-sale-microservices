 package com.flashsale.flashsale.feign;

 import com.flashsale.common.dto.Result;
 import com.flashsale.flashsale.dto.OrderDTO;
 import org.springframework.cloud.openfeign.FeignClient;
 import org.springframework.web.bind.annotation.PostMapping;
 import org.springframework.web.bind.annotation.RequestParam;

 import java.math.BigDecimal;

 @FeignClient(name = "order-service", path = "/api/order", fallbackFactory = OrderClientFallbackFactory.class)
 public interface OrderClient {

     @PostMapping("/flash/create")
     Result<OrderDTO> createFlashOrder(@RequestParam("userId") Long userId,
                                       @RequestParam("productId") Long productId,
                                       @RequestParam("activityId") Long activityId,
                                       @RequestParam("flashPrice") BigDecimal flashPrice,
                                       @RequestParam("quantity") Integer quantity);
 }
