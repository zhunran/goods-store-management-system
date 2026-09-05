package com.fengluan.gateway.filter;

import org.springframework.core.Ordered;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.annotation.Order;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * 链路追踪：为入站请求生成/透传 X-Trace-Id（WebFlux GlobalFilter），并写回响应头。
 * 高优先级先于 JwtAuthFilter 执行，保证鉴权前已生成 traceId。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter implements GlobalFilter {

    private static final String TRACE_ID = "X-Trace-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest req = exchange.getRequest();
        String traceId = req.getHeaders().getFirst(TRACE_ID);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        exchange.getResponse().getHeaders().set(TRACE_ID, traceId);
        ServerHttpRequest mutated = req.mutate().header(TRACE_ID, traceId).build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }
}