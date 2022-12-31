package com.helospark.financialdata.management.config;

import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.user.LoginController;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class ModelEnrichControllerAdvice {
    @Autowired
    private LoginController loginController;
    @Value("#{${stripe.accountType}}")
    private Map<String, String> planToPriceMap;
    @Value("${recaptcha.site-key}")
    private String recaptchaSiteKey;
    @Value("${auth.google.client-id}")
    private String googleAuthClientId;

    @ModelAttribute
    public void enrichModel(Model model, HttpServletRequest request) {
        Optional<DecodedJWT> optionalJwt = loginController.getJwt(request);

        model.addAttribute("isLoggedIn", optionalJwt.isPresent());
        if (optionalJwt.isPresent()) {
            var jwt = optionalJwt.get();

            model.addAttribute("loginExpiry", jwt.getExpiresAt().getTime() - new Date().getTime());
            model.addAttribute("loginEmail", jwt.getSubject());
            model.addAttribute("cancelling", String.valueOf(jwt.getClaim(LoginController.CANCELLING_CLAIM)));
            model.addAttribute("loginAccountType", loginController.getAccountType(jwt).toString());
            model.addAttribute("loginAccountTypeIndex", loginController.getAccountType(jwt).ordinal());
        }

        for (var entry : planToPriceMap.entrySet()) {
            model.addAttribute("account_" + entry.getKey(), entry.getValue());
        }

        model.addAttribute("recaptchaSiteKey", recaptchaSiteKey);
        model.addAttribute("googleAuthClientId", googleAuthClientId);
    }

}
