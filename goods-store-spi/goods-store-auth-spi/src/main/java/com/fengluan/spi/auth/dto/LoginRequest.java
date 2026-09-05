package com.fengluan.spi.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 登录请求：会员用 account，管理员用 username。
 */
@Data
public class LoginRequest {

    /** 会员 account / 管理员 username */
    @NotBlank(message = "账号不能为空")
    private String account;

    @NotBlank(message = "密码不能为空")
    private String password;

    /** 登录类型：member / admin */
    private String loginType;
}