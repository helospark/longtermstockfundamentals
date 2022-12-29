package com.helospark.financialdata.management.user;

import java.net.URI;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

@Service
public class RecaptchaValidationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RecaptchaValidationService.class);
    @Autowired
    private RestTemplate restTemplate;
    @Value("${recaptcha.secret-key}")
    private String recaptchaSecret;

    private static Pattern RESPONSE_PATTERN = Pattern.compile("[A-Za-z0-9_-]+");

    public boolean isValidCaptcha(String response, String email) {
        if (!responseSanityCheck(response)) {
            return false;
        }

        URI verifyUri = URI.create(String.format(
                "https://www.google.com/recaptcha/api/siteverify?secret=%s&response=%s",
                recaptchaSecret, response));

        GoogleResponse googleResponse = restTemplate.getForObject(verifyUri, GoogleResponse.class);

        if (!googleResponse.isSuccess()) {
            LOGGER.warn("Invalid captcha, response='{}', email={}", googleResponse);
        }
        return googleResponse.isSuccess();
    }

    private boolean responseSanityCheck(String response) {
        return StringUtils.hasLength(response) && RESPONSE_PATTERN.matcher(response).matches();
    }

}
