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

    /** 验证码会话 ID（仅 C 端登录必填，由 BFF 生成并校验，auth-api 不感知） */
    private String captchaId;

    /** 验证码输入（仅 C 端登录必填，由 BFF 校验后不再透传） */
    private String captchaCode;
}