package com.helospark.financialdata.management.user;

public class DeleteAccountResult {
    public String errorMessage;

    public DeleteAccountResult(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

}
