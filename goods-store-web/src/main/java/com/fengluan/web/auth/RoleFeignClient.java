package com.fengluan.web.auth;

import com.fengluan.spi.auth.RoleApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * web 层角色管理 Feign 客户端：extends auth 纯契约（auth-api 的 /auth/api/role/**、/permission/**）。
 */
@FeignClient(name = "goods-store-auth-api", contextId = "roleFeignClient", path = "/auth/api")
public interface RoleFeignClient extends RoleApi {
}