package com.fengluan.api;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RequestMapping("/web/api")
@RestController
@RequiredArgsConstructor
public class WebTestController {

    private static final String BRAND_API = "http://goods-store-brand-api";

    private final RestTemplate restTemplate;

    /**
     * 通过 Nacos 服务发现调用 brand-api 的分页接口
     * GET /web/api/brand/page?pageNum=1&pageSize=10
     */
    @GetMapping("/brand/page")
    @SuppressWarnings("unchecked")
    public Map<String, Object> brandPage(@RequestParam(defaultValue = "1") long pageNum,
                                         @RequestParam(defaultValue = "10") long pageSize) {
        String url = BRAND_API + "/brand/api/page?pageNum=" + pageNum + "&pageSize=" + pageSize;
        return restTemplate.getForObject(url, Map.class);
    }
}
