package com.etri.sodasapi.exception;

import lombok.Getter;

@Getter
public class ExceptionResponse {
    private int httpStatusCode;
    private String message;

    public ExceptionResponse(int httpStatusCode, String message) {
        this.httpStatusCode = httpStatusCode;
        this.message = message;
    }
}
