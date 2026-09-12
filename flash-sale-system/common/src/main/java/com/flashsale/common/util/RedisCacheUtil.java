 package com.flashsale.common.util;

 import lombok.RequiredArgsConstructor;
 import org.redisson.api.RBucket;
 import org.redisson.api.RLock;
 import org.redisson.api.RedissonClient;
 import org.springframework.stereotype.Component;

 import java.util.concurrent.ThreadLocalRandom;
 import java.util.concurrent.TimeUnit;
 import java.util.function.Supplier;

 @Component
 @RequiredArgsConstructor
 public class RedisCacheUtil {
     private final RedissonClient redissonClient;

     public <T> T getOrCompute(String key, long ttlSec, Supplier<T> loader) {
         RBucket<T> bucket = redissonClient.getBucket(key);
         T cached = bucket.get();
         if (cached != null) return cached;
         RLock lock = redissonClient.getLock("cache:mutex:" + key);
         try {
             if (lock.tryLock(100, TimeUnit.MILLISECONDS)) {
                 cached = bucket.get();
                 if (cached != null) return cached;
                 T v = loader.get();
                 bucket.set(v, ttlSec + ThreadLocalRandom.current().nextInt(10), TimeUnit.SECONDS);
                 return v;
             }
         } catch (InterruptedException e) { Thread.currentThread().interrupt();
         } finally { if (lock.isHeldByCurrentThread()) lock.unlock(); }
         return loader.get();
     }

     public void evict(String key) { redissonClient.getBucket(key).delete(); }
 }
