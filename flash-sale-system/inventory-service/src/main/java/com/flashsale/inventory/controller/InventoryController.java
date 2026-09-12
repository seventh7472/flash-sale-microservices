package com.flashsale.inventory.controller;

import com.flashsale.common.dto.Result;
import com.flashsale.inventory.entity.Stock;
import com.flashsale.inventory.mapper.StockMapper;
import com.flashsale.inventory.service.InventoryService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.flashsale.common.constant.ResultCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {
    private final InventoryService inventoryService;
    private final StockMapper stockMapper;

    @GetMapping("/{productId}")
    public Result<Stock> get(@PathVariable Long productId) {
        Stock s = stockMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Stock>()
                        .eq(Stock::getProductId, productId));
        return Result.success(s);
    }

   @PostMapping("/warm-up")
   @SentinelResource(value = "stock:warmup", defaultFallback = "defaultBlock")
    public Result<Void> warmUp(@RequestParam(required = false) Long activityId,
                               @RequestParam(required = false) Integer stock) {
        if (activityId == null || stock == null || stock <= 0) {
            return Result.error(ResultCode.VALIDATE_FAILED, "活动ID与库存必须为正数");
        }
        inventoryService.warmUpStock(activityId, stock);
        return Result.success();
    }

    @PostMapping("/decrease/{activityId}")
    public Result<Boolean> decrease(@PathVariable Long activityId, @RequestParam Long productId) {
        return Result.success(inventoryService.decreaseStock(activityId, productId));
    }

    @PostMapping("/release/{activityId}")
    public Result<Void> release(@PathVariable Long activityId, @RequestParam Long productId, @RequestParam Integer quantity) {
        inventoryService.releaseStock(activityId, productId, quantity);
        return Result.success();
    }

    @GetMapping("/stock/{activityId}")
    public Result<Integer> getStock(@PathVariable Long activityId) {
       return Result.success(inventoryService.getRedisStock(activityId));
   }

   public Result<Void> defaultBlock(Throwable t) {
       return Result.error(ResultCode.RATE_LIMITED, "请求过于频繁");
   }
}
