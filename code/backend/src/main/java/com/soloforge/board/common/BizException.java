package com.soloforge.board.common;

public class BizException extends RuntimeException {

    private final ErrorCode errorCode;
    private final transient Object payload;

    public BizException(ErrorCode errorCode) {
        this(errorCode, errorCode.getMessage(), null);
    }

    public BizException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public BizException(ErrorCode errorCode, String message, Object payload) {
        super(message);
        this.errorCode = errorCode;
        this.payload = payload;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Object getPayload() {
        return payload;
    }
}