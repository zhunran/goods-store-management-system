package com.fengluan.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** 秒杀关键常量与 Lua 脚本（抢购原子性 Day13 使用） */
@Configuration
public class RedisConfig {

    public static final String STOCK_KEY_PREFIX = "seckill:stock:";
    public static final String ORDER_KEY_PREFIX = "seckill:order:";

    /** 抢购 Lua：校验并扣减库存 + 记每人一单，原子返回 1/0 */
    public static final String BUY_LUA = """
        local stock = redis.call('get', KEYS[1])
        if not stock then return -1 end
        local left = tonumber(stock)
        if left <= 0 then return 0 end
        local done = redis.call('setnx', KEYS[2], 1)
        if done == 0 then return -2 end
        redis.call('decrby', KEYS[1], 1)
        return 1
        """;

    /** 抢购 Lua 脚本 Bean：KEYS[1]=库存键 seckill:stock:{sgId}，KEYS[2]=防重键 seckill:order:{memberId}:{sgId} */
    @Bean
    public DefaultRedisScript<Long> seckillScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(BUY_LUA);
        script.setResultType(Long.class);
        return script;
    }
}