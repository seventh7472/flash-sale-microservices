package com.flashsale.flashsale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.common.constant.ResultCode;
import com.flashsale.common.dto.Result;
import com.flashsale.common.exception.BusinessException;
import com.flashsale.flashsale.ai.CheatingRuleEngine;
import com.flashsale.flashsale.ai.FeatureCollector;
import com.flashsale.flashsale.ai.BlacklistService;
import com.flashsale.flashsale.dto.OrderDTO;
import com.flashsale.flashsale.entity.SeckillActivity;
import com.flashsale.flashsale.feign.GoodsClient;
import com.flashsale.flashsale.feign.InventoryClient;
import com.flashsale.flashsale.feign.OrderClient;
import com.flashsale.flashsale.feign.ProductDTO;
import com.flashsale.flashsale.mapper.SeckillActivityMapper;
import com.flashsale.flashsale.model.CheatingReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {
    private final SeckillActivityMapper activityMapper;
    private final RedissonClient redissonClient;
    private final InventoryClient inventoryClient;
    private final OrderClient orderClient;
    private final GoodsClient goodsClient;
    private final CheatingRuleEngine cheatingRuleEngine;
   private final FeatureCollector featureCollector;
   private final BlacklistService blacklistService;

    private static final String USER_BUY_KEY = "flash:user:buy:";

    public Result<OrderDTO> seckill(Long userId, String clientIp, Long activityId, String idempotencyToken) {
        SeckillActivity activity = activityMapper.selectById(activityId);
        if (activity == null) return Result.error(ResultCode.NOT_FOUND, "活动不存在");
        // 已取消(status=3)的活动不允许参与，即使时间窗口内
        if (activity.getStatus() != null && activity.getStatus() == 3)
            return Result.error(ResultCode.FLASH_SALE_ENDED, "活动已取消");
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(activity.getStartTime()))
            return Result.error(ResultCode.FLASH_SALE_NOT_STARTED, "秒杀未开始");
        if (now.isAfter(activity.getEndTime()))
            return Result.error(ResultCode.FLASH_SALE_ENDED, "秒杀已结束");

        // 黑名单拦截：被 AI 拉黑的用户/IP 直接拒绝
        if (blacklistService.isBlocked(userId, clientIp)) {
            log.warn("黑名单拦截: userId={}, ip={}", userId, clientIp);
            return Result.error(ResultCode.CHEATING_DETECTED, "账号已被系统临时封禁，禁止参与秒杀");
        }

        if (idempotencyToken == null || idempotencyToken.isBlank()) {
            return Result.error(ResultCode.VALIDATE_FAILED, "缺少幂等Token");
        }
        String idemKey = "idempotent:order:" + userId + ":" + idempotencyToken;
        boolean firstUse = redissonClient.getBucket(idemKey).trySet("1", 5, TimeUnit.MINUTES);
        if (!firstUse) {
            return Result.error(ResultCode.DUPLICATE_ORDER, "幂等Token已使用，请勿重复提交");
        }

        featureCollector.recordRequest(userId, clientIp, activityId);
        int freq = featureCollector.getFreq(userId);
       int accounts = featureCollector.getAccountCount(clientIp);
       List<Long> tl = featureCollector.getTimeline(userId);
       int concurrent = featureCollector.getConcurrent(activityId);
       CheatingReport aiReport = cheatingRuleEngine.evaluate(userId, clientIp, freq, accounts, tl, concurrent);
        // 可疑/高风险用户进入监控列表（1小时有效）
        if (aiReport.isReview() || aiReport.isHighRisk()) {
            blacklistService.markSuspicious(userId, clientIp, (int) aiReport.getRiskScore(), aiReport.getReason());
        }
        if (aiReport.isHighRisk()) {
            log.warn("AI拦截作弊: userId={}, activityId={}", userId, activityId);
            blacklistService.block(userId, clientIp, aiReport.getReason(), (int)aiReport.getRiskScore());
            redissonClient.getBucket(idemKey).delete();
            return Result.error(ResultCode.CHEATING_DETECTED, aiReport.getReason());
        }

        String buyKey = USER_BUY_KEY + activityId + ":" + userId;
        long ttl = ChronoUnit.SECONDS.between(now, activity.getEndTime());
        boolean notBought = redissonClient.getBucket(buyKey).trySet("1", Math.max(ttl, 60), TimeUnit.SECONDS);
        if (!notBought) {
            redissonClient.getBucket(idemKey).delete();
            return Result.error(ResultCode.DUPLICATE_ORDER, "您已参与过此秒杀");
        }

        Result<Boolean> stockResult = inventoryClient.decreaseStock(activityId, activity.getProductId());
        boolean stockOk = stockResult != null && Boolean.TRUE.equals(stockResult.getData());
        if (!stockOk) {
            redissonClient.getBucket(buyKey).delete();
            redissonClient.getBucket(idemKey).delete();
            return Result.error(ResultCode.STOCK_NOT_ENOUGH, "库存不足");
        }

        try {
            Result<OrderDTO> orderResult = orderClient.createFlashOrder(
                    userId, activity.getProductId(), activityId, activity.getFlashPrice(), activity.getLimitQuantity());
            if (orderResult == null || orderResult.getData() == null)
                throw new RuntimeException("订单创建失败");
            return Result.success(orderResult.getData());
        } catch (Exception e) {
            log.error("订单失败，回滚库存: activityId={}", activityId, e);
            try { inventoryClient.releaseStock(activityId, activity.getProductId(), 1); } catch (Exception ex) { log.error("回滚失败", ex); }
            redissonClient.getBucket(buyKey).delete();
            redissonClient.getBucket(idemKey).delete();
            return Result.error(ResultCode.BUSINESS_ERROR, "下单失败，请重试");
        }
    }

    public List<SeckillActivity> listActive() {
        LocalDateTime now = LocalDateTime.now();
        return activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .eq(SeckillActivity::getStatus, 1).ge(SeckillActivity::getEndTime, now)
                .orderByAsc(SeckillActivity::getStartTime));
    }

    public SeckillActivity getById(Long id) { return activityMapper.selectById(id); }

    public List<SeckillActivity> listAll() {
        return activityMapper.selectList(new LambdaQueryWrapper<SeckillActivity>()
                .orderByDesc(SeckillActivity::getId));
    }

   @Scheduled(fixedRate = 10000)
   public void autoUpdateStatus() {
       LocalDateTime now = LocalDateTime.now();
       List<SeckillActivity> waiting = activityMapper.selectList(
           new LambdaQueryWrapper<SeckillActivity>()
               .eq(SeckillActivity::getStatus, 0)
               .le(SeckillActivity::getStartTime, now));
       for (SeckillActivity a : waiting) {
           a.setStatus(1);
           activityMapper.updateById(a);
           log.info("auto status: activityId={}, WAIT -> ACTIVE", a.getId());
       }
       List<SeckillActivity> active = activityMapper.selectList(
           new LambdaQueryWrapper<SeckillActivity>()
               .eq(SeckillActivity::getStatus, 1)
               .le(SeckillActivity::getEndTime, now));
       for (SeckillActivity a : active) {
           a.setStatus(2);
           activityMapper.updateById(a);
           log.info("auto status: activityId={}, ACTIVE -> ENDED", a.getId());
       }
   }

   @Scheduled(fixedRate = 60000)
   public void autoWarmUp() {
       LocalDateTime now = LocalDateTime.now();
       List<SeckillActivity> upcoming = activityMapper.selectList(
           new LambdaQueryWrapper<SeckillActivity>()
               .eq(SeckillActivity::getStatus, 0)
               .gt(SeckillActivity::getStartTime, now)
               .lt(SeckillActivity::getStartTime, now.plusHours(1)));
       for (SeckillActivity a : upcoming) {
           try { inventoryClient.warmUpStock(a.getId(), a.getTotalStock());
               log.info("Auto warm-up: activityId={}, stock={}", a.getId(), a.getTotalStock());
           } catch(Exception e) { log.warn("Warm-up failed: activityId={}", a.getId()); }
       }
   }
    public void create(SeckillActivity activity, boolean autoWarmUp) {
        // 基础校验
        if (activity == null) throw new BusinessException(ResultCode.VALIDATE_FAILED, "参数不能为空");
        if (activity.getActivityName() == null || activity.getActivityName().trim().isEmpty())
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "活动名称不能为空");
        if (activity.getProductId() == null)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "请选择商品");
        if (activity.getFlashPrice() == null || activity.getFlashPrice().compareTo(BigDecimal.ZERO) <= 0)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "秒杀价必须大于0");
        if (activity.getTotalStock() == null || activity.getTotalStock() <= 0)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "总库存必须大于0");
        if (activity.getStartTime() == null || activity.getEndTime() == null)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "开始/结束时间不能为空");
        if (!activity.getStartTime().isBefore(activity.getEndTime()))
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "开始时间必须早于结束时间");

        // 商品存在性校验
        Result<ProductDTO> product = goodsClient.getProduct(activity.getProductId());
        if (product == null || product.getData() == null)
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品不存在或商品服务不可用");

        // 默认值
        if (activity.getLimitQuantity() == null) activity.setLimitQuantity(1);
        if (activity.getQpsLimit() == null) activity.setQpsLimit(100);

        activity.setStatus(0);
        activity.setCreateTime(LocalDateTime.now());
        activityMapper.insert(activity);

        // 创建后自动预热
        if (autoWarmUp) {
            try {
                inventoryClient.warmUpStock(activity.getId(), activity.getTotalStock());
                log.info("创建后自动预热: activityId={}, stock={}", activity.getId(), activity.getTotalStock());
            } catch (Exception e) {
                log.warn("创建后自动预热失败: activityId={}, err={}", activity.getId(), e.getMessage());
            }
        }
    }
}