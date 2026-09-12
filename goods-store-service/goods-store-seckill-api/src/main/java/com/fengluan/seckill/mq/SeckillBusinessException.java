package com.fengluan.seckill.mq;

/**
 * 秒杀建单业务失败（售罄/商品无效/会员无效/秒杀商品不存在）：
 * 属确定性失败，重试无意义 —— 外层补偿 Redis 后 ack。
 * 区别于 DB/网络等临时故障（走 nack 重试）。
 */
public class SeckillBusinessException extends RuntimeException {

    public SeckillBusinessException(String message) {
        super(message);
    }
}
