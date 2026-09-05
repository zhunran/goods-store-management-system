package com.fengluan.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final  Integer code;
    public BusinessException(ErrorCode code)
    {
        super(code.getMessage());
        this.code=code.getCode();
    }

    public BusinessException(String message,Integer code) {
        super(message);
        this.code=code;
    }
}
