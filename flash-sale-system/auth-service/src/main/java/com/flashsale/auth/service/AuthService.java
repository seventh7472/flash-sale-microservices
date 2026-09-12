package com.flashsale.auth.service;

import com.flashsale.auth.dto.LoginRequest;
import com.flashsale.auth.entity.User;
import com.flashsale.auth.mapper.UserMapper;
import com.flashsale.common.constant.ResultCode;
import com.flashsale.common.dto.Result;
import com.flashsale.common.exception.BusinessException;
import com.flashsale.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private static final String LOGIN_FAIL_KEY = "login:fail:";
    private static final int MAX_LOGIN_FAIL = 5;
    private static final long LOCK_MINUTES = 15;

    public Result<String> login(LoginRequest req) {
        String failKey = LOGIN_FAIL_KEY + req.getUsername();
        RBucket<Integer> failBucket = redissonClient.getBucket(failKey);
        Integer failed = failBucket.get();
        if (failed != null && failed >= MAX_LOGIN_FAIL) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "登录失败次数过多，请" + LOCK_MINUTES + "分钟后再试");
        }

        User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, req.getUsername()));
        // BCrypt密码校验：账号不存在与密码错误返回同一提示，避免暴露用户是否存在
        if (user == null || !PASSWORD_ENCODER.matches(req.getPassword(), user.getPassword())) {
            int next = (failed == null ? 0 : failed) + 1;
            failBucket.set(next, LOCK_MINUTES, TimeUnit.MINUTES);
            if (next >= MAX_LOGIN_FAIL) {
                log.warn("登录失败次数过多已锁定: username={}, times={}", req.getUsername(), next);
                throw new BusinessException(ResultCode.RATE_LIMITED, "登录失败次数过多，已锁定" + LOCK_MINUTES + "分钟");
            }
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        failBucket.delete();
        String token = JwtUtil.generate(user.getId(), user.getUsername(), user.getRole());
        return Result.success(token);
    }

    public Result<Void> register(LoginRequest req) {
        // 简单注册频率限制：每分钟最多 30 次，防止批量注册
        String rateKey = "register:rate:" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        long regCount = redissonClient.getAtomicLong(rateKey).incrementAndGet();
        if (regCount == 1) {
            redissonClient.getAtomicLong(rateKey).expire(java.time.Duration.ofSeconds(60));
        }
        if (regCount > 30) {
            throw new BusinessException(ResultCode.RATE_LIMITED, "注册过于频繁，请稍后再试");
        }
        Long count = userMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<User>()
                        .eq(User::getUsername, req.getUsername()));
        if (count > 0) {
            throw new BusinessException(ResultCode.VALIDATE_FAILED, "用户名已存在");
        }
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(PASSWORD_ENCODER.encode(req.getPassword()));
        user.setRole("USER");
        user.setRiskLevel(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return Result.success();
    }
}