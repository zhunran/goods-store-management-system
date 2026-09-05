package com.fengluan.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 管理端 /admin/** 路由拦截：注册 AdminRoleInterceptor 做角色校验。
 */
@Configuration
public class AdminWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AdminRoleInterceptor())
                .addPathPatterns("/app/api/**/admin", "/app/api/**/admin/**")
                .excludePathPatterns("/app/api/auth/admin/login", "/app/api/auth/admin/refresh");
    }
}
