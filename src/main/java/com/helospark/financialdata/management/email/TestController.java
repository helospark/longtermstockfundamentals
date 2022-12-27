package com.helospark.financialdata.management.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    EmailSender emailSender;

    @GetMapping("/test")
    public void test() {
        emailSender.sendEmail("Test email body", "Test subject", "bcsababcsaba9021@gmail.com");
    }

}
