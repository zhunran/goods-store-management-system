package com.fengluan.trade.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * trade 服务内 Feign 调用透传入站 X-User-Id，使下游 member-api 能做越权校验
 * （member getProfile 校验当前用户 == 目标 id）。
 */
public class TradeFeignHeaderInterceptor implements RequestInterceptor {

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