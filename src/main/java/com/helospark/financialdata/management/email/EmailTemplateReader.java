package com.helospark.financialdata.management.email;

import java.io.InputStream;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.helospark.financialdata.management.helper.SimpleTemplater;

@Service
public class EmailTemplateReader {
    private static final Logger LOGGER = LoggerFactory.getLogger(EmailTemplateReader.class);
    @Autowired
    private SimpleTemplater simpleTemplater;

    public String readTemplate(String templateName, Map<String, String> substitutions) {
        try {
            InputStream in = this.getClass().getResourceAsStream("/email/" + templateName);
            String template = new String(in.readAllBytes());

            return simpleTemplater.template(template, substitutions);
        } catch (Exception e) {
            LOGGER.error("Unable to process template", e);
            throw new RuntimeException(e);
        }
    }
}
