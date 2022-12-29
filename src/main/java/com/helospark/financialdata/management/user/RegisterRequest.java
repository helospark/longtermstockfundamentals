package com.helospark.financialdata.management.user;

import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class RegisterRequest {
    @Email(message = "Not a valid email address")
    @NotNull
    @NotEmpty
    public String email;

    @Length(min = 5, max = 100, message = "Password must be between 5 and 100 characters")
    @NotNull
    @NotEmpty
    public String password;

    @Length(min = 5, max = 100, message = "Password must be between 5 and 100 characters")
    @NotNull
    @NotEmpty
    public String passwordVerify;

    @NotNull
    @NotEmpty
    public String token;

    @Override
    public String toString() {
        return "RegisterRequest [email=" + email + "]";
    }

}
