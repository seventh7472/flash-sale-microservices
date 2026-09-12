 package com.flashsale.auth.controller;

 import com.flashsale.auth.dto.LoginRequest;
 import com.flashsale.auth.service.AuthService;
 import com.flashsale.common.dto.Result;
 import lombok.RequiredArgsConstructor;
 import org.springframework.web.bind.annotation.*;

 import javax.validation.Valid;

 @RestController
 @RequestMapping("/api/auth")
 @RequiredArgsConstructor
 public class AuthController {
     private final AuthService authService;

     @PostMapping("/login")
     public Result<String> login(@Valid @RequestBody LoginRequest req) {
         return authService.login(req);
     }

     @PostMapping("/register")
     public Result<Void> register(@Valid @RequestBody LoginRequest req) {
         return authService.register(req);
     }
 }
