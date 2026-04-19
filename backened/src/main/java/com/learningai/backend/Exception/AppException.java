package com.learningai.backend.Exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public AppException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static AppException notFound(String message) {
        return new AppException(message, HttpStatus.NOT_FOUND, "NOT_FOUND");
    }

    public static AppException badRequest(String message) {
        return new AppException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static AppException unauthorized(String message) {
        return new AppException(message, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
    }

    public static AppException forbidden(String message) {
        return new AppException(message, HttpStatus.FORBIDDEN, "FORBIDDEN");
    }

    public static AppException conflict(String message) {
        return new AppException(message, HttpStatus.CONFLICT, "CONFLICT");
    }
}
