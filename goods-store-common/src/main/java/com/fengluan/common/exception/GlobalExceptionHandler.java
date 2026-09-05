package com.fengluan.common.exception;

import com.fengluan.common.result.ApiResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 业务异常必须以非 2xx 的 HTTP 状态返回：
     * 若返回 HTTP 200 + ApiResult.error body，Feign 调用方会走"成功"分支，
     * 把错误 body 反序列化成全 null 的 DTO（错误被吞），前端拿到 code=200 的空数据。
     * 这里按业务码映射 HTTP 状态，使下游 FeignErrorDecoder 能触发并还原 code/message。
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResult<?>> handleBusinessException(BusinessException e) {
        log.warn("业务异常：code={}, message={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(resolveHttpStatus(e.getCode()))
                .body(ApiResult.error(e.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResult<?>> handleValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("参数校验失败");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResult.error(ErrorCode.BAD_REQUEST.getCode(), msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResult<?>> handleException(Exception e) {
        log.error("系统异常：", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResult.error(ErrorCode.INTERNAL_ERROR.getCode(), "服务器繁忙，请稍后重试"));
    }

    /**
     * 业务码 → HTTP 状态码映射：
     * 三位数以内（&lt;1000）视为 HTTP 语义状态码，原样透传（配合网关与前端 401 刷新逻辑）；
     * 四位业务码（1xxx~6xxx）统一按 400 返回——注意 BRAND_xxx(5xxx)/AUTH_xxx(6xxx)
     * 等业务码若按数值透传会被误判为"服务器 5xx 错误"，掩盖真实业务原因。
     */
    private HttpStatus resolveHttpStatus(Integer code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        if (code < 1000) {
            HttpStatus status = HttpStatus.resolve(code);
            return status != null ? status : HttpStatus.BAD_REQUEST;
        }
        return HttpStatus.BAD_REQUEST;
    }
}
