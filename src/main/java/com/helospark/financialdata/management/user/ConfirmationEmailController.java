package com.helospark.financialdata.management.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

@Controller
public class ConfirmationEmailController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConfirmationEmailController.class);
    public static final String ACTIVATE_URI = "/user/activate";

    @Autowired
    private ConfirmationEmailService confirmationEmailService;

    @GetMapping(ACTIVATE_URI)
    public String activeAccount(@RequestParam("token") @NotNull @NotEmpty String token, Model model) {
        try {
            confirmationEmailService.activeAccount(token);
            model.addAttribute("generalMessageTitle", "Account has been succesfully activated.");
            model.addAttribute("generalMessageBody", "Enjoy your research!");
            model.addAttribute("generalMessageRedirect", "/");
        } catch (RuntimeException e) {
            LOGGER.warn("Cannot activate account", e);
            model.addAttribute("generalMessageTitle", "Account activation failed!");
            model.addAttribute("generalMessageBody", "Error due to: " + e.getMessage());
            model.addAttribute("generalMessageRedirect", "/");
        }
        return "index";
    }

}
