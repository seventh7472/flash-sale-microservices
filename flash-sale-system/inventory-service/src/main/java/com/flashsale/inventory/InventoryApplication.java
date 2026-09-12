 package com.flashsale.inventory;

 import org.springframework.boot.SpringApplication;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

 @SpringBootApplication(scanBasePackages = {"com.flashsale"})
 @EnableDiscoveryClient
 public class InventoryApplication {
     public static void main(String[] args) {
         SpringApplication.run(InventoryApplication.class, args);
     }
 }
