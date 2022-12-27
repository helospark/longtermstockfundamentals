package com.helospark.financialdata.management.payment;

public class StripeWebhookException extends RuntimeException {

    public StripeWebhookException(String message, Exception cause) {
        super(message, cause);
    }

}
