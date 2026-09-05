package com.fengluan.seckill;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.fengluan.seckill.remote")
@MapperScan("com.fengluan.seckill.repository")
@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.fengluan")
public class SeckillApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(SeckillApiApplication.class, args);
    }
}
