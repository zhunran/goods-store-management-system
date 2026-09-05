package com.fengluan.trade.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * trade 层 Feign 全局配置：透传 X-User-Id 请求头到下游 api 服务（member-api 越权校验用）。
 */
@Configuration
public class FeignConfig {

    @Bean
    public TradeFeignHeaderInterceptor tradeFeignHeaderInterceptor() {
        return new TradeFeignHeaderInterceptor();
    }
}