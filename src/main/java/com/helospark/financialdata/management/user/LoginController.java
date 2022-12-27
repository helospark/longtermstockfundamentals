package com.helospark.financialdata.management.user;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.config.JwtService;
import com.helospark.financialdata.management.config.JwtValidatorFilter;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.PersistentSignin;
import com.helospark.financialdata.management.user.repository.PersistentSigninRepository;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
public class LoginController {
    private static final int TWO_MINUTES_IN_MS = 2 * 60 * 1000;
    public static final String REMEMBER_ME_COOKIE_NAME = "remember-me";
    public static final String JWT_COOKIE_NAME = "Authorization";
    private static final int REMEMBER_ME_EXPIRY = 10 * 365 * 24 * 60 * 60;
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginController.class);
    private static final int JWT_EXPIRE_TIME_IN_SECONDS = 2 * 60; // 4 * 60 * 60;
    private SecureRandom secureRandom = new SecureRandom();
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PersistentSigninRepository persistentSigninRepository;
    @Autowired
    BCryptPasswordEncoder passwordEncoder;
    @Autowired
    JwtService jwtService;
    @Value("${website.domain}")
    private String domain;
    @Value("${application.httpsOnly}")
    private boolean isHttpsOnly;

    @PostMapping("/user/login")
    public void login(@RequestBody LoginRequest request, HttpServletResponse httpResponse) {
        Optional<User> optionalUser = userRepository.findByEmail(request.email);

        if (!optionalUser.isPresent()) {
            throw new UserLoginException("User not found");
        }

        User user = optionalUser.get();
        if (!passwordEncoder.matches(request.password, user.getPassword())) {
            throw new UserLoginException("Wrong password for user " + user.getEmail());
        }

        byte[] bytes = new byte[60];
        secureRandom.nextBytes(bytes);
        String persistentToken = new String(Base64.getEncoder().encode(bytes));

        PersistentSignin persistentSignin = new PersistentSignin();
        persistentSignin.setKey(persistentToken);
        persistentSignin.setEmail(user.getEmail());

        persistentSigninRepository.save(persistentSignin);

        addRememberMeCookie(httpResponse, REMEMBER_ME_EXPIRY, persistentToken);
        addAuthorizationCookie(httpResponse, JWT_EXPIRE_TIME_IN_SECONDS, createJWT(user));
    }

    @PostMapping("/user/logout")
    public void logout(HttpServletResponse httpResponse) {
        addRememberMeCookie(httpResponse, 0, "");
        addAuthorizationCookie(httpResponse, 0, "");
    }

    @PostMapping("/user/jwt/refresh")
    public void refreshJwt(HttpServletRequest request, HttpServletResponse httpResponse) {
        Optional<DecodedJWT> currentJwt = getJwt(request);
        boolean renewRequired = true;

        if (currentJwt.isPresent()) {
            long msTillExpiry = currentJwt.get().getExpiresAt().getTime() - new Date().getTime();
            if (msTillExpiry > TWO_MINUTES_IN_MS) {
                httpResponse.addHeader("jwt-expiry", "" + msTillExpiry);
                renewRequired = false;
                LOGGER.info("Renew requested, but no renew needed, since there is still {}ms till expiry", msTillExpiry);
            }
        }

        if (renewRequired) {
            Optional<Cookie> rememberMeCookieOptional = findCookie(request, REMEMBER_ME_COOKIE_NAME);
            if (!rememberMeCookieOptional.isPresent()) {
                throw new JwtRefreshException("Remember-me cookie not found");
            }
            String value = rememberMeCookieOptional.get().getValue();
            if (value == null || value.isBlank()) {
                throw new JwtRefreshException("Empty remember-me cookie");
            }

            Optional<PersistentSignin> previousPersistentSignin = persistentSigninRepository.getPersistentSignin(value);

            if (!previousPersistentSignin.isPresent()) {
                throw new JwtRefreshException("Not currently logged in");
            }

            String email = previousPersistentSignin.get().getEmail();
            createAuthorizationFromEmail(httpResponse, email);

            httpResponse.addHeader("jwt-expiry", "" + ((JWT_EXPIRE_TIME_IN_SECONDS * 1000L)));
        }
    }

    public Cookie createAuthorizationFromEmail(HttpServletResponse httpResponse, String email) {
        Optional<User> optionalUser = userRepository.findByEmail(email);

        if (!optionalUser.isPresent()) {
            throw new JwtRefreshException("User doesn't exist with email=" + email);
        }

        User user = optionalUser.get();
        return addAuthorizationCookie(httpResponse, JWT_EXPIRE_TIME_IN_SECONDS, createJWT(user));
    }

    public Optional<DecodedJWT> getJwt(HttpServletRequest request) {
        return Optional.ofNullable((DecodedJWT) request.getAttribute(JwtValidatorFilter.JWT_ATTRIBUTE));
    }

    @ExceptionHandler(value = UserLoginException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public LoginErrorResponse onLoginError(UserLoginException exception, HttpServletRequest httpRequest) {
        LOGGER.info("Login failed for ip='{}', cause='{}'", httpRequest.getRemoteAddr(), exception.getMessage());
        return new LoginErrorResponse("Bad email or password");
    }

    @ExceptionHandler(value = JwtRefreshException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public LoginErrorResponse onJwtRefreshError(JwtRefreshException exception, HttpServletRequest httpRequest) {
        LOGGER.info("JWT refresh failed for ip='{}', cause='{}'", httpRequest.getRemoteAddr(), exception.getMessage());
        return new LoginErrorResponse("Unable to refresh JWT due to '" + exception.getMessage() + "'");
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public LoginErrorResponse onLoginException(Exception exception, HttpServletRequest httpRequest) {
        LOGGER.error("Unexpected login error for and ip='{}'", httpRequest.getRemoteAddr(), exception);
        return new LoginErrorResponse("Unexpected error during login");
    }

    public void addRememberMeCookie(HttpServletResponse httpResponse, int expiry, String value) {
        Cookie cookie = new Cookie(REMEMBER_ME_COOKIE_NAME, value);
        cookie.setMaxAge(expiry);
        cookie.setHttpOnly(true);
        cookie.setDomain(domain);
        cookie.setPath("/");
        if (isHttpsOnly) {
            cookie.setSecure(true);
        }
        cookie.setAttribute("SameSite", "Lax");
        httpResponse.addCookie(cookie);
    }

    public Cookie addAuthorizationCookie(HttpServletResponse httpResponse, int expiry, String value) {
        Cookie cookie = new Cookie(JWT_COOKIE_NAME, value);
        cookie.setMaxAge(expiry);
        cookie.setHttpOnly(true);
        cookie.setDomain(domain);
        cookie.setPath("/");
        if (isHttpsOnly) {
            cookie.setSecure(true);
        }
        cookie.setAttribute("SameSite", "Lax");
        httpResponse.addCookie(cookie);

        return cookie;
    }

    public Optional<Cookie> findCookie(HttpServletRequest request, String value) {
        if (request.getCookies() == null || request.getCookies().length == 0) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(a -> value.equals(a.getName()))
                .findFirst();
    }

    public String createJWT(User user) {
        Date expiration = new Date();
        expiration.setTime(expiration.getTime() + (JWT_EXPIRE_TIME_IN_SECONDS * 1000L));
        return JWT.create()
                .withIssuer(JwtService.ISSUER)
                .withExpiresAt(expiration)
                .withSubject(user.getEmail())
                .withClaim(JwtService.ACCOUNT_TYPE_CLAIM, user.getAccountType().toString())
                .sign(jwtService.getAlgorithm());
    }

    public AccountType getAccountType(DecodedJWT jwt) {
        return AccountType.fromString(jwt.getClaim(JwtService.ACCOUNT_TYPE_CLAIM).asString());
    }

}
