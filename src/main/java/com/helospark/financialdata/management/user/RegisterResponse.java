package com.helospark.financialdata.management.user;

public class RegisterResponse {
    public String email;
    public String errorMessage;
    public String fieldReference;

    public RegisterResponse(String email) {
        this.email = email;
    }

    public RegisterResponse(String email, Exception exception, String fieldReference) {
        this.email = email;
        this.errorMessage = exception.getMessage();
        this.fieldReference = fieldReference;
    }

}
