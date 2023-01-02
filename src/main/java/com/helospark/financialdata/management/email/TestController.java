package com.helospark.financialdata.management.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.helospark.financialdata.management.inspire.jobs.HighRoicJob;
import com.helospark.financialdata.management.user.ClearViewCountJob;

@Controller
public class TestController {
    @Autowired
    EmailSender emailSender;
    @Autowired
    private ClearViewCountJob clearViewCountJob;
    @Autowired
    private EmailTemplateReader emailTemplateReader;
    @Autowired
    private HighRoicJob highRoicJob;

    @GetMapping("/test")
    public String test(Model model) {
        //        String email = emailTemplateReader.readTemplate("declined-email-template.html", Map.of("STRIPE_CUSTOMER_PORTAL", "https://testUri.com"));

        emailSender.sendEmail("Test message", "Test message from code", "bcsababcsaba9021@gmail.com");
        //        highRoicJob.runHighRoicJob();

        return "index";
    }

}
