package com.fengluan.web.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * BFF 层 Feign 调用透传网关注入的 X-User-Id，使下游 api（如 member-api）能做越权校验。
 */
public class FeignHeaderPropagateInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return;
        }
        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader("X-User-Id");
        if (userId != null && !userId.isBlank()) {
            template.header("X-User-Id", userId);
        }
    }
}