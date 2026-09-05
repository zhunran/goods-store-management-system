package com.fengluan.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 管理端角色校验：管理端路径需网关透传的 X-User-Roles 含 ROLE_ADMIN。
 * 网关 JwtAuthFilter 已校验 Token 并写入 X-User-Roles，此处仅做角色断言。
 */
public class AdminRoleInterceptor implements HandlerInterceptor {

    private static final String ADMIN_ROLE = "ROLE_ADMIN";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String roles = request.getHeader("X-User-Roles");
        boolean isAdmin = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(ADMIN_ROLE::equals);
        if (!isAdmin) {
            response.setStatus(HttpStatus.FORBIDDEN.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"无权限访问\"}");
            return false;
        }
        return true;
    }
}
