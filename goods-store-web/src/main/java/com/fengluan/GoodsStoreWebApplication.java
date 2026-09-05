package com.fengluan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.fengluan")
@SpringBootApplication
public class GoodsStoreWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(GoodsStoreWebApplication.class, args);
    }

    /**
     * @LoadBalanced 使 RestTemplate 具备服务发现与客户端负载均衡能力，
     * URL 中的主机名部分会被解析为 Nacos 注册的服务名
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
