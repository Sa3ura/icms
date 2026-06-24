package com.icms.common;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final int code;

    public CustomException(String message) {
        super(message);
        this.code = 500;
    }

    public CustomException(int code, String message) {
        super(message);
        this.code = code;
    }
}
