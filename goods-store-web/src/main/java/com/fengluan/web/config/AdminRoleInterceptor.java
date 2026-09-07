package com.fengluan.web.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.List;

/**
 * 管理端权限校验：先断言网关透传的 X-User-Roles 含任一管理员角色（ROLE_ADMIN/ROLE_OPERATOR），
 * 再按需断言 X-User-Permissions 含指定权限码。
 * 两角色规模下不引入网关 path+method 表，权限码在登录时随 JWT 下发并经网关透传。
 */
public class AdminRoleInterceptor implements HandlerInterceptor {

    private static final List<String> ADMIN_ROLES = List.of("ROLE_ADMIN", "ROLE_OPERATOR");

    /** 可空：为空仅要求管理员角色，非空则额外要求具备该权限码 */
    private final String requiredPermission;

    public AdminRoleInterceptor(String requiredPermission) {
        this.requiredPermission = requiredPermission;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String roles = request.getHeader("X-User-Roles");
        boolean isAdmin = roles != null && Arrays.stream(roles.split(","))
                .map(String::trim)
                .anyMatch(ADMIN_ROLES::contains);
        if (!isAdmin) {
            return forbidden(response);
        }

        if (requiredPermission != null) {
            String permissions = request.getHeader("X-User-Permissions");
            boolean hasPerm = permissions != null && Arrays.stream(permissions.split(","))
                    .map(String::trim)
                    .anyMatch(requiredPermission::equals);
            if (!hasPerm) {
                return forbidden(response);
            }
        }
        return true;
    }

    private boolean forbidden(HttpServletResponse response) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":403,\"message\":\"无权限访问\"}");
        return false;
    }
}