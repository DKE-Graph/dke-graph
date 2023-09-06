package com.etri.sodasapi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SodasExceptionCode {
    // 400 BAD REQUEST
    BAD_REQUEST(HttpStatus.BAD_REQUEST.value(), "잘못된 요청입니다."),

    AUTHORIZATION_HEADER_NULL(HttpStatus.BAD_REQUEST.value(), "인증 헤더가 null입니다."),

    NON_POSITIVE_ID(HttpStatus.BAD_REQUEST.value(), "null");


    private final int httpStatusCode;
    private final String message;

    SodasExceptionCode(int httpStatusCode, String message) {
        this.httpStatusCode = httpStatusCode;
        this.message = message;
    }

}
