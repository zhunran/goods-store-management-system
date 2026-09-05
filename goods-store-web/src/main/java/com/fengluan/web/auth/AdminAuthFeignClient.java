package com.fengluan.web.auth;

import com.fengluan.spi.auth.dto.LoginRequest;
import com.fengluan.spi.auth.dto.LoginResponse;
import com.fengluan.spi.auth.dto.RefreshRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 管理员认证 Feign 客户端（auth-api 的 /auth/api/admin/**，RBAC 登录/刷新）
 */
@FeignClient(name = "goods-store-auth-api", contextId = "adminAuthFeignClient", path = "/auth/api")
public interface AdminAuthFeignClient {

    @PostMapping("/admin/login")
    LoginResponse adminLogin(@RequestBody LoginRequest request);

    @PostMapping("/admin/refresh")
    LoginResponse adminRefresh(@RequestBody RefreshRequest request);
}
