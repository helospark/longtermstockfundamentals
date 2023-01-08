package com.helospark.financialdata.management.email;

import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSender.class);
    @Value("${smtp.from}")
    String from;

    @Value("classpath:/dkim/dkim_private.der")
    Resource dkimResource;

    private Mailer mailer;

    public EmailSender(@Value("${smtp.username}") String userName,
            @Value("${smtp.password}") String password, @Value("${smtp.debug}") boolean debug) {
        mailer = MailerBuilder
                .withSMTPServer("smtp.gmail.com", 465, userName, password)
                .withTransportStrategy(TransportStrategy.SMTP_TLS)
                .withProperty("mail.smtp.auth", true)
                .withProperty("mail.smtp.ssl.enable", true)
                .withDebugLogging(debug)
                .buildMailer();
    }

    public void sendMail(String htmlMessage, String subject, String email) {
        try {
            Email emailToSend = EmailBuilder.startingBlank()
                    .from("LongTermStockFundamentals", from)
                    .to(null, email)
                    .withSubject(subject)
                    .withHTMLText(htmlMessage)
                    .signWithDomainKey(dkimResource.getInputStream(), "longtermstockfundamentals.com", "email")
                    .buildEmail();

            LOGGER.info("Sending email to {} with title '{}'", email, subject);
            mailer.sendMail(emailToSend);
            LOGGER.info("Sending email to {} success", email, subject);
        } catch (Exception e) {
            LOGGER.error("Error sending email", e);
        }
    }

}