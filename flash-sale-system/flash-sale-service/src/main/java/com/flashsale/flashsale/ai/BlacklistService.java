package com.flashsale.flashsale.ai;

import com.flashsale.flashsale.entity.CheatBlacklist;
import com.flashsale.flashsale.mapper.CheatBlacklistMapper;
import com.flashsale.flashsale.model.CheatUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistService {
    private final RedissonClient redisson;
    private final CheatBlacklistMapper blacklistMapper;

    public void block(Long userId, String ip, String reason, int score) {
        if (userId != null) redisson.getBucket("cheat:block:u:" + userId).set("1", 24, TimeUnit.HOURS);
        // 回环/未知 IP 不写入 IP 黑名单，避免误伤整个本机/内网
        if (ip != null && !"unknown".equals(ip) && !isLoopback(ip)) {
            redisson.getBucket("cheat:block:ip:" + ip).set("1", 24, TimeUnit.HOURS);
        }
        CheatBlacklist cb = new CheatBlacklist();
        cb.setUserId(userId); cb.setIp(ip); cb.setReason(reason);
        cb.setRiskScore(score); cb.setIsActive(1);
        cb.setExpireTime(LocalDateTime.now().plusDays(1));
        blacklistMapper.insert(cb);
    }

    /** 记录可疑用户（供 AI 防作弊监控展示，1 小时有效）；reason 为检测瞬间的规则引擎判定理由 */
    public void markSuspicious(Long userId, String ip, int score, String reason) {
        if (userId == null) return;
        CheatUser u = new CheatUser();
        u.setUserId(userId);
        u.setIp(ip);
        u.setRiskScore(score);
        u.setReason(reason);
        redisson.getMapCache("cheat:suspicious").put(userId, u, 1, TimeUnit.HOURS);
    }

    public boolean isBlocked(Long userId, String ip) {
        try {
            if (userId != null && redisson.getBucket("cheat:block:u:" + userId).get() != null) return true;
            if (ip != null && redisson.getBucket("cheat:block:ip:" + ip).get() != null) return true;
        } catch (Exception e) {
            log.warn("黑名单校验异常，视为未拉黑: {}", e.getMessage());
        }
        return false;
    }

    private boolean isLoopback(String ip) {
        return "127.0.0.1".equals(ip) || "::1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || ip.startsWith("127.");
    }
}