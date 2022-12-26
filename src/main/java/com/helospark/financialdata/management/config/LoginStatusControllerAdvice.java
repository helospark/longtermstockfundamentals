package com.helospark.financialdata.management.config;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.user.LoginController;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class LoginStatusControllerAdvice {
    @Autowired
    private LoginController loginController;

    @ModelAttribute
    public void enrichModel(Model model, HttpServletRequest request) {
        Optional<DecodedJWT> optionalJwt = loginController.getJwt(request);

        model.addAttribute("isLoggedIn", optionalJwt.isPresent());
        if (optionalJwt.isPresent()) {
            var jwt = optionalJwt.get();

            model.addAttribute("loginExpiry", jwt.getExpiresAt().toInstant());
            model.addAttribute("loginEmail", jwt.getSubject());
            model.addAttribute("loginAccountType", loginController.getAccountType(jwt).toString());
            model.addAttribute("loginAccountTypeIndex", loginController.getAccountType(jwt).ordinal());
        }
        request.getCookies();
    }

}
