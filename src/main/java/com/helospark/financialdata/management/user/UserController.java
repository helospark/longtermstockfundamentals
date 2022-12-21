package com.helospark.financialdata.management.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.ValidationException;

@RestController
public class UserController {
    @Autowired
    RegisterService userService;

    @PostMapping("/user/register")
    public RegisterResponse registerUser(@RequestBody RegisterRequest request) {
        if (!request.password.equals(request.passwordVerify)) {
            throw new RegistrationException("Passwords must match", "register_password_verify");
        }

        userService.registerUser(request);

        return new RegisterResponse(request.email);
    }

    @ExceptionHandler(value = { RegistrationException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public RegisterResponse exceptionClientHandler(RegistrationException exception) {
        return new RegisterResponse(null, exception, exception.getField());
    }

    @ExceptionHandler(value = { ValidationException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public RegisterResponse exceptionClientHandler(ValidationException exception) {
        return new RegisterResponse(null, exception, null);
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public RegisterResponse exceptionServerHandler(Exception exception) {
        return new RegisterResponse(null, exception, null);
    }
}