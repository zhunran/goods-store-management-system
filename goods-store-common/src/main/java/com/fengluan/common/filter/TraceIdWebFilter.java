package com.fengluan.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 链路追踪（Servlet 服务 web/api 用）：读上游 X-Trace-Id（无则生成），写入 MDC("TraceId")，
 * 响应回写头；供 Feign 透传与日志 %X{TraceId} 打印。
 */
@Component
public class TraceIdWebFilter extends OncePerRequestFilter {

    private static final String TRACE_ID = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = request.getHeader(TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put("TraceId", traceId);
        try {
            chain.doFilter(request, response);
            response.setHeader(TRACE_ID, traceId);
        } finally {
            MDC.remove("TraceId");
        }
    }
}