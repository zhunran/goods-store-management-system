package com.fengluan.common.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Getter
public class SnowflakeUtil {
    private final Snowflake snowflake;
    public SnowflakeUtil(@Value("${snowflake.worker-id:1}" )long workerId,
                         @Value("${snowflake.datacenter-id:1}")long datacenterId){
        this.snowflake= IdUtil.getSnowflake(workerId, datacenterId);
    }

    //生成数值型id（19位）
    public long nextId(){
        return snowflake.nextId();
    }
    //生成字符串型ID
    public  String nectIdStr(){
        return  snowflake.nextIdStr();
    }

}
