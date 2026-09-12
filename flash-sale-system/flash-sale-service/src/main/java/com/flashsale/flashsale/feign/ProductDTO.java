 package com.flashsale.flashsale.feign;

 import lombok.Data;
 import java.math.BigDecimal;

 @Data
 public class ProductDTO {
     private Long id;
     private String name;
     private BigDecimal originalPrice;
     private String image;
     private String description;
     private Integer status;
 }
