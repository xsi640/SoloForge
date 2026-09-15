package com.soloforge.board.common;

import java.util.List;

public class R<T> {

    private int code;
    private String message;
    private T data;

    public R() {
    }

    public R(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> R<T> ok(T data) {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), data);
    }

    public static R<Void> ok() {
        return new R<>(ErrorCode.SUCCESS.getCode(), ErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode) {
        return new R<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message) {
        return new R<>(errorCode.getCode(), message, null);
    }

    public static <T> R<T> fail(ErrorCode errorCode, String message, T data) {
        return new R<>(errorCode.getCode(), message, data);
    }

    public static R<List<FieldErrorItem>> fieldErrors(List<FieldErrorItem> errors) {
        String message = errors.isEmpty()
                ? ErrorCode.VALIDATION_FAILED.getMessage()
                : errors.get(0).getReason();
        return new R<>(ErrorCode.VALIDATION_FAILED.getCode(), message, errors);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}