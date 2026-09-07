package com.fengluan.spi.auth;

import com.fengluan.spi.auth.dto.ChangePwdRequest;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import com.fengluan.spi.auth.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * 认证服务纯 HTTP 契约。
 * <p>Day3 起分层架构规范：spi 禁止携带 {@code @FeignClient}，只写路径注解 + DTO/VO。
 * 提供方 {@code AuthController implements AuthApi}；消费方（web）定义
 * {@code XxxFeignClient extends AuthApi} 并自行加 {@code @FeignClient}。</p>
 */
public interface AuthApi {

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest request);

    @PostMapping("/register")
    Void register(@Valid @RequestBody RegisterRequest request);

    @PostMapping("/refresh")
    LoginResponse refresh(@Valid @RequestBody RefreshRequest request);

    @PostMapping("/logout")
    Void logout(@RequestHeader("Authorization") String authorization);

    @PutMapping("/password")
    Void changePassword(@RequestHeader("Authorization") String authorization,
                        @Valid @RequestBody ChangePwdRequest request);
}