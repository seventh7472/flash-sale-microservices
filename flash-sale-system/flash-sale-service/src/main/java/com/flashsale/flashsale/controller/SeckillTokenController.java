package com.flashsale.flashsale.controller;

import cn.hutool.core.util.IdUtil;
import com.flashsale.common.dto.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
@RequiredArgsConstructor
public class SeckillTokenController {

    @GetMapping("/token")
    public Result<String> token(@RequestHeader("X-User-Id") Long userId) {
        return Result.success(IdUtil.simpleUUID());
    }
}
