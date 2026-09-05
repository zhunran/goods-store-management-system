package com.fengluan.web.auth;

import com.fengluan.common.result.ApiResult;
import com.fengluan.spi.auth.dto.ChangePwdRequest;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import com.fengluan.spi.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 前端认证唯一入口（/app/api/auth/**），聚合代理到 web -> auth-api。
 */
@RestController
@RequestMapping("/app/api/auth")
@RequiredArgsConstructor
public class WebAuthController {

    private final WebAuthService webAuthService;

    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        return ApiResult.success(webAuthService.login(request));
    }

    @PostMapping("/admin/login")
    public ApiResult<LoginResponse> adminLogin(@RequestBody LoginRequest request) {
        return ApiResult.success(webAuthService.adminLogin(request));
    }

    @PostMapping("/admin/refresh")
    public ApiResult<LoginResponse> adminRefresh(@RequestBody RefreshRequest request) {
        return ApiResult.success(webAuthService.adminRefresh(request));
    }

    @PostMapping("/register")
    public ApiResult<Void> register(@RequestBody RegisterRequest request) {
        webAuthService.register(request);
        return ApiResult.success();
    }

    @PostMapping("/refresh")
    public ApiResult<LoginResponse> refresh(@RequestBody RefreshRequest request) {
        return ApiResult.success(webAuthService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResult<Void> logout(@RequestHeader("Authorization") String authorization) {
        webAuthService.logout(authorization);
        return ApiResult.success();
    }

    @PutMapping("/password")
    public ApiResult<Void> changePassword(@RequestHeader("Authorization") String authorization,
                               @RequestBody ChangePwdRequest request) {
        webAuthService.changePassword(authorization, request);
        return ApiResult.success();
    }
}