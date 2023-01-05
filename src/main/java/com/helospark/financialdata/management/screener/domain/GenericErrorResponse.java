package com.helospark.financialdata.management.screener.domain;

public class GenericErrorResponse {
    public String errorMessage;

    public GenericErrorResponse(String message) {
        this.errorMessage = message;
    }
}
