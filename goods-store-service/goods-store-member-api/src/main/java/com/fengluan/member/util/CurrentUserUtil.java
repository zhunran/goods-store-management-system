package com.fengluan.member.util;

import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 当前登录用户（经网关 JwtAuthFilter 写入 X-User-Id 请求头）
 * 契约方法签名不含 header 参数，故从 RequestContextHolder 读取。
 */
public final class CurrentUserUtil {

    private CurrentUserUtil() {
    }

    /** 读取当前用户 id（未登录抛 401） */
    public static Long currentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        HttpServletRequest request = attrs.getRequest();
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return Long.valueOf(userId);
    }

    /** 越权校验：被操作 id 必须等于当前用户 id */
    public static void assertOwned(Long targetId) {
        Long current = currentUserId();
        if (current == null || !current.equals(targetId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}