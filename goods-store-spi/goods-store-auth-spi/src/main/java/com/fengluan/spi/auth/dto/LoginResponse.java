package com.fengluan.spi.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 登录/刷新响应：双 Token + 用户信息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /** Access Token，默认 30 分钟 */
    private String accessToken;

    /** Refresh Token，默认 7 天 */
    private String refreshToken;

    /** Access Token 有效期（秒） */
    private Long expiresIn;

    /** 用户信息 */
    private UserInfo userInfo;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long userId;
        private String account;
        /** member / admin */
        private String type;
        /** 管理员角色编码（会员为空） */
        private List<String> roles;
        /** 管理员权限编码（会员为空） */
        private List<String> permissions;
    }
}