package com.fengluan.seckill.util;

import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
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
        Long id = currentUserIdOrNull();
        if (id == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return id;
    }

    /** 读取当前用户 id（未登录返回 null 不抛异常；用于列表等可选登录场景） */
    public static Long currentUserIdOrNull() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            return null;
        }
        String userId = attrs.getRequest().getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}