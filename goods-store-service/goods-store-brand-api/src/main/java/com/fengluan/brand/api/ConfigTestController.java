package com.fengluan.brand.api;

import com.fengluan.common.util.SnowflakeUtil;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/brand/api/config")
@RequiredArgsConstructor
@RefreshScope//配置变更主动重建
public class ConfigTestController {
    @Value("${brand.test-flag:default}")
    private String testFlag;
    @Value("${snowflake.worker-id:0}")
    private long workerId;
    private final SnowflakeUtil snowflakeUtil;
    @GetMapping("/check")
    public Map<String,Object> check(){
        Map<String,Object> map=new LinkedHashMap<>();
        map.put("testFlag",testFlag);
        map.put("workId",workerId);
        map.put("snowflakeId",snowflakeUtil.nectIdStr());
        return map;
    }
}
