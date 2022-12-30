package com.helospark.financialdata.management.payment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.helospark.financialdata.management.config.ratelimit.RateLimit;
import com.stripe.Stripe;
import com.stripe.model.checkout.Session;

@Controller
public class PaymentResultController {
    public static final String PAYMENT_SUCCESS_URI = "/payment/success";
    public static final String PAYMENT_CANCEL_URI = "/payment/cancel";

    @Value("${stripe.key.secret}")
    private String stripeSecretKey;

    @GetMapping(PAYMENT_SUCCESS_URI)
    public String onPaymentSuccess(@RequestParam("sessionId") String sessionId, Model model) {
        model.addAttribute("afterPayment", true);
        model.addAttribute("sessionId", sessionId);
        return "index";
    }

    @GetMapping("/payment/return-from-management-portal")
    public String onPaymentSuccess(Model model) {
        model.addAttribute("generalMessageRedirectImmediately", "/");
        model.addAttribute("generalMessageRefreshJwt", true);
        return "index";
    }

    @GetMapping(PAYMENT_CANCEL_URI)
    public String onPaymentCancelled() {
        return "redirect:/";
    }

    @GetMapping("/payment/status")
    @ResponseBody
    @RateLimit(requestPerMinute = 100)
    public PaymentStatus getResult(String sessionId) {
        try {
            Stripe.apiKey = stripeSecretKey;
            Session session = Session.retrieve(sessionId);
            return new PaymentStatus(session.getPaymentStatus());
        } catch (Exception e) {
            return new PaymentStatus("error");
        }
    }

    static class PaymentStatus {
        public String status;

        public PaymentStatus(String status) {
            this.status = status;
        }

    }

}
