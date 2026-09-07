package com.fengluan.web.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录图形验证码返回体。
 * <p>image 为 Hutool 生成的 base64 data URI（data:image/png;base64,...），
 * 前端可直接赋给 img.src，无需再拼接前缀。</p>
 */
@Data
@AllArgsConstructor
public class CaptchaVO {

    /** 验证码会话 ID，登录时原样回传用于 Redis 比对 */
    private String captchaId;

    /** 验证码图片（base64 data URI） */
    private String image;
}