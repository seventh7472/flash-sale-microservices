 package com.flashsale.order.entity;

 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.math.BigDecimal;
 import java.time.LocalDateTime;

 @Data
 @TableName("seckill_order")
 public class SeckillOrder {
     @TableId(type = IdType.AUTO) private Long id;
     private String orderNo;
     private Long userId;
     private Long productId;
     private Long activityId;
     private Integer quantity;
     private BigDecimal flashPrice;
     private BigDecimal totalPrice;
     private Integer status;
     private LocalDateTime payTime;
     private LocalDateTime createTime;
     private LocalDateTime updateTime;
 }
