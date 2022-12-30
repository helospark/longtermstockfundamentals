package com.helospark.financialdata.management.user;

import java.io.InputStream;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class DisposableEmailPredicate {
    List<String> disposableEmails;

    public DisposableEmailPredicate(@Value("classpath:disposable-emails.json") Resource source) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            InputStream stream = source.getInputStream();
            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, String.class);
            if (stream == null) {
                throw new RuntimeException("No disposable emails");
            }

            disposableEmails = objectMapper.readValue(stream, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isDisposableEmail(String email) {
        String[] parts = email.split("@");
        if (parts.length > 1) {
            for (var entry : disposableEmails) {
                if (parts[1].startsWith(entry)) {
                    return true;
                }
            }
        }
        return false;
    }

}
