 package com.flashsale.common.util;

 import com.flashsale.common.constant.ResultCode;
 import com.flashsale.common.exception.BusinessException;
 import lombok.RequiredArgsConstructor;
 import org.redisson.api.RLock;
 import org.redisson.api.RedissonClient;
 import org.springframework.stereotype.Component;

 import java.util.concurrent.TimeUnit;
 import java.util.function.Supplier;

 @Component
 @RequiredArgsConstructor
 public class RedissonLockUtil {
     private final RedissonClient redissonClient;

     public void runWithLock(String lockKey, long waitMs, Runnable action) {
         RLock lock = redissonClient.getLock(lockKey);
         try {
             try { if (!lock.tryLock(waitMs, TimeUnit.MILLISECONDS))
                 throw new BusinessException(ResultCode.RATE_LIMITED, "系统繁忙");
             } catch (InterruptedException e) { Thread.currentThread().interrupt();
                 throw new BusinessException(ResultCode.RATE_LIMITED, "系统繁忙"); }
             action.run();
         } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
     }

     public <T> T getWithLock(String lockKey, long waitMs, Supplier<T> supplier) {
         RLock lock = redissonClient.getLock(lockKey);
         try {
             try { if (!lock.tryLock(waitMs, TimeUnit.MILLISECONDS))
                 throw new BusinessException(ResultCode.RATE_LIMITED, "系统繁忙");
             } catch (InterruptedException e) { Thread.currentThread().interrupt();
                 throw new BusinessException(ResultCode.RATE_LIMITED, "系统繁忙"); }
             return supplier.get();
         } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
     }
 }
