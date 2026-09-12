package com.flashsale.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Component
public class BlacklistGlobalFilter implements GlobalFilter, Ordered {

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth/",
        "/api/product/list", "/api/product/detail",
        "/api/flash-sale/list", "/api/flash-sale/detail",
        "/api/inventory/stock/"
    );

    private final RedissonClient redisson;

    public BlacklistGlobalFilter(RedissonClient redisson) {
        this.redisson = redisson;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethodValue();
        // 公开只读接口不参与黑名单拦截，避免拉黑用户连浏览都被拒
        boolean publicRead = (path.startsWith("/api/product/") || path.startsWith("/api/inventory/"))
                && "GET".equalsIgnoreCase(method);
        if (PUBLIC_PATHS.stream().anyMatch(path::startsWith) || publicRead) {
            return chain.filter(exchange);
        }

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        String ip = exchange.getRequest().getRemoteAddress() != null
                ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() : "unknown";

        // Redis黑名单校验（读取异常视为未拉黑，避免因脏数据导致 500）
        try {
            if (userId != null && redisson.getBucket("cheat:block:u:" + userId).get() != null) {
                log.warn("黑名单拦截: userId={}, ip={}", userId, ip);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            if (ip != null && !"unknown".equals(ip) && redisson.getBucket("cheat:block:ip:" + ip).get() != null) {
                log.warn("黑名单IP拦截: userId={}, ip={}", userId, ip);
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        } catch (Exception e) {
            log.warn("黑名单校验异常，放行: {}", e.getMessage());
        }

        return chain.filter(exchange);
    }

    /** 在 AuthGlobalFilter(-100) 之后执行 */
    @Override
    public int getOrder() { return -50; }
}