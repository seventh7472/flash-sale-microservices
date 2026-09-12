package com.flashsale.flashsale.filter;

import com.flashsale.flashsale.ai.QpsCollector;
import lombok.RequiredArgsConstructor;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

@RequiredArgsConstructor
public class QpsFilter implements Filter {

    private final QpsCollector qpsCollector;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpReq = (HttpServletRequest) request;
            String uri = httpReq.getRequestURI();
            // 只统计秒杀和AI接口
            if (uri.startsWith("/api/flash-sale/") || uri.startsWith("/api/ai/")) {
                qpsCollector.recordRequest(uri, parseActivityId(uri));
            }
        }
        chain.doFilter(request, response);
    }

    private Long parseActivityId(String uri) {
        String prefix = "/api/flash-sale/seckill/";
        if (!uri.startsWith(prefix)) {
            return null;
        }
        String id = uri.substring(prefix.length());
        if (id.isEmpty() || id.contains("/")) {
            return null;
        }
        try {
            return Long.valueOf(id);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
