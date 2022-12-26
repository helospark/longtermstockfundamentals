package com.helospark.financialdata.management.config;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.user.LoginController;

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
    @Autowired
    private JwtService jwtService;
    @Autowired
    private LoginController loginController;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var httpRequest = ((HttpServletRequest) request);
        var httpResponse = (HttpServletResponse) response;

        Optional<Cookie> jwtCookie = loginController.findCookie(httpRequest, LoginController.JWT_COOKIE_NAME);

        if (jwtCookie.isPresent()) {
            if (!jwtService.validateJwt(jwtCookie.get().getValue())) {
                request = new CookieRemovingWrapper(httpRequest, Set.of(LoginController.JWT_COOKIE_NAME));
                loginController.createAuthorizationCookie(httpResponse, 0, "");
            }
        }

        chain.doFilter(request, response);
    }

}
