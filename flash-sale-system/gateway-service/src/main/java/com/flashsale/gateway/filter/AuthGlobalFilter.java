package com.flashsale.gateway.filter;

import com.flashsale.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    /** 无需登录即可访问的接口前缀（只读/登录注册） */
    private static final List<String> WHITELIST = List.of(
        "/api/auth/",
        "/api/product/list", "/api/product/detail",
        "/api/flash-sale/list", "/api/flash-sale/detail",
        "/api/inventory/stock/"
    );

    /** 仅 ADMIN 角色可访问的管理接口前缀 */
    private static final List<String> ADMIN_PATHS = List.of(
        "/api/product/create",
        "/api/product/update",
        "/api/product/delete",
        "/api/flash-sale/create",
        "/api/flash-sale/admin/",
        "/api/inventory/warm-up",
        "/api/ai/"
    );

    /** 仅供服务间内部调用（Feign 直连，不经过网关），禁止外部直接访问 */
    private static final List<String> INTERNAL_ONLY_PATHS = List.of(
        "/api/inventory/decrease",
        "/api/inventory/release",
        "/api/order/flash/create"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        String method = exchange.getRequest().getMethodValue();

        if (INTERNAL_ONLY_PATHS.stream().anyMatch(path::startsWith)) {
            return writeJson(exchange, HttpStatus.FORBIDDEN, "{\"code\":403,\"message\":\"内部接口，禁止外部访问\"}");
        }

        // 管理接口：必须携带有效 token 且 role=ADMIN，否则返回 401/403
        if (ADMIN_PATHS.stream().anyMatch(path::startsWith)) {
            return adminCheck(exchange, chain);
        }

        // 商品/库存的只读 GET 接口公开访问
        boolean publicRead = (path.startsWith("/api/product/") || path.startsWith("/api/inventory/"))
                && "GET".equalsIgnoreCase(method);
        if (WHITELIST.stream().anyMatch(path::startsWith) || publicRead) {
            return chain.filter(exchange);
        }

        // 其余接口（秒杀/订单等）：必须登录，X-User-Id 由网关从 JWT 注入，不信任客户端
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
        try {
            String jwt = token.substring(7);
            Long userId = JwtUtil.getUserId(jwt);
            exchange = exchange.mutate()
                    .request(r -> r.header("X-User-Id", String.valueOf(userId)))
                    .build();
            return chain.filter(exchange);
        } catch (Exception e) {
            log.warn("token验证失败: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private Mono<Void> adminCheck(ServerWebExchange exchange, GatewayFilterChain chain) {
        String token = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, "{\"code\":401,\"message\":\"未授权\"}");
        }
        try {
            Claims claims = JwtUtil.parse(token.substring(7));
            String role = claims.get("role", String.class);
            if (!"ADMIN".equals(role)) {
                return writeJson(exchange, HttpStatus.FORBIDDEN, "{\"code\":403,\"message\":\"无权限\"}");
            }
            Long userId = claims.get("userId", Long.class);
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r.header("X-User-Id", String.valueOf(userId)))
                    .build();
            return chain.filter(mutated);
        } catch (Exception e) {
            log.warn("admin token验证失败: {}", e.getMessage());
            return writeJson(exchange, HttpStatus.UNAUTHORIZED, "{\"code\":401,\"message\":\"未授权\"}");
        }
    }

    private Mono<Void> writeJson(ServerWebExchange exchange, HttpStatus status, String body) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() { return -100; }
}