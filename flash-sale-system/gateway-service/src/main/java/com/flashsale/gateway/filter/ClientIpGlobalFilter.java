package com.flashsale.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

/**
 * 客户端真实 IP 透传过滤器。
 *
 * 下游 flash-sale-service 的防作弊模块（FeatureCollector / BlacklistService）通过
 * 请求头 X-Forwarded-For 取客户端 IP。若网关不写入该头，下游只能拿到网关自身地址，
 * 导致“同 IP 多账号”维度与 IP 黑名单失效。这里把直连对端地址写入该头后转发。
 *
 * 说明：若请求已带 X-Forwarded-For，按代理链惯例在其后追加，不覆盖已有值。
 */
@Component
public class ClientIpGlobalFilter implements GlobalFilter, Ordered {

    private static final String X_FORWARDED_FOR = "X-Forwarded-For";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        InetSocketAddress remote = request.getRemoteAddress();
        if (remote == null || remote.getAddress() == null) {
            return chain.filter(exchange);
        }

        String clientIp = remote.getAddress().getHostAddress();
        String existing = request.getHeaders().getFirst(X_FORWARDED_FOR);
        String forwarded = (existing == null || existing.isBlank()) ? clientIp : existing + ", " + clientIp;

        ServerWebExchange mutated = exchange.mutate()
                .request(r -> r.header(X_FORWARDED_FOR, forwarded))
                .build();
        return chain.filter(mutated);
    }

    /** 最先执行，保证后续转发（AuthGlobalFilter -100 / BlacklistGlobalFilter -50 之后）都携带真实 IP */
    @Override
    public int getOrder() { return -200; }
}
