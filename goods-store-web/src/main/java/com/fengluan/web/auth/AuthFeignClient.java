package com.fengluan.web.auth;

import com.fengluan.spi.auth.AuthApi;
import org.springframework.cloud.openfeign.FeignClient;

/**
 * web（BFF 聚合层）访问 auth-api 的 Feign 客户端，复用 spi 纯契约 AuthApi。
 */
@FeignClient(name = "goods-store-auth-api", path = "/auth/api")
public interface AuthFeignClient extends AuthApi {
}