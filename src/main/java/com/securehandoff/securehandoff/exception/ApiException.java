package com.securehandoff.securehandoff.exception;

import org.springframework.http.HttpStatus;

public class ApiException {
        private final HttpStatus status;

    public ApiException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
