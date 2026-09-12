 package com.flashsale.order.controller;

import com.flashsale.common.dto.Result;
 import com.alibaba.csp.sentinel.annotation.SentinelResource;
 import com.alibaba.csp.sentinel.slots.block.BlockException;
 import com.flashsale.common.constant.ResultCode;
 import com.flashsale.common.util.UserContext;
 import com.flashsale.order.entity.SeckillOrder;
 import com.flashsale.order.service.OrderService;
 import lombok.RequiredArgsConstructor;
 import org.springframework.web.bind.annotation.*;

 import java.math.BigDecimal;
 import java.util.List;

 @RestController
 @RequestMapping("/api/order")
 @RequiredArgsConstructor
 public class OrderController {
     private final OrderService orderService;

    @GetMapping("/list")
    @SentinelResource(value = "order:list", defaultFallback = "defaultBlock")
     public Result<List<SeckillOrder>> list() {
         return Result.success(orderService.getUserOrders(UserContext.getUserId()));
     }

    @GetMapping("/detail/{orderNo}")
    @SentinelResource(value = "order:detail", defaultFallback = "defaultBlock")
     public Result<SeckillOrder> detail(@PathVariable String orderNo) {
         SeckillOrder o = orderService.getByOrderNo(orderNo, UserContext.getUserId());
         return o == null ? Result.error(ResultCode.NOT_FOUND, "订单不存在") : Result.success(o);
     }

     @PostMapping("/pay/{orderNo}")
     public Result<Void> pay(@PathVariable String orderNo) {
         orderService.payOrder(orderNo, UserContext.getUserId()); return Result.success();
     }

     @PostMapping("/cancel/{orderNo}")
     public Result<Void> cancel(@PathVariable String orderNo) {
         orderService.cancelOrder(orderNo, UserContext.getUserId()); return Result.success();
     }

    @PostMapping("/normal/create")
    public Result<SeckillOrder> normalCreate(@RequestParam Long productId,
                                             @RequestParam(defaultValue = "1") Integer quantity) {
        return Result.success(orderService.createNormalOrder(UserContext.getUserId(), productId, quantity));
    }

     @PostMapping("/flash/create")
     public Result<SeckillOrder> flashCreate(@RequestParam Long userId, @RequestParam Long productId,
                                              @RequestParam Long activityId, @RequestParam BigDecimal flashPrice,
                                              @RequestParam(defaultValue = "1") Integer quantity) {
        return Result.success(orderService.createFlashOrder(userId, productId, activityId, flashPrice, quantity));
    }

    public Result<Void> defaultBlock(Throwable t) {
        return Result.error(ResultCode.RATE_LIMITED, "请求过于频繁");
    }
 }
