package com.fengluan.web.auth;

import com.fengluan.spi.auth.dto.ChangePwdRequest;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import com.fengluan.spi.auth.dto.RegisterRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


/**
 * web 认证聚合服务：代理转发给 auth-api，前端不直连 /auth/api/**。
 */
@Service
@RequiredArgsConstructor
public class WebAuthService {

    private final AuthFeignClient authFeignClient;
    private final AdminAuthFeignClient adminAuthFeignClient;

    public LoginResponse login(LoginRequest req) {
        return authFeignClient.login(req);
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