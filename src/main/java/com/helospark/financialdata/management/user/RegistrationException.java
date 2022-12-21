package com.helospark.financialdata.management.user;

public class RegistrationException extends FieldAwareException {

    public RegistrationException(String string, String field) {
        super(string, field);
    }

}
