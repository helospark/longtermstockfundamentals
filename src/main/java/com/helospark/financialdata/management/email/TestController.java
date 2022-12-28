package com.helospark.financialdata.management.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TestController {
    @Autowired
    EmailSender emailSender;

    @GetMapping("/test")
    public String test(Model model) {
        model.addAttribute("generalMessageTitle", "Subscription is not yet updated");
        model.addAttribute("generalMessageBody", "Status of Stripe payment is FREE, please check payment method.");
        model.addAttribute("generalMessageRedirect", "/");
        model.addAttribute("generalMessageRefreshJwt", true);
        return "index";
    }

}
