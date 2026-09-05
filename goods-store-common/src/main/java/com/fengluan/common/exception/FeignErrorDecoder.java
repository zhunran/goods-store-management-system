package com.fengluan.common.exception;

import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Feign 异常解码（Day 14）：把下游服务的 ApiResult 响应体解码为 BusinessException，
 * 保留原始 code/message（而非吞成 500）。JSON 用 Jackson 3（tools.jackson）。
 */
@Slf4j
@Component
public class FeignErrorDecoder implements ErrorDecoder {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        String body = null;
        if (response.body() != null) {
            try {
                body = new String(response.body().asInputStream().readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException ignored) {
                // 读到空则走兜底
            }
        }
        // 解码 ApiResult{code,message}
        if (body != null && !body.isBlank()) {
            try {
                JsonNode node = mapper.readTree(body);
                JsonNode codeNode = node.get("code");
                JsonNode msgNode = node.get("message");
                if (codeNode != null && codeNode.isIntegralNumber()) {
                    int code = codeNode.asInt();
                    String message = msgNode != null ? msgNode.asText() : "服务调用失败";
                    return new BusinessException(message, code);
                }
            } catch (Exception e) {
                log.warn("Feign 错误响应体解析失败，methodKey={} body={}", methodKey, body, e);
            }
        }
        // 兜底：响应体不是 ApiResult（如 404/502 默认错误体），按 HTTP 状态透传，
        // 保证调用方能拿到非 2xx 的真实原因，而不是笼统的"请求参数错误"
        int status = response.status();
        return new BusinessException("下游服务调用失败(HTTP " + status + ")，请检查目标服务是否已启动最新代码", status);
    }
}