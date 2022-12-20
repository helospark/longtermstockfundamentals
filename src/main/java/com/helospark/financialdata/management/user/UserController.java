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
            throw new RegistrationException("Passwords must match");
        }

        userService.registerUser(request);

        return new RegisterResponse(request.userName);
    }

    @ExceptionHandler(value = { RegistrationException.class, ValidationException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public RegisterResponse exceptionClientHandler(Exception exception) {
        return new RegisterResponse(null, exception);
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public RegisterResponse exceptionServerHandler(Exception exception) {
        return new RegisterResponse(null, exception);
    }
}
