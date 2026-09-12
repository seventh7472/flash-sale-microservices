package com.flashsale.flashsale.service;

import com.flashsale.common.constant.ResultCode;
import com.flashsale.common.dto.Result;
import com.flashsale.flashsale.dto.OrderDTO;
import com.flashsale.flashsale.entity.SeckillActivity;
import com.flashsale.flashsale.ai.CheatingRuleEngine;
import com.flashsale.flashsale.ai.FeatureCollector;
import com.flashsale.flashsale.ai.BlacklistService;
import com.flashsale.flashsale.model.CheatingReport;
import com.flashsale.flashsale.feign.GoodsClient;
import com.flashsale.flashsale.feign.InventoryClient;
import com.flashsale.flashsale.feign.OrderClient;
import com.flashsale.flashsale.mapper.SeckillActivityMapper;
import org.junit.jupiter.api.*;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FlashSaleConcurrentTest {

// ── 真实 Feign 实现（线程安全） ──
    static class TInv implements InventoryClient {
    final AtomicInteger s = new AtomicInteger();
    void init(int v) { s.set(v); }
public Result<Boolean> decreaseStock(Long activityId, Long productId) { return Result.success(s.getAndDecrement() > 0); }
public Result<Void> releaseStock(Long activityId, Long productId, Integer q) { s.addAndGet(q); return Result.success(); }
public Result<Void> warmUpStock(Long a, Integer q) { return Result.success(); }
    }
    static class TOrd implements OrderClient {
    final ConcurrentHashMap<String,Boolean> m = new ConcurrentHashMap<>();
    void init() { m.clear(); }
public Result<OrderDTO> createFlashOrder(Long u,Long p,Long a,BigDecimal f,Integer q) {
    if (m.putIfAbsent(u+":"+a,Boolean.TRUE) != null)
    throw new RuntimeException("DUP");
    OrderDTO o = new OrderDTO(); o.setOrderNo("N"+System.nanoTime()); o.setUserId(u); o.setActivityId(a);
    return Result.success(o);
    }
    }

// ── Redis Bucket 线程安全真实实现 ──
    static class TRedis {
    final ConcurrentHashMap<String,Boolean> store = new ConcurrentHashMap<>();
    final ThreadLocal<String> key = new ThreadLocal<>();
    void track(String k) { key.set(k); }
    void init() { store.clear(); }
    boolean trySet() { String k=key.get(); return k==null||store.putIfAbsent(k,Boolean.TRUE)==null; }
    boolean delete() { String k=key.get(); return k!=null&&store.remove(k)!=null; }
    }

// ── 测试 ──
    private FlashSaleService svc;
    private TInv inv;
    private TOrd ord;
    private TRedis rds;

@BeforeEach
    void setUp() {
    inv=new TInv(); ord=new TOrd(); rds=new TRedis(); CheatingRuleEngine engine=new CheatingRuleEngine();

// Redisson mock: getBucket 返回 mock RBucket，但 trySet/delete 代理到 TRedis
    RedissonClient redisson=mock(RedissonClient.class);
    when(redisson.getBucket(anyString())).thenAnswer(i->{
    rds.track(i.getArgument(0));
@SuppressWarnings("unchecked")
    RBucket<Object> b=mock(RBucket.class);
    when(b.trySet(any(),anyLong(),any())).thenAnswer(j->rds.trySet());
    when(b.delete()).thenAnswer(j->rds.delete());
    return b;
    });

// Mapper
    SeckillActivityMapper mapper=mock(SeckillActivityMapper.class);
    SeckillActivity a=new SeckillActivity();
    a.setId(1L);a.setProductId(100L);a.setFlashPrice(new BigDecimal("99"));a.setLimitQuantity(1);
    a.setStatus(1);a.setStartTime(LocalDateTime.now().minusHours(1));a.setEndTime(LocalDateTime.now().plusHours(1));
    when(mapper.selectById(1L)).thenReturn(a);

    svc=new FlashSaleService(mapper,redisson,inv,ord,mock(GoodsClient.class),engine,mock(FeatureCollector.class),mock(BlacklistService.class));
    }

@Test void t1_100thread_10stock() throws Exception {
    int N=100,S=10;inv.init(S);ord.init();rds.init();
    var p=Executors.newFixedThreadPool(N);var b=new CyclicBarrier(N);var l=new CountDownLatch(N);
    var ok=new AtomicInteger();var fail=new AtomicInteger();var err=new ConcurrentLinkedQueue<String>();
    for(int i=0;i<N;i++){long u=1001L+i;
    p.submit(()->{try{b.await(5,TimeUnit.SECONDS);
    Result<?> r=svc.seckill(u,"0.0.0.0",1L,"test-token-"+u);
    if(r.getCode()==200)ok.incrementAndGet();
    else if(r.getCode()==ResultCode.STOCK_NOT_ENOUGH.getCode())fail.incrementAndGet();
    else err.add("c="+r.getCode()+":"+r.getMessage());
    }catch(Exception e){err.add(e.getClass().getSimpleName());}finally{l.countDown();}});}
    assertTrue(l.await(30,TimeUnit.SECONDS));p.shutdown();
    assertEquals(S,ok.get());assertEquals(N-S,fail.get());assertTrue(err.isEmpty(),String.join(";",err));
    }

@Test void t2_sameUser_dup() throws Exception {
    inv.init(5);ord.init();rds.init();
    var p=Executors.newFixedThreadPool(10);var b=new CyclicBarrier(10);var l=new CountDownLatch(10);
    var ok=new AtomicInteger();var dup=new AtomicInteger();var err=new ConcurrentLinkedQueue<String>();
    for(int i=0;i<10;i++){p.submit(()->{try{b.await(5,TimeUnit.SECONDS);
    Result<?> r=svc.seckill(999L,"0.0.0.0",1L,"test-token-"+System.nanoTime());
    if(r.getCode()==200)ok.incrementAndGet();
    else if(r.getCode()==ResultCode.DUPLICATE_ORDER.getCode())dup.incrementAndGet();
    else err.add("c="+r.getCode()+":"+r.getMessage());
    }catch(Exception e){err.add(e.getMessage());}finally{l.countDown();}});}
    l.await(30,TimeUnit.SECONDS);p.shutdown();
    assertEquals(1,ok.get());assertEquals(9,dup.get());assertTrue(err.isEmpty(),String.join(";",err));
    }

@Test void t3_1stock_10req() throws Exception {
    inv.init(1);ord.init();rds.init();
    var p=Executors.newFixedThreadPool(10);var b=new CyclicBarrier(10);var l=new CountDownLatch(10);
    var ok=new AtomicInteger();var fail=new AtomicInteger();var err=new ConcurrentLinkedQueue<String>();
    for(int i=0;i<10;i++){long u=2001L+i;
    p.submit(()->{try{b.await(5,TimeUnit.SECONDS);
    Result<?> r=svc.seckill(u,"0.0.0.0",1L,"test-token-"+u);
    if(r.getCode()==200)ok.incrementAndGet();
    else if(r.getCode()==ResultCode.STOCK_NOT_ENOUGH.getCode())fail.incrementAndGet();
    else err.add("c="+r.getCode()+":"+r.getMessage());
    }catch(Exception e){err.add(e.getMessage());}finally{l.countDown();}});}
    l.await(30,TimeUnit.SECONDS);p.shutdown();
    assertEquals(1,ok.get());assertEquals(9,fail.get());assertTrue(err.isEmpty(),String.join(";",err));
    }
    }
