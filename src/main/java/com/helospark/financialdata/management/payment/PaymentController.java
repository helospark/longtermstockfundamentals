package com.helospark.financialdata.management.payment;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
import com.helospark.financialdata.management.config.ratelimit.RateLimit;
import com.helospark.financialdata.management.email.EmailSender;
import com.helospark.financialdata.management.email.EmailTemplateReader;
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
import com.stripe.model.InvoiceLineItem;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionCreateParams.SubscriptionData;

import jakarta.servlet.http.HttpServletRequest;

@Controller
public class PaymentController {
    private static final String PLAN_METADATA = "plan";
    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);
    @Autowired
    private LoginController loginController;
    @Autowired
    private StripeUserMappingRepository stripeUserMappingRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserLastPaymentRepository userLastPaymentRepository;
    @Autowired
    private EmailSender emailSender;
    @Autowired
    private EmailTemplateReader emailTemplateReader;

    @Value("${stripe.key.secret}")
    private String stripeSecretKey;
    @Value("${stripe.webhook.secret}")
    private String stripeWebhookSecret;

    @Value("${website.domain}")
    private String domain;
    @Value("${website.https}")
    private boolean https;
    @Value("${website.port:0}")
    private int port;
    @Value("${payment.failed.email-enabled}")
    private boolean sendEmailOnFailure;

    private Map<AccountType, String> accountToPriceMap = new HashMap<>();

    public PaymentController(@Value("#{${stripe.accountType}}") Map<String, String> accountToPriceMapString) {
        for (var entry : accountToPriceMapString.entrySet()) {
            accountToPriceMap.put(AccountType.fromString(entry.getKey()), entry.getValue());
        }
    }

    @PostMapping("/payment/initialize")
    @RateLimit(requestPerMinute = 5)
    public Object onPayment(@RequestParam(PLAN_METADATA) String plan, HttpServletRequest servletRequest, Model model) {
        Stripe.apiKey = stripeSecretKey;
        Optional<DecodedJWT> optionalJwt = loginController.getJwt(servletRequest);

        if (!optionalJwt.isPresent()) {
            LOGGER.info("User not logged in");
            model.addAttribute("generalMessageTitle", "Not logged in");
            model.addAttribute("generalMessageBody", "You have to login to or <a href=\"/#sign_up\">sign up</a> to this plan.");
            model.addAttribute("generalMessageRedirect", "/");
            return "index";
        } else {
            DecodedJWT jwt = optionalJwt.get();

            Optional<StripeUserMapping> userToStripeOptional = stripeUserMappingRepository.findStripeUserMappingByEmail(jwt.getSubject());

            StripeUserMapping stripeUserMapping;
            AccountType accountType = getAccountTypeFromPayment(plan);
            if (userToStripeOptional.isEmpty()) {
                String customerId = createCustomer(jwt);
                stripeUserMapping = new StripeUserMapping();
                stripeUserMapping.setEmail(jwt.getSubject());
                stripeUserMapping.setStripeCustomerId(customerId);
                stripeUserMapping.setLastRequestedAccountType(accountType);
                stripeUserMapping.setCurrentSubscriptionId(null);
                stripeUserMappingRepository.save(stripeUserMapping);
            } else {
                stripeUserMapping = userToStripeOptional.get();
                stripeUserMapping.setLastRequestedAccountType(accountType);
                stripeUserMappingRepository.save(stripeUserMapping);
            }
            String customerId = stripeUserMapping.getStripeCustomerId();

            if (accountType.equals(AccountType.FREE)) {
                if (stripeUserMapping.getCurrentSubscriptionId() != null) {
                    cancelSubsciption(customerId, stripeUserMapping, jwt.getSubject());
                    model.addAttribute("generalMessageTitle", "Subscription cancelled");
                    model.addAttribute("generalMessageBody",
                            "Successfully cancelled the plan, you will not be charged anymore. However you may still enjoy your paid account until the end of your payment period.");
                    model.addAttribute("generalMessageRedirect", "/");
                    model.addAttribute("generalMessageRefreshJwt", true);
                    return "index";
                } else {
                    LOGGER.error("User {} wants to cancel their subscription, but they have no subscription", jwt.getSubject());
                    model.addAttribute("generalMessageTitle", "No subscription found");
                    model.addAttribute("generalMessageBody", "Wanted to cancel subscription, but no current subscription found. Contact support.");
                    model.addAttribute("generalMessageRedirect", "/");
                    return "index";
                }
            } else if (stripeUserMapping.getCurrentSubscriptionId() == null || isSubsciptionAlreadyCanceled(stripeUserMapping.getCurrentSubscriptionId())) {
                String paymentUri = createSubscription(plan, customerId);

                RedirectView redirectView = new RedirectView(paymentUri);
                redirectView.setStatusCode(HttpStatus.SEE_OTHER);
                return redirectView;
            } else {
                updateSubscription(plan, customerId, stripeUserMapping, accountType, model, jwt);
                return "index";
            }
        }
    }

    private boolean isSubsciptionAlreadyCanceled(String currentSubscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(currentSubscriptionId);
            return subscription.getStatus().equals("canceled") || subscription.getStatus().equals("incomplete_expired");
        } catch (Exception e) {
            throw new PaymentException("Unable to load the previous subsciption", e);
        }
    }

    private void cancelSubsciption(String customerId, StripeUserMapping stripeUserMapping, String email) {
        try {
            Stripe.apiKey = stripeSecretKey;

            Subscription subscription = Subscription.retrieve(stripeUserMapping.getCurrentSubscriptionId());

            if (subscription.getStatus().equals("canceled") || subscription.getStatus().equals("incomplete_expired")) {
                User user = userRepository.findByEmail(email).get();
                if (user.getAccountType() != AccountType.FREE) {
                    LOGGER.warn("Wanted to cancel subscription, but it was already canceled, but it's not consistent with DB");
                    user.setAccountType(AccountType.FREE);
                    userRepository.save(user);
                }
            } else {
                SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                        .setCancelAtPeriodEnd(true)
                        .setDescription("Cancelling subscription")
                        .build();

                subscription.update(params);

                User user = userRepository.findByEmail(email).get();
                user.setCancelling(true);
                userRepository.save(user);
            }
        } catch (Exception e) {
            throw new PaymentException("Unable to cancel subscription", e);
        }
    }

    private void updateSubscription(String plan, String customerId, StripeUserMapping stripeUserMapping, AccountType accountType, Model model, DecodedJWT jwt) {
        try {
            Stripe.apiKey = stripeSecretKey;

            Subscription subscription = Subscription.retrieve(stripeUserMapping.getCurrentSubscriptionId());

            SubscriptionUpdateParams params = SubscriptionUpdateParams.builder()
                    .setCancelAtPeriodEnd(false)
                    .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                    .setDescription("Modifying the subscription to " + accountType)
                    .addItem(SubscriptionUpdateParams.Item.builder()
                            .setId(subscription.getItems().getData().get(0).getId())
                            .setPrice(plan)
                            .build())
                    .build();

            Subscription updatedSubscription = subscription.update(params);

            if (updatedSubscription.getStatus().equals("active")) {
                var user = userRepository.findByEmail(jwt.getSubject()).get();
                user.setAccountType(accountType);
                userRepository.save(user);

                model.addAttribute("generalMessageTitle", "Subscription updated");
                model.addAttribute("generalMessageBody", "Succesfully updated to " + accountType + " plan.");
                model.addAttribute("generalMessageRedirect", "/");
                model.addAttribute("generalMessageRefreshJwt", true);
            } else {
                String managementUrl = createManagementUrl(customerId);
                model.addAttribute("generalMessageTitle", "Subscription update is pending");
                model.addAttribute("generalMessageBody",
                        "Your Stripe payment status is '" + updatedSubscription.getStatus()
                                + "', this is most likely due to an additional 3DS authentication requirements, or it could be due to payment failure.<br/>"
                                + "To finish payment, please navigate to <a href=\"" + managementUrl + "\">Stripe customer portal</a>, and pay your 'open' invoice.<br/>");
                model.addAttribute("generalMessageRedirect", "/");
                model.addAttribute("generalMessageRefreshJwt", true);
            }

        } catch (Exception e) {
            throw new PaymentException("Unable to update subscription", e);
        }
    }

    private String createManagementUrl(String customerId) {
        return createManagementUrlWithReturn(customerId, buildBaseUrl());
    }

    public String createManagementUrlWithReturn(String customerId, String returnUri) {
        try {
            Stripe.apiKey = stripeSecretKey;

            com.stripe.param.billingportal.SessionCreateParams params = new com.stripe.param.billingportal.SessionCreateParams.Builder()
                    .setReturnUrl(returnUri)
                    .setCustomer(customerId)
                    .build();
            com.stripe.model.billingportal.Session portalSession = com.stripe.model.billingportal.Session.create(params);

            return portalSession.getUrl();
        } catch (Exception e) {
            throw new PaymentException("Unable to get link of customer portal", e);
        }
    }

    @ExceptionHandler(PaymentException.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public String onPaymentException(PaymentException exception, Model model) {
        LOGGER.error("Error making payment", exception);
        model.addAttribute("generalMessageTitle", "Unable to subscribe to plan");
        model.addAttribute("generalMessageBody", exception.getMessage());
        model.addAttribute("generalMessageRedirect", "/");
        return "index";
    }

    @PostMapping("/stripe/webhook")
    private ResponseEntity<String> stripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        Stripe.apiKey = stripeSecretKey;
        LOGGER.debug("Webhook payment event received: payload='{}', signature='{}'", payload, sigHeader);
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            LOGGER.error("Invalid stripe signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }
        String eventType = event.getType();
        LOGGER.info("[{}] Received webhook event", eventType);

        try {
            String customerId = getCustomerId(event, payload);

            LOGGER.info("[{}] Event customerId={}", eventType, customerId);

            if (customerId == null) {
                LOGGER.info("[{}] Skipping event '{}', because it doesn't have any customerId", eventType, event.getId());
                return ResponseEntity.status(HttpStatus.OK).body("");
            }

            Optional<StripeUserMapping> optionalUserMapping = stripeUserMappingRepository.getStripeUserMapping(customerId);
            if (!optionalUserMapping.isPresent()) {
                LOGGER.warn("[{}] Unable to find customerId->email mapping customerId={}, trying to find user by email", eventType, customerId);
                String customerEmail = getCustomerEmail(event, payload);
                if (customerEmail != null) {
                    Optional<User> optionalUser = userRepository.findByEmail(customerEmail);
                    if (optionalUser.isPresent()) {
                        StripeUserMapping stripeUserMapping = new StripeUserMapping();
                        stripeUserMapping.setEmail(optionalUser.get().getEmail());
                        stripeUserMapping.setStripeCustomerId(customerId);
                        stripeUserMappingRepository.save(stripeUserMapping);
                        optionalUserMapping = Optional.of(stripeUserMapping);

                        LOGGER.info("[{}] Succesfully recovered, email={}, customerId={}", eventType, stripeUserMapping.getEmail(), stripeUserMapping.getStripeCustomerId());
                    }
                }

                if (!optionalUserMapping.isPresent()) {
                    LOGGER.error("Unknown account {}", customerId);
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
                }
            }
            LOGGER.info("[{}] Event email={}", eventType, optionalUserMapping.get().getEmail());
            Optional<User> optionalUser = userRepository.findByEmail(optionalUserMapping.get().getEmail());
            if (!optionalUser.isPresent()) {
                LOGGER.error("[{}] User doesn't exist {}", eventType, optionalUserMapping.get().getEmail());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
            }
            var user = optionalUser.get();
            LOGGER.info("[{}] Event user={}", eventType, user);

            switch (eventType) {
                case "checkout.session.completed": {
                    Session session = ((Session) event.getDataObjectDeserializer().getObject().get());
                    var userMapping = optionalUserMapping.get();
                    userMapping.setCurrentSubscriptionId(session.getSubscription());
                    stripeUserMappingRepository.save(userMapping);
                    LOGGER.info("[{}] Added event subscription={}", eventType, session.getSubscription());

                    break;
                }
                case "customer.subscription.created": {
                    Subscription session = ((Subscription) event.getDataObjectDeserializer().getObject().get());
                    var userMapping = optionalUserMapping.get();
                    userMapping.setCurrentSubscriptionId(session.getId());
                    stripeUserMappingRepository.save(userMapping);
                    LOGGER.info("[{}] Added event subscription={}", eventType, session.getId());

                    break;
                }
                case "customer.created": {
                    Customer customer = ((Customer) event.getDataObjectDeserializer().getObject().get());
                    var userMapping = optionalUserMapping.get();
                    stripeUserMappingRepository.removeAllEntriesWithEmail(userMapping.getEmail()); // when an account is recreated
                    userMapping.setStripeCustomerId(customer.getId());
                    stripeUserMappingRepository.save(userMapping);
                    LOGGER.info("[{}] Customer created with id={}", eventType, customer.getId());

                    break;
                }
                case "customer.subscription.deleted": {
                    var userMapping = optionalUserMapping.get();
                    LOGGER.info("[{}] User '{}' changed from '{}' to FREE account, because subscription was deleted", eventType, user.getEmail(), user.getAccountType());

                    userMapping.setCurrentSubscriptionId(null);
                    stripeUserMappingRepository.save(userMapping);

                    user.setAccountType(AccountType.FREE);
                    userRepository.save(user);

                    break;
                }
                case "invoice.paid": {
                    updateLastPaymentDate(user);

                    Invoice invoice = ((Invoice) event.getDataObjectDeserializer().getObject().get());
                    Long amountPaid = invoice.getAmountPaid();
                    AccountType newAccount = getNewAccountType(invoice, optionalUserMapping.get().getCurrentSubscriptionId());
                    LOGGER.info("[{}] User '{}' paid subscription for {} account", eventType, user.getEmail(), newAccount);

                    if (!user.getAccountType().equals(newAccount)) {
                        LOGGER.info("[{}] Received invoice paid, updating user={}, from={}, to={}", eventType, user.getEmail(), user.getAccountType(), newAccount);
                        user.setAccountType(newAccount);
                        userRepository.save(user);
                    }

                    if (amountPaid == null) {
                        LOGGER.warn("[{}] Amount paid is null", eventType);
                    } else {
                        int amountPaidInDollars = (int) (amountPaid / 100);
                        if (amountPaidInDollars != user.getAccountType().getPrice()) {
                            LOGGER.warn("[{}] User '{}' paid {} but their account ({}) requires {}", eventType, user.getEmail(), amountPaidInDollars, user.getAccountType(),
                                    user.getAccountType().getPrice());
                        }
                    }
                    break;
                }
                case "invoice.payment_failed": {
                    Invoice invoice = ((Invoice) event.getDataObjectDeserializer().getObject().get());
                    LOGGER.error("[{}] Payment failed for user={}, attempts={}", eventType, user, invoice.getAttemptCount());

                    if (sendEmailOnFailure) {
                        String customerPortal = createManagementUrl(customerId);

                        String email = emailTemplateReader.readTemplate("declined-email-template.html", Map.of("STRIPE_CUSTOMER_PORTAL", customerPortal));

                        emailSender.sendEmail(email, "Payment declined for LongTermStockFundamentals subscription", user.getEmail());
                    }
                    break;
                }
                default:
                    LOGGER.info("[{}] Unhandled event received for user={}", eventType, user.getEmail());
            }
        } catch (RuntimeException e) {
            LOGGER.error("[{}] Error while handling event", eventType, e);
            throw e;
        }

        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    public AccountType getNewAccountType(Invoice invoice, String subscriptionId) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            return getAccountTypeFromPayment(subscription.getItems().getData().get(0).getPrice().getId());
        } catch (Exception e) {
            LOGGER.warn("Unable to get item from subscription", e);
            List<InvoiceLineItem> lineItems = invoice.getLines().getData();
            InvoiceLineItem lineItem = lineItems.stream().filter(a -> a.getAmount() >= 0).findFirst().orElse(lineItems.get(0));
            AccountType newAccount = getAccountTypeFromPayment(lineItem.getPrice().getId());
            return newAccount;
        }
    }

    public AccountType getAccountType(String subscriptionId, StripeUserMapping stripeUserMapping) {
        try {
            Subscription subscription = Subscription.retrieve(subscriptionId);
            LOGGER.info("Subscription: " + subscription.getMetadata());
            AccountType newAccount = AccountType.fromString(subscription.getMetadata().get(PLAN_METADATA)); // optionalUserMapping.get().getLastRequestedAccountType();
            return newAccount;
        } catch (Exception e) {
            LOGGER.warn("Unable to read metadata", e);
            AccountType lastAccountType = stripeUserMapping.getLastRequestedAccountType();
            if (lastAccountType != null) {
                return lastAccountType;
            } else {
                LOGGER.error("Unable to determine what user paid subscription for");
                return AccountType.STANDARD;
            }
        }
    }

    private String getCustomerId(Event event, String rawJson) {
        if (event.getDataObjectDeserializer().getObject().isPresent()) {
            StripeObject object = event.getDataObjectDeserializer().getObject().get();
            if (object instanceof Session) {
                return ((Session) object).getCustomer();
            } else if (object instanceof Invoice) {
                return ((Invoice) object).getCustomer();
            } else if (object instanceof Customer) {
                return ((Customer) object).getId();
            } else if (object instanceof PaymentIntent) {
                return ((PaymentIntent) object).getCustomer();
            } else if (object instanceof Subscription) {
                return ((Subscription) object).getCustomer();
            }
        }
        return null;
    }

    private String getCustomerEmail(Event event, String payload) {
        if (event.getDataObjectDeserializer().getObject().isPresent()) {
            StripeObject object = event.getDataObjectDeserializer().getObject().get();
            if (object instanceof Session) {
                return ((Session) object).getCustomerEmail();
            } else if (object instanceof Invoice) {
                return ((Invoice) object).getCustomerEmail();
            } else if (object instanceof Customer) {
                return ((Customer) object).getEmail();
            } else if (object instanceof PaymentIntent) {
                return ((PaymentIntent) object).getCustomerObject().getEmail();
            } else if (object instanceof Subscription) {
                return ((Subscription) object).getCustomerObject().getEmail();
            }
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
            SubscriptionData subscriptionMetadata = SubscriptionData.builder()
                    .putMetadata(PLAN_METADATA, getAccountTypeFromPayment(priceId).toString())
                    .build();
            SessionCreateParams params = new SessionCreateParams.Builder()
                    .setCustomer(customerId)
                    .setSuccessUrl(buildSuccessUrl())
                    .setCancelUrl(buildCancelUrl())
                    .setSubscriptionData(subscriptionMetadata)
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

    private String createCustomer(DecodedJWT jwt) {
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

    @PostMapping("/payment/customerportal")
    public Object redirectToCustomerPortal(HttpServletRequest request, Model model) {
        Optional<DecodedJWT> jwt = loginController.getJwt(request);

        if (!jwt.isPresent()) {
            model.addAttribute("generalMessageTitle", "You are not logged in");
            model.addAttribute("generalMessageBody", "You must be logged in to view customer portal");
            model.addAttribute("generalMessageRedirect", "/");
            return "index";
        }

        Optional<StripeUserMapping> optionalUserMapping = stripeUserMappingRepository.findStripeUserMappingByEmail(jwt.get().getSubject());

        if (!optionalUserMapping.isPresent()) {
            model.addAttribute("generalMessageTitle", "You do not have subscription");
            model.addAttribute("generalMessageBody", "No subscription found for your account");
            model.addAttribute("generalMessageRedirect", "/");
            return "index";
        }

        String url = buildBaseUrl() + "/profile";
        String managementUrl = createManagementUrlWithReturn(optionalUserMapping.get().getStripeCustomerId(), url);

        RedirectView redirectView = new RedirectView(managementUrl);
        redirectView.setStatusCode(HttpStatus.SEE_OTHER);
        return redirectView;
    }

    public String getStripeSecretKey() {
        return stripeSecretKey;
    }

}
