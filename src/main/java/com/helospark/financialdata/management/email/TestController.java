package com.helospark.financialdata.management.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.helospark.financialdata.management.user.ClearViewCountJob;

@Controller
public class TestController {
    @Autowired
    EmailSender emailSender;
    @Autowired
    private ClearViewCountJob clearViewCountJob;

    @GetMapping("/test")
    public String test(Model model) {
        //        clearViewCountJob.clearViewCount();
        return "index";
    }

}
