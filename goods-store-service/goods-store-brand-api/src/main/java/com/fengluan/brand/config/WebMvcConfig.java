package com.fengluan.brand.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 静态资源映射：将 /static/upload/** 映射到本地磁盘上传目录
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${upload.local.path:./static/upload}")
    private String basePath;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(basePath).toAbsolutePath().normalize().toString();
        registry.addResourceHandler("/static/upload/**")
                .addResourceLocations("file:" + absolute + "/");
    }
}