package com.helospark.financialdata.management.user;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class UserUpdateController {
    @Autowired
    private LoginController loginController;
    @Autowired
    private UserRepository userRepository;

    @GetMapping("/user/show-price")
    public String hidePrice(@RequestParam("enabled") boolean enabled, HttpServletRequest servletRequest) {
        User user = loginController.findUserOrThrow(servletRequest);

        user.setHidePrice(!enabled);

        userRepository.save(user);

        return "DONE";
    }

}
