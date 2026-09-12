 package com.flashsale.flashsale.entity;

 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.math.BigDecimal;
 import java.time.LocalDateTime;

 @Data
 @TableName("seckill_activity")
 public class SeckillActivity {
     @TableId(type = IdType.AUTO) private Long id;
     private Long productId;
     private String activityName;
     private BigDecimal flashPrice;
     private Integer totalStock;
     private Integer limitQuantity;
     private Integer qpsLimit;
     private LocalDateTime startTime;
     private LocalDateTime endTime;
     private Integer status;
     @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
     private LocalDateTime updateTime;
 }
