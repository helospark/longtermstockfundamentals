package com.helospark.financialdata.management.user;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.joda.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;

@Component
public class RegisterService {
    @Autowired
    private UserRepository userRepository;
    BCryptPasswordEncoder bCryptPasswordEncoder;

    public RegisterService() {
        bCryptPasswordEncoder = new BCryptPasswordEncoder(10, new SecureRandom());
    }

    public void registerUser(RegisterRequest request) {
        Optional<User> existingUser = userRepository.findByUserName(request.userName);

        if (existingUser.isPresent()) {
            throw new RegistrationException("Username '" + request.userName + "' is already used");
        }
        List<User> userByEmail = userRepository.findByEmail(request.email);
        if (!userByEmail.isEmpty()) {
            throw new RegistrationException("Email " + request.email + " is already used");
        }

        String encodedPassword = bCryptPasswordEncoder.encode(request.password);

        User user = new User();
        user.setAccountType(AccountType.FREE);
        user.setActivated(false);
        user.setEmail(request.email);
        user.setUserName(request.userName);
        user.setPassword(encodedPassword);
        user.setRegistered(new LocalDate().toString());

        userRepository.save(user);
    }
}
