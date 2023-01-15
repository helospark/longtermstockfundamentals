package com.helospark.financialdata.management.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.jboss.logging.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.PersistentSignin;
import com.helospark.financialdata.management.user.repository.PersistentSigninRepository;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(1)
public class JwtValidatorFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtValidatorFilter.class);
    public static final String JWT_ATTRIBUTE = "jwt-attribute";
    private static final Set<String> EXCLUDED_URI_PATTERNS = Set.of("/js/.*", "/css/.*", "/images/.*");
    @Autowired
    private JwtService jwtService;
    @Autowired
    private LoginController loginController;
    @Autowired
    private PersistentSigninRepository persistentSigninRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var httpRequest = ((HttpServletRequest) request);
        var httpResponse = (HttpServletResponse) response;

        if (!isExcludedUri(httpRequest)) {
            Optional<Cookie> jwtCookie = loginController.findCookie(httpRequest, LoginController.JWT_COOKIE_NAME);

            boolean loggedIn = false;
            if (jwtCookie.isPresent()) {
                Optional<DecodedJWT> decodedJwt = jwtService.getDecodedJwt(jwtCookie.get().getValue());
                if (!decodedJwt.isPresent()) {
                    MDC.put("email", "-");
                    httpRequest = new CookieRemovingWrapper(httpRequest, Set.of(LoginController.JWT_COOKIE_NAME));
                    loginController.addAuthorizationCookie(httpResponse, 0, "");
                } else {
                    httpRequest.setAttribute(JWT_ATTRIBUTE, decodedJwt.get());
                    MDC.put("email", decodedJwt.get().getSubject());
                    loggedIn = true;
                }
            }
            if (!loggedIn) {
                try {
                    Optional<Cookie> rememberMeCookie = loginController.findCookie(httpRequest, LoginController.REMEMBER_ME_COOKIE_NAME);
                    if (rememberMeCookie.isPresent()) {
                        Optional<PersistentSignin> persistentSignin = persistentSigninRepository.getPersistentSignin(rememberMeCookie.get().getValue());
                        if (persistentSignin.isPresent()) {
                            String user = persistentSignin.get().getEmail();
                            Cookie cookie = loginController.createAuthorizationFromEmail(httpResponse, user);

                            Optional<DecodedJWT> decodedJwt = jwtService.getDecodedJwt(cookie.getValue());

                            if (decodedJwt.isPresent()) {
                                httpRequest.setAttribute(JWT_ATTRIBUTE, decodedJwt.get());
                                httpRequest = new CookieAddingWrapper(httpRequest, List.of(cookie));
                            }
                        } else {
                            LOGGER.warn("Persistent signin cookie present, but data not available in DB");
                        }
                    }
                } catch (Exception e) {
                    LOGGER.error("Cannot run persistent signin", e);
                }
            }
        }

        chain.doFilter(request, response);

    }

    private boolean isExcludedUri(HttpServletRequest httpRequest) {
        String uri = httpRequest.getRequestURI();

        return EXCLUDED_URI_PATTERNS.stream()
                .filter(excludedUri -> uri.matches(excludedUri))
                .findFirst()
                .isPresent();
    }

}
