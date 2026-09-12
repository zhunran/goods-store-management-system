package com.fengluan.seckill.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

/** 秒杀关键常量与 Lua 脚本（抢购原子性 Day13 使用） */
@Configuration
public class RedisConfig {

    public static final String STOCK_KEY_PREFIX = "seckill:stock:";
    public static final String ORDER_KEY_PREFIX = "seckill:order:";
    /** 补偿幂等标记键前缀（建单失败/超时关单补偿用，Day 3 使用） */
    public static final String COMP_KEY_PREFIX = "seckill:comp:";

    /** 抢购 Lua：校验并扣减库存 + 记每人一单（带 TTL），原子返回 1/0/-1/-2 */
    public static final String BUY_LUA = """
        local stock = redis.call('get', KEYS[1])
        if not stock then return -1 end
        local left = tonumber(stock)
        if left <= 0 then return 0 end
        local done = redis.call('setnx', KEYS[2], 1)
        if done == 0 then return -2 end
        redis.call('expire', KEYS[2], tonumber(ARGV[1]))
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

    /** 回补 Lua：库存键存在才 INCR（防凭空造库存），并清防重键 */
    public static final String RESTORE_LUA = """
        if redis.call('exists', KEYS[1]) == 1 then
          redis.call('incr', KEYS[1])
          redis.call('del', KEYS[2])
          return 1
        end
        return 0
        """;

    /** 回补脚本 Bean：KEYS[1]=库存键，KEYS[2]=防重键 */
    @Bean
    public DefaultRedisScript<Long> restoreScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptText(RESTORE_LUA);
        script.setResultType(Long.class);
        return script;
    }
}