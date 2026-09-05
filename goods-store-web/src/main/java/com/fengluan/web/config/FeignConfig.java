package com.fengluan.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * web 层 Feign 全局配置：透传 X-User-Id 请求头到下游 api 服务。
 */
@Configuration
public class FeignConfig {

    @Bean
    public FeignHeaderPropagateInterceptor feignHeaderPropagateInterceptor() {
        return new FeignHeaderPropagateInterceptor();
    }
}