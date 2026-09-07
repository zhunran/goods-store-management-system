package com.fengluan.web.auth;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.LineCaptcha;
import cn.hutool.core.util.IdUtil;
import com.fengluan.common.exception.BusinessException;
import com.fengluan.common.exception.ErrorCode;
import com.fengluan.spi.auth.dto.ChangePwdRequest;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import com.fengluan.spi.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;


/**
 * web 认证聚合服务：代理转发给 auth-api，前端不直连 /auth/api/**。
 */
@Service
@RequiredArgsConstructor
public class WebAuthService {

    private final AuthFeignClient authFeignClient;
    private final AdminAuthFeignClient adminAuthFeignClient;
    private final StringRedisTemplate redisTemplate;

    /** 验证码 Redis key 前缀 */
    private static final String CAPTCHA_KEY_PREFIX = "captcha:";
    /** 验证码有效期：5 分钟，一次性（校验后立即删除） */
    private static final Duration CAPTCHA_TTL = Duration.ofMinutes(5);

    public CaptchaVO captcha() {
        LineCaptcha captcha = CaptchaUtil.createLineCaptcha(120, 40, 4, 60);
        String uuid = IdUtil.simpleUUID();
        redisTemplate.opsForValue().set(CAPTCHA_KEY_PREFIX + uuid, captcha.getCode(), CAPTCHA_TTL);
        return new CaptchaVO(uuid, captcha.getImageBase64Data());
    }

    public LoginResponse login(LoginRequest req) {
        verifyCaptcha(req);
        return authFeignClient.login(req);
    }

    /** 校验图形验证码（仅 C 端登录）：比对成功与否都删除，保证一次性；验证码只在 BFF 消费，不透传 auth-api。 */
    private void verifyCaptcha(LoginRequest req) {
        if (!StringUtils.hasText(req.getCaptchaId()) || !StringUtils.hasText(req.getCaptchaCode())) {
            throw new BusinessException(ErrorCode.CAPTCHA_REQUIRED);
        }
        String key = CAPTCHA_KEY_PREFIX + req.getCaptchaId();
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
        // 一次性：无论比对结果如何，先删除再判定
        redisTemplate.delete(key);
        if (!stored.equalsIgnoreCase(req.getCaptchaCode().trim())) {
            throw new BusinessException(ErrorCode.CAPTCHA_ERROR);
        }
    }

    public LoginResponse adminLogin(LoginRequest req) {
        return adminAuthFeignClient.adminLogin(req);
    }

    public LoginResponse adminRefresh(RefreshRequest req) {
        return adminAuthFeignClient.adminRefresh(req);
    }

    public void register(RegisterRequest req) {
        authFeignClient.register(req);
    }

    public LoginResponse refresh(RefreshRequest req) {
        return authFeignClient.refresh(req);
    }

    public void logout(String authorization) {
        authFeignClient.logout(authorization);
    }

    public void changePassword(String authorization, ChangePwdRequest req) {
        authFeignClient.changePassword(authorization, req);
    }
}