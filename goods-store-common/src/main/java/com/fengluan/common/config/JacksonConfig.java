package com.fengluan.common.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.ext.javatime.deser.LocalDateTimeDeserializer;
import tools.jackson.databind.ext.javatime.ser.LocalDateTimeSerializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Jackson 3 全局序列化配置（对应 Boot 4：JSON 库为 Jackson 3，包名 tools.jackson）。
 * - Long / long 统一序列化为字符串：防前端 JS（2^53 精度上限）读雪花 ID 丢精度
 * - LocalDateTime 统一为 yyyy-MM-dd HH:mm:ss
 *
 * 说明：
 * - Boot 4 已将 WRITE_DATES_AS_TIMESTAMPS 默认改为 false（LocalDateTime 默认 ISO-8601 字符串），
 *   这里仅再把格式统一成项目约定的完整时间。
 * - Jackson 3 中工具包由 jackson-datatype-jsr310 合并进 jackson-databind，位于
 *   tools.jackson.databind.ext.javatime；类型级的序列化/反序列化通过 SimpleModule 注册。
 * - Boot 4 将 JsonMapperBuilderCustomizer 从 spring-boot.autoconfigure.jackson 迁至
 *   spring-boot-jackson 模块的 org.springframework.boot.jackson.autoconfigure 包。
 */
@Configuration
public class JacksonConfig {
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public JsonMapperBuilderCustomizer jacksonCustomizer() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        return builder -> {
            SimpleModule module = new SimpleModule("GoodsStoreJacksonModule");
            module.addSerializer(Long.class, ToStringSerializer.instance);
            module.addSerializer(Long.TYPE, ToStringSerializer.instance);
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(formatter));
            module.addDeserializer(LocalDateTime.class, new LocalDateTimeDeserializer(formatter));
            builder.addModule(module);
        };
    }
}