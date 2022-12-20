package com.helospark.financialdata.management.user;

public class RegisterResponse {
    public String userName;
    public String errorMessage;

    public RegisterResponse(String userName) {
        this.userName = userName;
    }

    public RegisterResponse(String userName, Exception exception) {
        this.userName = userName;
        this.errorMessage = exception.getMessage();
    }

}
