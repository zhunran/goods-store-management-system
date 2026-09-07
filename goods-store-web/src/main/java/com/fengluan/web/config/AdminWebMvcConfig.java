package com.fengluan.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 管理端路由拦截：注册 AdminRoleInterceptor 做「角色 + 权限码」双重校验。
 * - 角色管理（role:manage）仅 ADMIN 可见
 * - 秒杀/会员为 ADMIN 专属，OPERATOR 无相应权限码
 * - 商品/品牌/订单两角色共有，仅校验管理员角色
 */
@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminRoleInterceptor("role:manage"))
                .addPathPatterns("/app/api/role", "/app/api/role/**");

        registry.addInterceptor(new AdminRoleInterceptor("seckill:list"))
                .addPathPatterns("/app/api/seckill/admin", "/app/api/seckill/admin/**");

        registry.addInterceptor(new AdminRoleInterceptor("member:list"))
                .addPathPatterns("/app/api/member/admin", "/app/api/member/admin/**");

        registry.addInterceptor(new AdminRoleInterceptor(null))
                .addPathPatterns(
                        "/app/api/product/admin", "/app/api/product/admin/**",
                        "/app/api/brand/admin", "/app/api/brand/admin/**",
                        "/app/api/order/admin", "/app/api/order/admin/**");
    }
}