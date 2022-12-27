package com.helospark.financialdata.management.payment;

public class PaymentException extends RuntimeException {

    public PaymentException(String message, Exception cause) {
        super(message, cause);
    }

}
