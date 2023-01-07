package com.helospark.financialdata.management.user;

public class GenericResponseAccountResult {
    public String errorMessage;

    public GenericResponseAccountResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
