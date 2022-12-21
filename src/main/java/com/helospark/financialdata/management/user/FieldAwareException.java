package com.helospark.financialdata.management.user;

public class FieldAwareException extends RuntimeException {
    private String field;

    public FieldAwareException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }

}
