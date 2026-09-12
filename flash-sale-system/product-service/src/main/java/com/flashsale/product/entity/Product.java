 package com.flashsale.product.entity;

 import com.baomidou.mybatisplus.annotation.*;
 import lombok.Data;
 import java.math.BigDecimal;
 import java.time.LocalDateTime;

 @Data
 @TableName("product")
 public class Product {
     @TableId(type = IdType.AUTO)
     private Long id;
     private String name;
     private String description;
     private BigDecimal originalPrice;
     private String image;
     private Integer status;
     @TableField(fill = FieldFill.INSERT)
     private LocalDateTime createTime;
     private LocalDateTime updateTime;
 }
