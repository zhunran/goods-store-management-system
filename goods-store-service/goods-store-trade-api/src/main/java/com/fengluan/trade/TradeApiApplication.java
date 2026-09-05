package com.fengluan.trade;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
@MapperScan("com.fengluan.trade.repository")
@SpringBootApplication(scanBasePackages = "com.fengluan")
@EnableFeignClients(basePackages = "com.fengluan.trade.remote")
public class TradeApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeApiApplication.class, args);
    }
}
