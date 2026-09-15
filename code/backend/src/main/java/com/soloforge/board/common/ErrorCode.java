package com.soloforge.board.common;

public enum ErrorCode {

    SUCCESS(0, 200, "ok"),
    VALIDATION_FAILED(1001, 400, "参数校验失败"),
    UNAUTHORIZED(1002, 401, "未登录或会话已失效"),
    ACCOUNT_DISABLED(1003, 401, "账号已停用，请联系管理员"),
    BAD_CREDENTIALS(1004, 400, "账号或密码错误"),
    USERNAME_EXISTS(1005, 400, "登录名已存在"),
    NOT_FOUND(1006, 404, "目标不存在"),
    OPERATION_NOT_ALLOWED(1007, 400, "不允许的操作"),
    FORBIDDEN(1008, 403, "无权限访问"),
    SERVER_ERROR(9999, 500, "服务端异常");

    private final int code;
    private final int httpStatus;
    private final String message;

    ErrorCode(int code, int httpStatus, String message) {
        this.code = code;
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }
}