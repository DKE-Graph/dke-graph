package com.etri.sodasapi.exception;

import lombok.Getter;

@Getter
public class SodasException extends RuntimeException{
    private final SodasExceptionCode sodasExceptionCode;

    public SodasException(SodasExceptionCode sodasExceptionCode) {
        this.sodasExceptionCode = sodasExceptionCode;
    }

}
