package com.helospark.financialdata.management.payment;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class PaymentResultController {
    public static final String PAYMENT_SUCCESS_URI = "/payment/success";
    public static final String PAYMENT_CANCEL_URI = "/payment/cancel";

    @GetMapping(PAYMENT_SUCCESS_URI)
    public String onPaymentSuccess(@RequestParam("sessionId") String sessionId) {
        return "index";
    }

    @GetMapping(PAYMENT_CANCEL_URI)
    public String onPaymentCancelled() {
        return "redirect:/";
    }

}
