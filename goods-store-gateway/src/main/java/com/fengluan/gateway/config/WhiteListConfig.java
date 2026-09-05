package com.fengluan.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.util.ArrayList;
import java.util.List;


//免鉴权路径白名单，prefix配置来源
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class WhiteListConfig {
    private final AntPathMatcher matcher=new AntPathMatcher();
    //默认白名单，登录/注册，品牌/商品 网关自身 健康检查
    private List<String> whitelist =new ArrayList<>(List.of(
            "/auth/api/login",
            "/auth/api/register",
            "/auth/api/admin/login",
            "/brand/api/**",
            "/good/api/**",
            "/actuator/**",
            // 静态图片资源放行：浏览器 <img>/el-image 请求不会携带 Authorization 头
            "/static/upload/**",
            // web（BFF）登录类端点放行，其余 /app/api/** 需鉴权
            "/app/api/auth/login",
            "/app/api/auth/register",
            "/app/api/auth/refresh",
            "/app/api/auth/admin/login",
            "/app/api/auth/admin/refresh",
            "/app/api/brand/list",
            "/app/api/product/list",
            "/app/api/product/tree",
            // 商品详情（C 端，* 仅匹配单层，不影响 /app/api/product/admin/** 需鉴权）
            "/app/api/product/*"
    ));
    public boolean isWhiteListed(String path)
    {
        return whitelist.stream().anyMatch(p->matcher.match(p,path));
    }
}
