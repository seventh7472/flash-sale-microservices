 package com.flashsale.inventory.entity;

 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.time.LocalDateTime;

 @Data
 @TableName("stock")
 public class Stock {
     @TableId(type = IdType.AUTO) private Long id;
     private Long productId;
     private Integer totalStock;
     private Integer soldCount;
     private Integer version;
     @TableField(fill = FieldFill.INSERT) private LocalDateTime createTime;
     private LocalDateTime updateTime;
 }
