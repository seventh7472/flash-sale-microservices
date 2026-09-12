 package com.flashsale.order;

 import org.springframework.boot.SpringApplication;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
 import org.springframework.cloud.openfeign.EnableFeignClients;
 import org.springframework.scheduling.annotation.EnableScheduling;

 @SpringBootApplication(scanBasePackages = {"com.flashsale"})
 @EnableDiscoveryClient
 @EnableScheduling
 @EnableFeignClients
 public class OrderApplication {
     public static void main(String[] args) {
         SpringApplication.run(OrderApplication.class, args);
     }
 }
