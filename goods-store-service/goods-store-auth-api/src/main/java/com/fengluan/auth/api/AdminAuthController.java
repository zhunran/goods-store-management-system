package com.fengluan.auth.api;

import com.fengluan.auth.service.AdminAuthService;
import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员认证控制器（RBAC），仅处理管理员登录。
 */
@RestController
@RequestMapping("/auth/api")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @PostMapping("/admin/login")
    public LoginResponse adminLogin(@RequestBody LoginRequest request) {
        return adminAuthService.login(request);
    }

    @PostMapping("/admin/refresh")
    public LoginResponse adminRefresh(@RequestBody RefreshRequest request) {
        return adminAuthService.refresh(request);
    }
}