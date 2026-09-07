package com.fengluan.common;

import com.fengluan.common.config.JacksonConfig;
import com.fengluan.common.mq.OrderCancelMessage;
import com.fengluan.common.mq.OrderCreateMessage;
import com.fengluan.common.mq.SeckillOrderMessage;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Day 1 公共模块单测：验证 Jackson 3 全局序列化规则与 3 个 MQ 消息体往返。
 */
class CommonDay1Test {

    /** 应用项目自定义序列化规则后的 JsonMapper（等价于 Spring 容器内的 mapper） */
    private JsonMapper configuredMapper() {
        JsonMapper.Builder builder = JsonMapper.builder();
        new JacksonConfig().jacksonCustomizer().customize(builder);
        return builder.build();
    }

    @Test
    void jacksonShouldSerializeLongAsString() throws Exception {
        JsonMapper mapper = configuredMapper();
        OrderCreateMessage msg = OrderCreateMessage.builder()
                .orderNo("1940000000000000001")
                .memberId(1234567890123456789L)
                .items(List.of(OrderCreateMessage.Item.builder()
                        .goodId(1L).skuId(2L).count(3).build()))
                .build();
        String json = mapper.writeValueAsString(msg);
        assertTrue(json.contains("\"memberId\":\"1234567890123456789\""),
                "Long 必须序列化为字符串，实际输出：" + json);
        OrderCreateMessage back = mapper.readValue(json, OrderCreateMessage.class);
        assertEquals(msg.getMemberId(), back.getMemberId());
    }

    @Test
    void localDateTimeShouldUseUnifiedPattern() throws Exception {
        JsonMapper mapper = configuredMapper();
        String json = mapper.writeValueAsString(
                Map.of("t", LocalDateTime.of(2026, 8, 29, 10, 30, 0)));
        assertTrue(json.contains("2026-08-29 10:30:00"), "实际输出：" + json);
    }

    @Test
    void mqMessagesShouldRoundTrip() throws Exception {
        JsonMapper mapper = configuredMapper();

        OrderCreateMessage create = OrderCreateMessage.builder()
                .orderNo("1940000000000000001")
                .memberId(123L)
                .items(List.of(OrderCreateMessage.Item.builder()
                        .goodId(1L).skuId(2L).count(3).build()))
                .build();
        OrderCreateMessage createBack = mapper.readValue(
                mapper.writeValueAsString(create), OrderCreateMessage.class);
        assertEquals(create, createBack);

        SeckillOrderMessage seckill = SeckillOrderMessage.builder()
                .seckillId(1L).seckillGoodId(2L).goodId(3L).memberId(4L)
                .seckillPrice(new BigDecimal("1000")).orderNo("1940000000000000002")
                .build();
        SeckillOrderMessage seckillBack = mapper.readValue(
                mapper.writeValueAsString(seckill), SeckillOrderMessage.class);
        assertEquals(seckill, seckillBack);

        OrderCancelMessage cancel = OrderCancelMessage.builder()
                .orderNo("1940000000000000003")
                .reason("TIMEOUT")
                .items(List.of(OrderCreateMessage.Item.builder()
                        .goodId(1L).skuId(2L).count(3).build()))
                .build();
        OrderCancelMessage cancelBack = mapper.readValue(
                mapper.writeValueAsString(cancel), OrderCancelMessage.class);
        assertEquals(cancel, cancelBack);
    }
}