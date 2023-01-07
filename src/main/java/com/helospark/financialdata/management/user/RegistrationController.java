package com.helospark.financialdata.management.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.management.config.ratelimit.RateLimit;

import jakarta.validation.ValidationException;

@RestController
public class RegistrationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegistrationController.class);
    @Autowired
    RegisterService registerService;
    @Autowired
    private RecaptchaValidationService recaptchaService;
    @Autowired
    private DisposableEmailPredicate disposableEmailPredicate;
    @Autowired
    private PasswordValidator passwordValidator;

    @PostMapping("/user/register")
    @RateLimit(requestPerMinute = 10)
    public RegisterResponse registerUser(@RequestBody RegisterRequest request) {
        if (!request.password.equals(request.passwordVerify)) {
            throw new RegistrationException("Passwords must match", "register_password_verify");
        }
        if (!passwordValidator.validatePassword(request.password)) {
            throw new RegistrationException("Password must be at least 5 characters long", "register_password");
        }
        if (!recaptchaService.isValidCaptcha(request.token, request.email)) {
            throw new RegistrationException("Captcha not valid", null);
        }
        if (request.acceptTerms == null || !request.acceptTerms) {
            throw new RegistrationException("You must accept terms and privacy policy", "accept_terms");
        }
        if (disposableEmailPredicate.isDisposableEmail(request.email)) {
            throw new RegistrationException("You cannot use a disposable email", "register_email");
        }

        registerService.registerUser(request);

        return new RegisterResponse(request.email);
    }

    @ExceptionHandler(value = { RegistrationException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public RegisterResponse exceptionClientHandler(RegistrationException exception) {
        LOGGER.warn("Exception during registration", exception);
        return new RegisterResponse(null, exception, exception.getField());
    }

    @ExceptionHandler(value = { ValidationException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public RegisterResponse exceptionClientHandler(ValidationException exception) {
        LOGGER.warn("Exception during registration", exception);
        return new RegisterResponse(null, exception, null);
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public RegisterResponse exceptionServerHandler(Exception exception) {
        LOGGER.error("Exception during registration", exception);
        return new RegisterResponse(null, exception, null);
    }
}
