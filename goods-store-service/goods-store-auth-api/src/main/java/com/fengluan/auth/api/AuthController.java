package com.fengluan.auth.api;

import com.fengluan.auth.service.AuthService;
import com.fengluan.spi.auth.AuthApi;
import com.fengluan.spi.auth.dto.ChangePwdRequest;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import com.fengluan.spi.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 会员认证控制器：兑现 spi 契约 AuthApi（register/login/refresh/logout/password）。
 */
@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;

    @Override
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @Override
    public Void register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return null;
    }

    @Override
    public LoginResponse refresh(@RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @Override
    public Void logout(@RequestHeader("Authorization") String authorization) {
        authService.logout(authorization);
        return null;
    }

    @Override
    @PutMapping("/password")
    public Void changePassword(@RequestHeader("Authorization") String authorization,
                               @RequestBody ChangePwdRequest request) {
        authService.changePassword(authorization, request);
        return null;
    }
}