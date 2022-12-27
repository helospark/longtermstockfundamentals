package com.helospark.financialdata.management.helper;

import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SimpleTemplater {

    public String template(String string, Map<String, String> template) {
        String result = string;
        for (var entry : template.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

}
