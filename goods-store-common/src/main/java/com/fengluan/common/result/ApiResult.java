package com.fengluan.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiResult<T> {
    private Integer code;
    private boolean success;
    private String message;
    private T data;

    // 1. 最常用：无参，快速返回成功
    public static <T> ApiResult<T> success() {
        return new ApiResult<>(200, true, null, null);
    }

    // 2. 常用：只返回数据（message 为 null）
    public static <T> ApiResult<T> success(T data) {
        return new ApiResult<>(200, true,null, data);
    }

    // 3. 较常用：只返回 message（data 为 null）
    public static <T> ApiResult<T> success(String message) {
        return new ApiResult<>(200,true ,message, null);
    }

    // 4. 最完整：同时返回 message 和 data
    public static <T> ApiResult<T> success(String message, T data) {
        return new ApiResult<>(200,true, message, data);
    }

    public static <T> ApiResult<T> error(Integer code, String message) {
        ApiResult<T> apiResult = new ApiResult<>();
        apiResult.setCode(code);
        apiResult.setMessage(message);
        return apiResult;
    }

    public static <T> ApiResult<T> error(String message) {
        return error(500, message);
    }
}
