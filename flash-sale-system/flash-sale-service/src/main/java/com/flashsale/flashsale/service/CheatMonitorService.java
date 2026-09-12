package com.flashsale.flashsale.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.flashsale.flashsale.ai.BlacklistService;
import com.flashsale.flashsale.ai.CheatingRuleEngine;
import com.flashsale.flashsale.ai.FeatureCollector;
import com.flashsale.flashsale.entity.CheatBlacklist;
import com.flashsale.flashsale.mapper.CheatBlacklistMapper;
import com.flashsale.flashsale.mapper.UserMapper;
import com.flashsale.flashsale.model.CheatUser;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheatMonitorService {
    private static final String SUSPICIOUS_KEY = "cheat:suspicious";

    private final BlacklistService blacklistService;
    private final FeatureCollector featureCollector;
    private final CheatingRuleEngine ruleEngine;
    private final CheatBlacklistMapper blacklistMapper;
    private final UserMapper userMapper;
    private final RedissonClient redisson;

    /** 真实可疑用户：秒杀时被规则引擎标记为 review/highRisk 的用户（Redis 缓存 1 小时） */
    public List<CheatUser> listSuspicious() {
        List<CheatUser> list = new ArrayList<>(redisson.<Long, CheatUser>getMapCache(SUSPICIOUS_KEY).values());
        List<Long> userIds = list.stream()
                .map(CheatUser::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            Map<Long, String> names = new HashMap<>();
            Map<Long, String> roles = new HashMap<>();
            userMapper.selectBatchIds(userIds).forEach(u -> {
                names.put(u.getId(), u.getUsername());
                roles.put(u.getId(), u.getRole());
            });
            // 管理账号不纳入可疑/可封禁列表，避免误封管理员
            list.removeIf(u -> "ADMIN".equals(roles.get(u.getUserId())));
            list.forEach(u -> u.setUsername(names.get(u.getUserId())));
        }
        // 补充真实请求次数：从 Redis 累计计数读取，保证前端“请求次数/总请求数”为真实值
        list.forEach(u -> u.setRequestCount(featureCollector.getRequestCount(u.getUserId())));
        // 补充“检测理由”：为可疑用户生成 DeepSeek 自然语言说明（可降级，失败时前端回退展示规则 reason）
        list.forEach(u -> {
            if (u.getAiAnalysis() == null) {
                u.setAiAnalysis(ruleEngine.analyzeSuspicious(
                        u.getUserId(), u.getIp(),
                        u.getRiskScore() == null ? 0 : u.getRiskScore(),
                        u.getReason()));
            }
        });
        return list.stream()
                .sorted(Comparator.comparing(CheatUser::getRiskScore).reversed())
                .collect(Collectors.toList());
    }

    public boolean ban(Long userId) {
        RMapCache<Long, CheatUser> cache = redisson.getMapCache(SUSPICIOUS_KEY);
        CheatUser user = cache.get(userId);
        if (user == null) {
            return false;
        }
        com.flashsale.flashsale.entity.User u = userMapper.selectById(userId);
        if (u != null && "ADMIN".equals(u.getRole())) {
            throw new com.flashsale.common.exception.BusinessException(
                    com.flashsale.common.constant.ResultCode.VALIDATE_FAILED, "不能封禁管理账号");
        }
        blacklistService.block(userId, user.getIp(), "AI防作弊监控手动封禁", user.getRiskScore());
        cache.remove(userId);
        return true;
    }

    public List<CheatBlacklist> listBlacklist() {
        List<CheatBlacklist> list = blacklistMapper.selectList(new LambdaQueryWrapper<CheatBlacklist>()
                .orderByDesc(CheatBlacklist::getCreateTime));
        Map<Long, String> usernames = new HashMap<>();
        List<Long> userIds = list.stream()
                .map(CheatBlacklist::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (!userIds.isEmpty()) {
            userMapper.selectBatchIds(userIds)
                    .forEach(u -> usernames.put(u.getId(), u.getUsername()));
        }
        list.forEach(cb -> cb.setUsername(usernames.get(cb.getUserId())));
        return list;
    }
}