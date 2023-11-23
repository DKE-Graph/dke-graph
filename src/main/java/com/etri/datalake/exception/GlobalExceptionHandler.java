package com.etri.datalake.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(SodasException.class)
    public ResponseEntity<ExceptionResponse> baseExceptionHandler(SodasException e) {
        return ResponseEntity
                .status(e.getSodasExceptionCode().getHttpStatusCode())
                .body(new ExceptionResponse(e.getSodasExceptionCode().getHttpStatusCode(), e.getSodasExceptionCode().getMessage()));
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<String> handleUnauthorizedException(UnauthorizedException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
    }


}
