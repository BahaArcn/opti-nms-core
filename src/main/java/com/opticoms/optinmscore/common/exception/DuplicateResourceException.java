package com.opticoms.optinmscore.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class DuplicateResourceException extends ResponseStatusException {

    public DuplicateResourceException(String resourceName, String fieldName, String fieldValue) {
        super(HttpStatus.CONFLICT,
                String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }

    public DuplicateResourceException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
