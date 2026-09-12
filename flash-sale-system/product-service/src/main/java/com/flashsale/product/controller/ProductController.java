 package com.flashsale.product.controller;

 import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
 import com.flashsale.common.dto.Result;
 import com.flashsale.product.entity.Product;
 import com.flashsale.product.service.ProductService;
 import lombok.RequiredArgsConstructor;
 import org.springframework.web.bind.annotation.*;

 @RestController
 @RequestMapping("/api/product")
 @RequiredArgsConstructor
 public class ProductController {
     private final ProductService productService;

     @GetMapping("/list")
     public Result<Page<Product>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size) {
         return Result.success(productService.page(page, size));
     }

     @GetMapping("/detail/{id}")
     public Result<Product> detail(@PathVariable Long id) {
         return Result.success(productService.getById(id));
     }

     @GetMapping("/{id}")
     public Result<Product> getById(@PathVariable Long id) {
         return Result.success(productService.getById(id));
     }

     @PostMapping("/create")
     public Result<Void> create(@RequestBody Product product) {
         productService.create(product);
         return Result.success();
     }

     @PutMapping("/update")
     public Result<Void> update(@RequestBody Product product) {
         productService.update(product);
         return Result.success();
     }

     @DeleteMapping("/delete/{id}")
     public Result<Void> delete(@PathVariable Long id) {
         productService.delete(id);
         return Result.success();
     }
 }
