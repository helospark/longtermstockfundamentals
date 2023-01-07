package com.helospark.financialdata.management.user;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.config.ratelimit.RateLimit;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class PasswordChangeController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PasswordChangeController.class);
    @Autowired
    private LoginController loginController;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private PasswordValidator passwordValidator;

    @PostMapping("/user/password")
    @RateLimit(requestPerMinute = 5)
    public GenericResponseAccountResult passwordChange(@RequestBody PasswordChangeRequest request, HttpServletRequest servletRequest, HttpServletResponse response, Model model) {
        LOGGER.info("Received password change request");
        Optional<DecodedJWT> optionalJwt = loginController.getJwt(servletRequest);

        if (!optionalJwt.isPresent()) {
            throw new PasswordChangeException("Not logged in");
        }

        if (!request.newPassword.equals(request.newPasswordVerify)) {
            throw new PasswordChangeException("New passwords do not match");
        }
        if (request.newPassword.equals(request.oldPassword)) {
            throw new PasswordChangeException("New password cannot be the same as the old password");
        }

        if (!passwordValidator.validatePassword(request.newPassword)) {
            throw new PasswordChangeException("New password must be at least 5 characters long");
        }

        Optional<User> optionalUser = userRepository.findByEmail(optionalJwt.get().getSubject());

        if (!optionalUser.isPresent()) {
            throw new PasswordChangeException("User does not exists");
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(request.oldPassword, user.getPassword())) {
            throw new PasswordChangeException("Old password is not correct");
        }

        LOGGER.info("Changing password for user {}.", user);

        user.setPassword(passwordEncoder.encode(request.newPassword));
        userRepository.save(user);

        return new GenericResponseAccountResult("");
    }

    @ExceptionHandler(value = { PasswordChangeException.class })
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public GenericResponseAccountResult exceptionClientHandler(PasswordChangeException exception, Model model) {
        LOGGER.error("Error while changing password", exception);
        return new GenericResponseAccountResult(exception.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public GenericResponseAccountResult exceptionServerHandler(Exception exception) {
        LOGGER.error("Exception during password change", exception);
        return new GenericResponseAccountResult("Unexpected error while changeing password");
    }
}
