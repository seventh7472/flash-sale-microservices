package com.flashsale.inventory.service;

import com.flashsale.inventory.mapper.StockMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockSyncService {
    private final StockMapper stockMapper;

    @Async("stockSyncExecutor")
    public void syncSoldCount(Long productId, int delta) {
        try {
            int rows = delta > 0
                    ? stockMapper.addSoldCount(productId, delta)
                    : stockMapper.unlockStock(productId, -delta);
            log.info("MySQL sold_count 同步: productId={}, delta={}, rows={}", productId, delta, rows);
        } catch (Exception e) {
            log.error("MySQL sold_count 同步失败: productId={}, delta={}", productId, delta, e);
        }
    }
}
