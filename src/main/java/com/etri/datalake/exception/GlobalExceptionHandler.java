package com.etri.datalake.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class GlobalExceptionHandler {
    @ExceptionHandler(SodasException.class)
    public ResponseEntity<ExceptionResponse> baseExceptionHandler(SodasException e) {
        return ResponseEntity
                .status(e.getSodasExceptionCode().getHttpStatusCode())
                .body(new ExceptionResponse(e.getSodasExceptionCode().getHttpStatusCode(), e.getSodasExceptionCode().getMessage()));
    }
}
