package com.helospark.financialdata.management.user;

import java.security.SecureRandom;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;

@Component
public class RegisterService {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ConfirmationEmailService confirmationEmailService;
    @Value("${email.verification.enabled}")
    private boolean emailVerificationEnabled;

    BCryptPasswordEncoder bCryptPasswordEncoder;

    public RegisterService() {
        bCryptPasswordEncoder = new BCryptPasswordEncoder(10, new SecureRandom());
    }

    public void registerUser(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByEmail(request.email);

        if (existingUser.isPresent()) {
            throw new RegistrationException("User '" + request.email + "' is already used", "register_email");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(request.password);

        User user = new User();
        user.setAccountType(AccountType.FREE);
        user.setActivated(false);
        user.setEmail(request.email);
        user.setPassword(encodedPassword);
        user.setRegistered(new LocalDate().toString());

        userRepository.save(user);

        if (emailVerificationEnabled) {
            confirmationEmailService.sendConfirmationEmail(request.email);
        }
    }
}
