package com.helospark.financialdata.management.user;

import org.springframework.stereotype.Component;

@Component
public class PasswordValidator {

    public boolean validatePassword(String password) {
        if (password == null) {
            return false;
        }
        if (password.length() < 5) {
            return false;
        }
        // more validation here
        return true;
    }

}
