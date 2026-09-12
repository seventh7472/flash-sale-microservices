package com.flashsale.inventory.service;

 import com.flashsale.inventory.entity.Stock;
 import com.flashsale.inventory.mapper.StockMapper;
import com.flashsale.inventory.service.StockSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
 public class InventoryService {
    private final StockMapper stockMapper;
    private final RedissonClient redissonClient;
    private final StockSyncService stockSyncService;

    private static final String STOCK_KEY = "flash:sale:stock:";

    // Lua: 原子扣减1 → 返回 1(成功)/0(不足)/-1(无key)
    private static final String LUA_DECR =
            "local s = redis.call('get', KEYS[1]) " +
            "if not s then return -1 end " +
            "if tonumber(s) < 1 then return 0 end " +
            "redis.call('decr', KEYS[1]) return 1";

    // Lua: 原子释放库存 → 返回恢复后的库存
    private static final String LUA_INCR =
            "local s = redis.call('get', KEYS[1]) " +
            "if not s then return -1 end " +
            "return redis.call('incrby', KEYS[1], tonumber(ARGV[1]))";

    public void warmUpStock(Long activityId, Integer totalStock) {
        redissonClient.getAtomicLong(STOCK_KEY + activityId).set(totalStock.longValue());
        log.info("库存预热: activityId={}, stock={}", activityId, totalStock);
    }

    /** Lua原子扣减（主力） */
    public boolean decreaseStock(Long activityId, Long productId) {
        Long result = redissonClient.getScript().eval(RScript.Mode.READ_WRITE,
                LUA_DECR, RScript.ReturnType.INTEGER,
                Collections.singletonList(STOCK_KEY + activityId));
        if (result == null || result <= 0) {
            log.warn("库存不足: activityId={}", activityId);
            return false;
        }
        stockSyncService.syncSoldCount(productId, 1);
        return true;
    }

    /** 释放库存（订单超时取消时调用） */
    public Long releaseStock(Long activityId, Long productId, Integer quantity) {
        // 必须用 StringCodec + 字符串参数：默认编解码器会把 Integer 编码成二进制，Lua 的 tonumber 无法解析导致释放失败
        Long stock = redissonClient.getScript(org.redisson.client.codec.StringCodec.INSTANCE)
                .eval(RScript.Mode.READ_WRITE,
                LUA_INCR, RScript.ReturnType.INTEGER,
                Collections.singletonList(STOCK_KEY + activityId), String.valueOf(quantity));
        log.info("库存释放: activityId={}, productId={}, quantity={}, after={}", activityId, productId, quantity, stock);
        stockSyncService.syncSoldCount(productId, -quantity);
        return stock;
    }

    /** 兜底：Redisson锁 + 乐观锁 */
    public boolean decreaseStockWithLock(Long productId, int quantity) {
        RLock lock = redissonClient.getLock("inventory:lock:" + productId);
        try {
            lock.lock();
            Stock inv = stockMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Stock>()
                            .eq(Stock::getProductId, productId));
            if (inv == null) return false;
            int available = inv.getTotalStock() - inv.getSoldCount();
            if (available < quantity) return false;
            int rows = stockMapper.lockStock(productId, quantity, inv.getVersion());
            return rows > 0;
        } finally {
            if (lock.isHeldByCurrentThread()) lock.unlock();
        }
    }

    public Integer getRedisStock(Long activityId) {
        return (int) redissonClient.getAtomicLong(STOCK_KEY + activityId).get();
    }
}
