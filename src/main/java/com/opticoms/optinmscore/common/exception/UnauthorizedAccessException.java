package com.opticoms.optinmscore.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class UnauthorizedAccessException extends ResponseStatusException {

    public UnauthorizedAccessException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }

    public UnauthorizedAccessException() {
        super(HttpStatus.FORBIDDEN, "You do not have permission to access this resource");
    }
}
