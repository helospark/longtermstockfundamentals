package com.helospark.financialdata.management.payment;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.view.RedirectView;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.payment.repository.StripeUserMapping;
import com.helospark.financialdata.management.payment.repository.StripeUserMappingRepository;
import com.helospark.financialdata.management.payment.repository.UserLastPayment;
import com.helospark.financialdata.management.payment.repository.UserLastPaymentRepository;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.Invoice;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);
    @Autowired
    private LoginController loginController;
    @Autowired
    private StripeUserMappingRepository stripeUserMappingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserLastPaymentRepository userLastPaymentRepository;

    @Value("${stripe.key.secret}")
    private String stripeSecretKey;
    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Value("${website.domain}")
    private String domain;
    @Value("${application.httpsOnly}")
    private boolean https;
    @Value("${website.port:0}")
    private int port;

    private Map<AccountType, String> accountToPriceMap = new HashMap<>();

    public PaymentController(@Value("#{${stripe.accountType}}") Map<String, String> accountToPriceMapString) {
        Stripe.apiKey = stripeSecretKey;
        for (var entry : accountToPriceMapString.entrySet()) {
            accountToPriceMap.put(AccountType.fromString(entry.getKey()), entry.getValue());
        }
    }

    @PostMapping("/payment/initialize")
    public Object onPayment(@RequestParam("plan") String plan, HttpServletRequest servletRequest, Model model) {
        Optional<DecodedJWT> optionalJwt = loginController.getJwt(servletRequest);

        if (!optionalJwt.isPresent()) {
            model.addAttribute("generalMessageTitle", "Not logged in.");
            model.addAttribute("generalMessageBody", "In order to subscribe to this plan, you have to log in.");
            model.addAttribute("generalMessageRedirect", "/");
            return "index";
        } else {
            DecodedJWT jwt = optionalJwt.get();

            Optional<StripeUserMapping> userToStripeOptional = stripeUserMappingRepository.findStripeUserMappingByEmail(jwt.getSubject());

            String customerId = "";
            if (userToStripeOptional.isEmpty()) {
                customerId = createUser(jwt);
                StripeUserMapping userMappingCreate = new StripeUserMapping();
                userMappingCreate.setEmail(jwt.getSubject());
                userMappingCreate.setStripeCustomerId(customerId);
                stripeUserMappingRepository.save(userMappingCreate);
            } else {
                StripeUserMapping stripeUserMapping = userToStripeOptional.get();

                customerId = stripeUserMapping.getStripeCustomerId();
            }

            String paymentUri = createSubscription(plan, customerId);

            RedirectView redirectView = new RedirectView(paymentUri);
            redirectView.setStatusCode(HttpStatus.SEE_OTHER);
            return redirectView;
        }
    }

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public String onPaymentException(PaymentException exception, Model model) {
        model.addAttribute("generalMessageTitle", "Unable to subscribe to plan");
        model.addAttribute("generalMessageBody", exception.getMessage());
        model.addAttribute("generalMessageRedirect", "/");
        return "index";
    }

    @PostMapping("/stripe/webhook")
    private ResponseEntity<String> stripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        String endpointSecret = "";

        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, endpointSecret);
        } catch (SignatureVerificationException e) {
            LOGGER.error("Invalid stripe signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }

        String customerId = getCustomerId(event, payload);

        Optional<StripeUserMapping> userMapping = stripeUserMappingRepository.getStripeUserMapping(customerId);
        if (!userMapping.isPresent()) {
            LOGGER.error("Unknown account {}", event.getAccount());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
        Optional<User> optionalUser = userRepository.findByEmail(userMapping.get().getEmail());
        if (!optionalUser.isPresent()) {
            LOGGER.error("User doesn't exist {}", userMapping.get().getEmail());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
        var user = optionalUser.get();

        switch (event.getType()) {
            case "checkout.session.completed": {
                var lineItems = ((Session) event.getDataObjectDeserializer().getObject().get()).getLineItems().getData();

                if (lineItems == null || lineItems.size() != 1) {
                    LOGGER.error("Exactly one lineItem expected for user={}, lineItems={}", user.getEmail(), ReflectionToStringBuilder.toString(lineItems));
                }

                String priceId = lineItems.get(0).getPrice().getId();

                user.setAccountType(getAccountTypeFromPayment(priceId));
                userRepository.save(user);

                updateLastPaymentDate(user);

                break;
            }
            case "invoice.paid": {
                updateLastPaymentDate(user);
                break;
            }
            case "invoice.payment_failed": {
                Invoice invoice = ((Invoice) event.getDataObjectDeserializer().getObject().get());
                LOGGER.error("Payment failed attempts=" + invoice.getAttemptCount());
                // The payment failed or the customer does not have a valid payment method.
                // The subscription becomes past_due. Notify your customer and send them to the
                // customer portal to update their payment information.
                break;
            }
            default:
                LOGGER.info("Unhandled event received for user={}, eventType={}", user.getEmail(), event.getType());
        }

        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    private String getCustomerId(Event event, String rawJson) {
        if (event.getType().equals("checkout.session.")) {
            return ((Session) event.getDataObjectDeserializer().getObject().get()).getCustomer();
        } else if (event.getType().contains("invoice.")) {
            return ((Invoice) event.getDataObjectDeserializer().getObject().get()).getCustomer();
        } else if (event.getType().contains("customer.")) {
            return ((Customer) event.getDataObjectDeserializer().getObject().get()).getId();
        } else if (event.getType().contains("payment_intent.")) {
            return ((PaymentIntent) event.getDataObjectDeserializer().getObject().get()).getCustomer();
        }
        return null;
    }

    private AccountType getAccountTypeFromPayment(String plan) {
        for (var entry : accountToPriceMap.entrySet()) {
            if (entry.getValue().equals(plan)) {
                return entry.getKey();
            }
        }
        throw new PaymentException("Unable to find price type", null);
    }

    private void updateLastPaymentDate(User user) {
        UserLastPayment userLastPayment = new UserLastPayment();
        userLastPayment.setEmail(user.getEmail());
        userLastPayment.setLastPaymentDate(LocalDateTime.now());

        userLastPaymentRepository.save(userLastPayment);
    }

    private String createSubscription(String priceId, String customerId) {
        try {
            SessionCreateParams params = new SessionCreateParams.Builder()
                    .setCustomer(customerId)
                    .setSuccessUrl(buildSuccessUrl())
                    .setCancelUrl(buildCancelUrl())
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .addLineItem(new SessionCreateParams.LineItem.Builder()
                            .setQuantity(1L)
                            .setPrice(priceId)
                            .build())
                    .build();

            Session session = Session.create(params);

            return session.getUrl();
        } catch (Exception e) {
            throw new PaymentException("Unable to create subscription on Stripe", e);
        }
    }

    private String buildSuccessUrl() {
        String url = buildBaseUrl();
        url += PaymentResultController.PAYMENT_SUCCESS_URI;
        url += "?sessionId={CHECKOUT_SESSION_ID}";
        return url;
    }

    private String buildCancelUrl() {
        String url = buildBaseUrl();
        url += PaymentResultController.PAYMENT_CANCEL_URI;
        return url;
    }

    public String buildBaseUrl() {
        String url = https ? "https://" : "http://";
        url += domain;
        if (port != 0) {
            url += ":" + port;
        }
        return url;
    }

    private String createUser(DecodedJWT jwt) {
        try {
            String email = jwt.getSubject();

            CustomerCreateParams params = CustomerCreateParams
                    .builder()
                    .setEmail(email)
                    .build();

            Customer customer = Customer.create(params);

            return customer.getId();
        } catch (Exception e) {
            throw new PaymentException("Unable to create user on Stripe", e);
        }
    }

}
