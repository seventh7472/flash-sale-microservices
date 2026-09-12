 package com.flashsale.flashsale.ai;
 import lombok.RequiredArgsConstructor;
 import org.redisson.api.*;
 import org.springframework.stereotype.Component;
 import java.util.List;
 import java.time.Duration;
import java.util.concurrent.TimeUnit;

 @Component @RequiredArgsConstructor
 public class FeatureCollector {
     private final RedissonClient redisson;

     public void recordRequest(Long userId, String ip, Long activityId) {
         String fk = "cheat:freq:" + userId;
         RBucket<Integer> fb = redisson.getBucket(fk);
         Integer v = fb.get(); fb.set(v == null ? 1 : v + 1, 1, TimeUnit.SECONDS);
         // 累计请求计数（1 小时有效），供防作弊监控展示“请求次数”；
         // cheat:freq 只有 1 秒 TTL，只能表达瞬时频率，不能当总请求数
         RAtomicLong reqCounter = redisson.getAtomicLong("cheat:req:" + userId);
         reqCounter.incrementAndGet();
         reqCounter.expire(Duration.ofHours(1));
         // 回环/未知 IP 不参与多账号维度统计，避免本地共享 IP 造成误判
         if (ip != null && !"unknown".equals(ip) && !isLoopback(ip)) {
             redisson.getSet("cheat:ip:" + ip).add(String.valueOf(userId));
             redisson.getSet("cheat:ip:" + ip).expire(Duration.ofHours(1));
         }
         RList<Long> tl = redisson.getList("cheat:tl:" + userId);
         tl.add(System.currentTimeMillis());
         tl.expire(Duration.ofHours(1));
         while (tl.size() > 10) tl.remove(0);
         String sk = "cheat:act:" + activityId + ":" + (System.currentTimeMillis() / 1000);
         RBucket<Integer> sb = redisson.getBucket(sk);
         Integer sv = sb.get(); sb.set(sv == null ? 1 : sv + 1, 3, TimeUnit.SECONDS);
     }
     private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("127.");
    }

    public int getFreq(Long uid) { Integer v = redisson.<Integer>getBucket("cheat:freq:" + uid).get(); return v == null ? 1 : v; }
     /** 该用户累计请求次数（cheat:req，1 小时滑动窗口），无记录返回 0 */
     public int getRequestCount(Long uid) {
         if (uid == null) return 0;
         long v = redisson.getAtomicLong("cheat:req:" + uid).get();
         return (int) Math.min(v, Integer.MAX_VALUE);
     }
     public int getAccountCount(String ip) { return redisson.getSet("cheat:ip:" + ip).size(); }
     public List<Long> getTimeline(Long uid) { return redisson.<Long>getList("cheat:tl:" + uid).readAll(); }
     public int getConcurrent(Long aid) {
         String k = "cheat:act:" + aid + ":" + (System.currentTimeMillis() / 1000);
         Integer v = redisson.<Integer>getBucket(k).get(); return v == null ? 0 : v;
     }
 }
