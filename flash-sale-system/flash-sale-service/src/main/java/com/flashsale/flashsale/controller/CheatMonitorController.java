package com.flashsale.flashsale.controller;

import com.flashsale.common.dto.Result;
import com.flashsale.flashsale.entity.CheatBlacklist;
import com.flashsale.flashsale.model.CheatUser;
import com.flashsale.flashsale.service.CheatMonitorService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class CheatMonitorController {
    private final CheatMonitorService cheatMonitorService;

    @GetMapping("/cheat-users")
    public Result<List<CheatUser>> cheatUsers() {
        return Result.success(cheatMonitorService.listSuspicious());
    }

    @PostMapping("/cheat-ban/{userId}")
    public Result<Void> cheatBan(@PathVariable Long userId) {
        if (!cheatMonitorService.ban(userId)) {
            return Result.failed("用户不存在或已不在可疑列表");
        }
        return Result.success();
    }

    @GetMapping("/cheat-blacklist")
    public Result<List<CheatBlacklist>> cheatBlacklist() {
        return Result.success(cheatMonitorService.listBlacklist());
    }
}
