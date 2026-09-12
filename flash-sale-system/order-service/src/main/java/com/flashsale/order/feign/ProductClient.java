package com.flashsale.order.feign;

import com.flashsale.common.dto.Result;
import com.flashsale.order.dto.ProductDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "goods-service", path = "/api/product", fallbackFactory = ProductClientFallbackFactory.class)
public interface ProductClient {

    @GetMapping("/detail/{id}")
    Result<ProductDTO> getById(@PathVariable("id") Long id);
}