package com.helospark.financialdata.management.user;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.config.ratelimit.RateLimit;
import com.helospark.financialdata.management.payment.PaymentController;
import com.helospark.financialdata.management.payment.PaymentException;
import com.helospark.financialdata.management.payment.repository.StripeUserMapping;
import com.helospark.financialdata.management.payment.repository.StripeUserMappingRepository;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;
import com.helospark.financialdata.management.user.repository.ViewedStocksRepository;
import com.stripe.Stripe;
import com.stripe.model.Subscription;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class UserDeleteController {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserDeleteController.class);
    @Autowired
    private LoginController loginController;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;
    @Autowired
    private PaymentController paymentController;
    @Autowired
    private StripeUserMappingRepository stripeUserMappingRepository;
    @Autowired
    private ViewedStocksRepository viewedStocksRepository;
    @Autowired
    private ScheduledExecutorService scheduledExecutorService;

    @PostMapping("/user/delete")
    @RateLimit(requestPerMinute = 5)
    public DeleteAccountResult registerUser(@RequestBody UserDeleteRequest request, HttpServletRequest servletRequest, HttpServletResponse response, Model model) {
        LOGGER.info("Received account deletion request");
        Optional<DecodedJWT> optionalJwt = loginController.getJwt(servletRequest);

        if (!optionalJwt.isPresent()) {
            throw new AccountDeletionException("Not logged in");
        }

        Optional<User> optionalUser = userRepository.findByEmail(optionalJwt.get().getSubject());

        if (!optionalUser.isPresent()) {
            throw new AccountDeletionException("User already not exists");
        }

        User user = optionalUser.get();

        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new AccountDeletionException("Wrong password provided");
        }

        LOGGER.info("Deleting user {}.", user);

        if (user.getAccountType() != AccountType.FREE && user.getAccountType() != AccountType.ADMIN) {
            LOGGER.info("Cancelling subscription for {}.", user);
            cancelSubscriptionImmediatelyFor(user);
        }

        // Make sure user cannot log in while we schedule deletion
        user.setPassword("");
        userRepository.save(user);

        // Delete later, to avoid exceptions in the async Stripe webhook callbacks.
        scheduledExecutorService.schedule(() -> {
            try {
                viewedStocksRepository.clearViewedStocks(user.getEmail());
                userRepository.delete(user);
            } catch (Exception e) {
                LOGGER.error("Unable to clean user", user);
            }
        }, 120, TimeUnit.SECONDS);

        loginController.addAuthorizationCookie(response, 0, "");
        loginController.addRememberMeCookie(response, 0, "");

        model.addAttribute("generalMessageTitle", "Account removed");
        return new DeleteAccountResult("");
    }

    public void cancelSubscriptionImmediatelyFor(User user) {
        try {
            Stripe.apiKey = paymentController.getStripeSecretKey();

            Optional<StripeUserMapping> optionalUserMapping = stripeUserMappingRepository.findStripeUserMappingByEmail(user.getEmail());

            if (!optionalUserMapping.isPresent()) {
                throw new RuntimeException("User doesn't have a customerId mapping");
            }
            LOGGER.info("Usermapping={}.", optionalUserMapping.get());

            Subscription subscription = Subscription.retrieve(optionalUserMapping.get().getCurrentSubscriptionId());

            if (subscription.getStatus().equals("canceled") || subscription.getStatus().equals("incomplete_expired")) {
                LOGGER.warn("Wanted to cancel subscription, but it was already canceled, but it's not consistent with DB");
            } else {
                Subscription result = subscription.cancel();

                LOGGER.info("Subscription status={}.", result.getStatus());
            }
        } catch (Exception e) {
            throw new PaymentException("Unable to cancel subscription", e);
        }
    }

    @ExceptionHandler(value = { AccountDeletionException.class })
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public DeleteAccountResult exceptionClientHandler(AccountDeletionException exception, Model model) {
        LOGGER.error("Error while removing account", exception);

        return new DeleteAccountResult(exception.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public DeleteAccountResult exceptionServerHandler(Exception exception) {
        LOGGER.error("Exception during registration", exception);
        return new DeleteAccountResult("Unexpected error while removing account");
    }
}
