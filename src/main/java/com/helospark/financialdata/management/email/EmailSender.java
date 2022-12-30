package com.helospark.financialdata.management.email;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailSender {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailSender.class);
    @Value("${smtp.username}")
    String userName;
    @Value("${smtp.password}")
    String password;
    @Value("${smtp.from}")
    String from;
    @Value("${smtp.debug}")
    boolean debug;

    public void sendEmail(String htmlMessage, String subject, String email) {
        String to = email;
        String host = "smtp.gmail.com";

        Properties properties = System.getProperties();

        // Setup mail server
        properties.put("mail.smtp.host", host);
        properties.put("mail.smtp.port", "465");
        properties.put("mail.smtp.ssl.enable", "true");
        properties.put("mail.smtp.auth", "true");

        Session session = Session.getInstance(properties, new javax.mail.Authenticator() {

            @Override
            protected PasswordAuthentication getPasswordAuthentication() {

                return new PasswordAuthentication(userName, password);

            }

        });

        if (debug) {
            session.setDebug(true);
        }

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
            message.setSubject(subject);
            message.setContent(htmlMessage,
                    "text/html; charset=utf-8");
            LOGGER.info("Sending email to {} with title '{}'", email, subject);
            Transport.send(message);
        } catch (MessagingException mex) {
            LOGGER.error("Unable to send email", mex);
        }

    }

}