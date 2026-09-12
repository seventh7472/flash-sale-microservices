 package com.flashsale.flashsale.dto;

 import lombok.Data;
 import java.math.BigDecimal;
 import java.time.LocalDateTime;

 /** 秒杀服务本地订单DTO，避免跨服务依赖实体类 */
 @Data
 public class OrderDTO {
     private Long id;
     private String orderNo;
     private Long userId;
     private Long productId;
     private Long activityId;
     private Integer quantity;
     private BigDecimal totalPrice;
     private Integer status;
     private LocalDateTime createTime;
 }
