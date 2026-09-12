 package com.flashsale.flashsale.feign;

 import com.flashsale.common.dto.Result;
 import org.springframework.cloud.openfeign.FeignClient;
 import org.springframework.web.bind.annotation.GetMapping;
 import org.springframework.web.bind.annotation.PathVariable;

 @FeignClient(name = "goods-service", path = "/api/product", fallbackFactory = GoodsClientFallbackFactory.class)
 public interface GoodsClient {
     @GetMapping("/detail/{id}")
     Result<ProductDTO> getProduct(@PathVariable("id") Long id);
 }
