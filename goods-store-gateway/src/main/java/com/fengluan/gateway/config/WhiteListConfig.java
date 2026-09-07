package com.fengluan.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
    //任意方法均放行：登录/注册/刷新/验证码 + 静态图片资源
    private List<String> whitelist =new ArrayList<>(List.of(
            "/auth/api/login",
            "/auth/api/register",
            "/auth/api/admin/login",
            // 静态图片资源放行：浏览器 <img>/el-image 请求不会携带 Authorization 头
            "/static/upload/**",
            // web（BFF）登录类端点放行，其余 /app/api/** 需鉴权
            "/app/api/auth/login",
            "/app/api/auth/register",
            "/app/api/auth/refresh",
            "/app/api/auth/admin/login",
            "/app/api/auth/admin/refresh",
            "/app/api/auth/captcha"
    ));

    //仅 GET 放行：C 端公开只读查询（BFF 聚合），写接口一律需鉴权
    private List<String> readWhitelist =new ArrayList<>(List.of(
            "/app/api/brand/list",
            "/app/api/product/list",
            "/app/api/product/tree",
            // 商品详情；单层匹配不会命中 /app/api/product/admin（该路径无 GET 定义）
            "/app/api/product/*"
    ));

    public boolean isWhiteListed(String path, HttpMethod method)
    {
        if (whitelist.stream().anyMatch(p->matcher.match(p,path))) {
            return true;
        }
        return method == HttpMethod.GET
                && readWhitelist.stream().anyMatch(p->matcher.match(p,path));
    }
}
