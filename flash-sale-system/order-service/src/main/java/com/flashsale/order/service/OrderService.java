package com.flashsale.order.service;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.common.constant.ResultCode;
import com.flashsale.common.dto.Result;
import com.flashsale.common.exception.BusinessException;
import com.flashsale.common.util.RedisCacheUtil;
import com.flashsale.order.dto.ProductDTO;
import com.flashsale.order.entity.SeckillOrder;
import com.flashsale.order.feign.InventoryClient;
import com.flashsale.order.feign.ProductClient;
import com.flashsale.order.mapper.SeckillOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final SeckillOrderMapper orderMapper;
    private final RedissonClient redissonClient;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;
    private final RedisCacheUtil redisCacheUtil;

    @Transactional
    public SeckillOrder createFlashOrder(Long userId, Long productId, Long activityId,
                                   BigDecimal flashPrice, Integer quantity) {
        String lockKey = "order:lock:" + userId + ":" + activityId;
        RLock lock = redissonClient.getLock(lockKey);
        try {
            lock.lock();
            Long exists = orderMapper.selectCount(
                    new LambdaQueryWrapper<SeckillOrder>()
                            .eq(SeckillOrder::getUserId, userId)
                            .eq(SeckillOrder::getActivityId, activityId)
                            .in(SeckillOrder::getStatus, 0, 1));
            if (exists > 0) throw new BusinessException(ResultCode.DUPLICATE_ORDER, "您已参与过此秒杀");
            SeckillOrder o = new SeckillOrder();
            o.setOrderNo(IdUtil.getSnowflakeNextIdStr());
            o.setUserId(userId); o.setProductId(productId); o.setActivityId(activityId);
            o.setQuantity(quantity); o.setFlashPrice(flashPrice);
            o.setTotalPrice(flashPrice.multiply(BigDecimal.valueOf(quantity)));
            o.setStatus(0);
            orderMapper.insert(o);
            redisCacheUtil.evict("orders:user:" + userId);
            return o;
        } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
    }

    @Transactional
    public SeckillOrder createNormalOrder(Long userId, Long productId, Integer quantity) {
        if (productId == null || quantity == null || quantity < 1) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "商品或数量不合法");
        }
        Result<ProductDTO> pr = productClient.getById(productId);
        if (pr == null || pr.getData() == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "商品不存在或服务不可用");
        }
        BigDecimal price = pr.getData().getOriginalPrice();
        SeckillOrder o = new SeckillOrder();
        o.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        o.setUserId(userId); o.setProductId(productId); o.setActivityId(null);
        o.setQuantity(quantity); o.setFlashPrice(price);
        o.setTotalPrice(price.multiply(BigDecimal.valueOf(quantity)));
        o.setStatus(0);
        orderMapper.insert(o);
        redisCacheUtil.evict("orders:user:" + userId);
        return o;
    }

    @Scheduled(fixedRate = 30000)
    public void cancelExpiredOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        List<SeckillOrder> expired = orderMapper.selectList(
                new LambdaQueryWrapper<SeckillOrder>()
                        .eq(SeckillOrder::getStatus, 0).lt(SeckillOrder::getCreateTime, deadline));
        if (expired.isEmpty()) return;
        for (SeckillOrder o : expired) {
            o.setStatus(2); orderMapper.updateById(o);
            redisCacheUtil.evict("order:no:" + o.getOrderNo());
            redisCacheUtil.evict("orders:user:" + o.getUserId());
            try { inventoryClient.releaseStock(o.getActivityId(), o.getProductId(), o.getQuantity()); }
            catch (Exception e) { log.error("释放库存失败: {}", o.getOrderNo(), e); }
        }
    }

    @Transactional
    public void cancelOrder(String orderNo, Long userId) {
        SeckillOrder o = orderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getOrderNo, orderNo));
        if (o != null && !o.getUserId().equals(userId))
            throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该订单");
        if (o != null && o.getStatus() == 0) {
            o.setStatus(2); orderMapper.updateById(o);
            // 秒杀订单取消时释放 Redis 库存，避免库存泄漏
            if (o.getActivityId() != null) {
                try {
                    inventoryClient.releaseStock(o.getActivityId(), o.getProductId(), o.getQuantity());
                } catch (Exception e) {
                    log.error("取消订单释放库存失败: orderNo={}", orderNo, e);
                }
            }
        }
        if (o != null) { redisCacheUtil.evict("order:no:" + orderNo); redisCacheUtil.evict("orders:user:" + o.getUserId()); }
    }

    @Transactional
    public void payOrder(String orderNo, Long userId) {
        SeckillOrder o = orderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getOrderNo, orderNo));
        if (o == null) throw new BusinessException(ResultCode.NOT_FOUND, "订单不存在");
        if (!o.getUserId().equals(userId)) throw new BusinessException(ResultCode.FORBIDDEN, "无权操作该订单");
        if (o.getStatus() != 0) throw new BusinessException(ResultCode.BUSINESS_ERROR, "订单状态不允许支付");
        o.setStatus(1); o.setPayTime(LocalDateTime.now()); orderMapper.updateById(o);
        redisCacheUtil.evict("orders:user:" + o.getUserId());
        redisCacheUtil.evict("order:no:" + orderNo);
    }

    public List<SeckillOrder> getUserOrders(Long userId) {
        return redisCacheUtil.getOrCompute("orders:user:" + userId, 300, () ->
            orderMapper.selectList(
                new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getUserId, userId)
                        .orderByDesc(SeckillOrder::getCreateTime)));
    }

    public SeckillOrder getByOrderNo(String orderNo, Long userId) {
        SeckillOrder o = redisCacheUtil.getOrCompute("order:no:" + orderNo, 300, () ->
            orderMapper.selectOne(
                new LambdaQueryWrapper<SeckillOrder>().eq(SeckillOrder::getOrderNo, orderNo)));
        return (o != null && o.getUserId().equals(userId)) ? o : null;
    }
}