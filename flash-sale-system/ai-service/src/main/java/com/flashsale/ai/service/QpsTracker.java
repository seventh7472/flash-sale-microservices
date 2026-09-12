 package com.flashsale.ai.service;

 import lombok.extern.slf4j.Slf4j;
 import org.springframework.scheduling.annotation.EnableScheduling;
 import org.springframework.scheduling.annotation.Scheduled;
 import org.springframework.stereotype.Component;

 import javax.annotation.PostConstruct;
 import java.time.LocalDateTime;
 import java.time.format.DateTimeFormatter;
 import java.util.ArrayList;
 import java.util.List;
 import java.util.concurrent.ConcurrentHashMap;
 import java.util.concurrent.atomic.AtomicLong;

 @Slf4j
 @Component
 @EnableScheduling
 public class QpsTracker {

     private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

     // key = "endpoint:yyyyMMddHHmm", value = 请求计数
     private final ConcurrentHashMap<String, AtomicLong> store = new ConcurrentHashMap<>();
     private volatile String currentSlot = "";

     @PostConstruct
     void init() {
         currentSlot = LocalDateTime.now().format(FMT);
         log.info("QpsTracker 已启动, 初始 slot={}", currentSlot);
     }

     /** 记录一次请求 */
     public void record(String endpoint) {
         String key = endpoint + ":" + currentSlot;
         store.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
     }

     /** 获取最近 N 分钟的请求数列表（index 0=最旧） */
     public List<Integer> getRecentMinutes(String endpoint, int minutes) {
         LocalDateTime now = LocalDateTime.now();
         List<Integer> result = new ArrayList<>();
         for (int i = minutes - 1; i >= 0; i--) {
             String slot = now.minusMinutes(i).format(FMT);
             AtomicLong v = store.get(endpoint + ":" + slot);
             result.add(v != null ? v.intValue() : 0);
         }
         return result;
     }

     /** 每分钟切换 slot */
     @Scheduled(fixedRate = 60_000)
     void rotate() {
         currentSlot = LocalDateTime.now().format(FMT);
     }

     /** 清理超过30分钟的旧数据 */
     @Scheduled(fixedRate = 300_000)
     void cleanup() {
         String cutoff = LocalDateTime.now().minusMinutes(30).format(FMT);
         store.keySet().removeIf(k -> {
             String[] parts = k.split(":");
             return parts.length >= 2 && parts[parts.length - 1].compareTo(cutoff) < 0;
         });
     }
 }
