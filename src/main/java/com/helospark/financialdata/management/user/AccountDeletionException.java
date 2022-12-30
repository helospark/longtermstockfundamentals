package com.helospark.financialdata.management.user;

public class AccountDeletionException extends RuntimeException {

    public AccountDeletionException(String message, Throwable cause) {
        super(message, cause);
    }

    public AccountDeletionException(String message) {
        super(message);
    }

}
