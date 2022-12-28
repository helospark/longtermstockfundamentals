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
        for (var entry : accountToPriceMapString.entrySet()) {
            accountToPriceMap.put(AccountType.fromString(entry.getKey()), entry.getValue());
        }
    }

    @PostMapping("/payment/initialize")
    public Object onPayment(@RequestParam(PLAN_METADATA) String plan, HttpServletRequest servletRequest, Model model) {
        Stripe.apiKey = stripeSecretKey;
        Optional<DecodedJWT> optionalJwt = loginController.getJwt(servletRequest);

        if (!optionalJwt.isPresent()) {
            model.addAttribute("generalMessageTitle", "Not logged in.");
            model.addAttribute("generalMessageBody", "In order to subscribe to this plan, you have to log in.");
            model.addAttribute("generalMessageRedirect", "/");
            return "index";
        } else {
            DecodedJWT jwt = optionalJwt.get();

            Optional<StripeUserMapping> userToStripeOptional = stripeUserMappingRepository.findStripeUserMappingByEmail(jwt.getSubject());

            StripeUserMapping stripeUserMapping;
            AccountType accountType = getAccountTypeFromPayment(plan);
            if (userToStripeOptional.isEmpty()) {
                String customerId = createUser(jwt);
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
                    model.addAttribute("generalMessageBody", "Succesfully updated to FREE plan.");
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
                        .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                        .setDescription("Cancelling subscription")
                        .build();

                subscription.update(params);
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
                model.addAttribute("generalMessageTitle", "Subscription is not yet updated");
                model.addAttribute("generalMessageBody",
                        "Status of Stripe payment is " + updatedSubscription.getStatus()
                                + ", this could be due to wrong payment method or additional authentication needed.<br/>"
                                + "Please finish payement here: <a href=\"" + managementUrl + "\">" + managementUrl
                                + "</a>");
                model.addAttribute("generalMessageRedirect", "/");
                model.addAttribute("generalMessageRefreshJwt", true);
            }

        } catch (Exception e) {
            throw new PaymentException("Unable to update subscription", e);
        }
    }

    private String createManagementUrl(String customerId) {
        try {
            com.stripe.param.billingportal.SessionCreateParams params = new com.stripe.param.billingportal.SessionCreateParams.Builder()
                    .setReturnUrl(buildBaseUrl())
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
        LOGGER.info("Webhook payment event received: payload='{}', signature='{}'", payload, sigHeader);
        Event event = null;

        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeWebhookSecret);
        } catch (SignatureVerificationException e) {
            LOGGER.error("Invalid stripe signature", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("");
        }

        String customerId = getCustomerId(event, payload);

        if (customerId == null) {
            LOGGER.info("Skipping event '{}', because it doesn't have any customerId", event.getType());
            return ResponseEntity.status(HttpStatus.OK).body("");
        }

        Optional<StripeUserMapping> optionalUserMapping = stripeUserMappingRepository.getStripeUserMapping(customerId);
        if (!optionalUserMapping.isPresent()) {
            LOGGER.error("Unknown account {}", customerId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
        Optional<User> optionalUser = userRepository.findByEmail(optionalUserMapping.get().getEmail());
        if (!optionalUser.isPresent()) {
            LOGGER.error("User doesn't exist {}", optionalUserMapping.get().getEmail());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
        var user = optionalUser.get();

        switch (event.getType()) {
            case "checkout.session.completed": {
                Session session = ((Session) event.getDataObjectDeserializer().getObject().get());
                AccountType newAccount = getAccountType(session.getSubscription(), optionalUserMapping.get());
                LOGGER.info("User '{}' changed to {} account from {}", user.getEmail(), newAccount, user.getAccountType());

                var userMapping = optionalUserMapping.get();
                userMapping.setCurrentSubscriptionId(session.getSubscription());
                stripeUserMappingRepository.save(userMapping);

                //                user.setAccountType(newAccount); // wait until invoice.paid
                //                userRepository.save(user);

                updateLastPaymentDate(user);

                break;
            }
            case "customer.subscription.deleted": {
                LOGGER.info("User '{}' changed to FREE account, because subscription was deleted", user.getEmail());
                var userMapping = optionalUserMapping.get();
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
                LOGGER.info("User '{}' paid subscription for {} account", user.getEmail(), newAccount);

                if (!user.getAccountType().equals(newAccount)) {
                    LOGGER.info("Received invoice paid, updating user={}, from={}, to={}", user.getEmail(), user.getAccountType(), newAccount);
                    user.setAccountType(newAccount);
                    userRepository.save(user);
                }

                if (amountPaid == null) {
                    LOGGER.warn("Amount paid is null");
                } else {
                    int amountPaidInDollars = (int) (amountPaid / 100);
                    if (amountPaidInDollars != user.getAccountType().getPrice()) {
                        LOGGER.warn("User '{}' paid {} but their account ({}) requires {}", user.getEmail(), amountPaidInDollars, user.getAccountType(), user.getAccountType().getPrice());
                    }
                }
                break;
            }
            case "invoice.payment_failed": {
                Invoice invoice = ((Invoice) event.getDataObjectDeserializer().getObject().get());
                LOGGER.error("Payment failed attempts={}", invoice.getAttemptCount());
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
            return stripeUserMapping.getLastRequestedAccountType();
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
