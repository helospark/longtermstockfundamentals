package com.helospark.financialdata.management.inspire;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InspirationClientException extends RuntimeException {

    public InspirationClientException(String message) {
        super(message);
    }

}
