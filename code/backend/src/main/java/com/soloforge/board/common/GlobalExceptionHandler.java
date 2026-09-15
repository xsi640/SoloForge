package com.soloforge.board.common;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<R<Object>> handleBiz(BizException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(R.fail(errorCode, ex.getMessage(), ex.getPayload()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<R<List<FieldErrorItem>>> handleBodyValidation(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(R.fieldErrors(toFieldErrors(ex.getBindingResult().getFieldErrors())));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<R<List<FieldErrorItem>>> handleBind(BindException ex) {
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(R.fieldErrors(toFieldErrors(ex.getBindingResult().getFieldErrors())));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<R<List<FieldErrorItem>>> handleMissingParameter(MissingServletRequestParameterException ex) {
        List<FieldErrorItem> errors = new ArrayList<>();
        errors.add(new FieldErrorItem(ex.getParameterName(), "该参数必填"));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(R.fieldErrors(errors));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<R<List<FieldErrorItem>>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        List<FieldErrorItem> errors = new ArrayList<>();
        errors.add(new FieldErrorItem(ex.getName(), "参数格式不正确"));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(R.fieldErrors(errors));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<R<List<FieldErrorItem>>> handleUnreadable(HttpMessageNotReadableException ex) {
        List<FieldErrorItem> errors = new ArrayList<>();
        errors.add(new FieldErrorItem("body", "请求体格式不正确"));
        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.getHttpStatus())
                .body(R.fieldErrors(errors));
    }

    @ExceptionHandler({ NoResourceFoundException.class, NoHandlerFoundException.class })
    public ResponseEntity<R<Object>> handleNoHandler(Exception ex) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus())
                .body(R.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<R<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(ErrorCode.NOT_FOUND.getHttpStatus())
                .body(R.fail(ErrorCode.NOT_FOUND));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<R<Object>> handleUnexpected(Exception ex) {
        log.error("未处理的服务端异常", ex);
        return ResponseEntity.status(ErrorCode.SERVER_ERROR.getHttpStatus())
                .body(R.fail(ErrorCode.SERVER_ERROR));
    }

    private List<FieldErrorItem> toFieldErrors(List<FieldError> fieldErrors) {
        List<FieldErrorItem> errors = new ArrayList<>();
        for (FieldError fieldError : fieldErrors) {
            errors.add(new FieldErrorItem(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        if (errors.isEmpty()) {
            errors.add(new FieldErrorItem("body", ErrorCode.VALIDATION_FAILED.getMessage()));
        }
        return errors;
    }
}