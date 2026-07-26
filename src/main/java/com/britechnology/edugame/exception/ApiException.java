package com.britechnology.edugame.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(message, HttpStatus.BAD_REQUEST);
    }

    public static ApiException notFound(String message) {
        return new ApiException(message, HttpStatus.NOT_FOUND);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(message, HttpStatus.UNAUTHORIZED);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(message, HttpStatus.FORBIDDEN);
    }

    public static ApiException internalServerError(String message) {
        return new ApiException(message, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    public static ApiException serviceUnavailable(String message) {
        return new ApiException(message, HttpStatus.SERVICE_UNAVAILABLE);
    }
}
