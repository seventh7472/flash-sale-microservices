package com.flashsale.flashsale.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.flashsale.common.constant.ResultCode;
import com.flashsale.common.dto.Result;
import com.flashsale.flashsale.dto.OrderDTO;
import com.flashsale.flashsale.entity.SeckillActivity;
import com.flashsale.flashsale.service.FlashSaleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/flash-sale")
@RequiredArgsConstructor
public class FlashSaleController {

    private final FlashSaleService flashSaleService;

    @PostMapping("/seckill/{activityId}")
    @SentinelResource(value = "flash:seckill", blockHandler = "seckillBlock")
    public Result<OrderDTO> seckill(@PathVariable Long activityId,
                                     @RequestHeader("X-User-Id") Long userId,
                                     @RequestHeader(value = "X-Forwarded-For", defaultValue = "unknown") String clientIp,
                                     @RequestParam(value = "idempotencyToken", required = false) String idempotencyToken) {
        return flashSaleService.seckill(userId, extractClientIp(clientIp), activityId, idempotencyToken);
    }

    /**
     * 取 X-Forwarded-For 中的真实客户端 IP。
     * 网关 ClientIpGlobalFilter 会把直连对端地址追加到末尾，
     * 因此最后一个非空片段才是可信真实 IP（前面的可能被客户端伪造）。
     */
    private static String extractClientIp(String forwardedFor) {
        if (forwardedFor == null || forwardedFor.isBlank()) {
            return "unknown";
        }
        String[] parts = forwardedFor.split(",");
        for (int i = parts.length - 1; i >= 0; i--) {
            String candidate = parts[i].trim();
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }
        return "unknown";
    }

    public Result<OrderDTO> seckillBlock(Long activityId, Long userId, String clientIp, String idempotencyToken, BlockException e) {
        log.warn("秒杀限流: userId={}, ip={}", userId, clientIp);
        return Result.error(ResultCode.RATE_LIMITED, "系统繁忙，请稍后重试");
    }

    @GetMapping("/list")
    public Result<List<SeckillActivity>> list() {
        return Result.success(flashSaleService.listActive());
    }

    @GetMapping("/admin/list")
    public Result<List<SeckillActivity>> adminList() {
        return Result.success(flashSaleService.listAll());
    }

    @GetMapping("/detail/{id}")
    public Result<SeckillActivity> detail(@PathVariable Long id) {
        return Result.success(flashSaleService.getById(id));
    }

    @PostMapping("/create")
    public Result<Void> create(@RequestBody SeckillActivity activity,
                               @RequestParam(value = "autoWarmUp", defaultValue = "false") boolean autoWarmUp) {
        flashSaleService.create(activity, autoWarmUp);
        return Result.success();
    }
}
