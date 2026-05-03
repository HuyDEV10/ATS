package com.dacn.ATS.exception;

import com.dacn.ATS.common.enums.ResultCodeEnum;

public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(ResultCodeEnum errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ResultCodeEnum errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public int getCode() {
        return code;
    }
}
