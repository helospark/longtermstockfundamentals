package com.helospark.financialdata.management.user;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.email.EmailSender;
import com.helospark.financialdata.management.helper.SimpleTemplater;
import com.helospark.financialdata.management.user.repository.ConfirmationEmail;
import com.helospark.financialdata.management.user.repository.ConfirmationEmailRepository;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;

@Component
public class ConfirmationEmailService {
    private SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private ConfirmationEmailRepository confirmationEmailRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private EmailSender emailSender;
    @Value("${website.domain}")
    private String domain;
    @Value("${application.httpsOnly}")
    private boolean https;
    @Value("${website.port:0}")
    private int port;
    @Autowired
    private SimpleTemplater simpleTemplater;

    public void sendConfirmationEmail(String email) {
        byte[] bytes = new byte[30];
        secureRandom.nextBytes(bytes);
        String confirmationToken = UUID.randomUUID().toString();

        ConfirmationEmail confirmationEmail = new ConfirmationEmail();
        confirmationEmail.setConfirmationId(confirmationToken);
        confirmationEmail.setEmail(email);
        ZoneId zoneId = ZoneId.systemDefault();
        long expiration = LocalDateTime.now().plusDays(10).atZone(zoneId).toEpochSecond();
        confirmationEmail.setExpiration(expiration);

        confirmationEmailRepository.save(confirmationEmail);

        String confirmationUrl = buildConfirmationUrl(confirmationToken);

        Map<String, String> templates = Map.of("URL", confirmationUrl);

        String subject = "LongTermStockFundamentals - Activate account";
        String message = "Hello!"
                + "<p>You are receiving this message, because you have signed up to <a href=\"longtermstockfundamentals.com\">longtermstockfundamentals.com</a> with this email address.<br/>"
                + "Click on this link to activate your account:<br/>"
                + "<a href=\"{{URL}}\">{{URL}}</a>"
                + "</p><br/>"
                + "<p>If you have not signed up, you can safely ignore this message.</p>"
                + "Regards,<br/>LongTermStocksFundamentals.com team";

        emailSender.sendEmail(simpleTemplater.template(message, templates), subject, email);
    }

    public void activeAccount(String token) {
        Optional<ConfirmationEmail> result = confirmationEmailRepository.getConfirmationEmail(token);

        if (result.isPresent()) {
            confirmationEmailRepository.removeConfirmationEmail(token);
            Optional<User> optionalUser = userRepository.findByEmail(result.get().getEmail());
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();
                user.setActivated(true);
                userRepository.save(user);
            } else {
                throw new RuntimeException("User doesn't exist anymore.");
            }
        } else {
            throw new RuntimeException("Invalid token passed");
        }
    }

    private String buildConfirmationUrl(String confirmationToken) {
        String url = https ? "https://" : "http://";
        url += domain;
        if (port != 0) {
            url += ":" + port;
        }
        url += ConfirmationEmailController.ACTIVATE_URI;
        url += "?token=" + confirmationToken;
        return url;
    }

}
