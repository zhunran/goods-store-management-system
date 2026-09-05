package com.fengluan.common.feign;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

/**
 * 链路追踪：Feign 调用前从 MDC("TraceId") 取 traceId 透传到下游 X-Trace-Id 头。
 * api/web 都依赖 common 并被扫描（scanBasePackages="com.fengluan"），自动装配到 Feign 客户端。
 */
@Component
public class FeignTraceInterceptor implements RequestInterceptor {

    private static final String TRACE_ID = "X-Trace-Id";

    @Override
    public void apply(RequestTemplate template) {
        String traceId = MDC.get("TraceId");
        if (traceId != null && template.headers().get(TRACE_ID) == null) {
            template.header(TRACE_ID, traceId);
        }
    }
}