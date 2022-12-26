package com.helospark.financialdata.management.config;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CookieRemovingWrapper extends HttpServletRequestWrapper {
    private Cookie[] newCookies;

    public CookieRemovingWrapper(HttpServletRequest httpRequest, Set<String> toRemove) {
        super(httpRequest);
        var oldCookies = httpRequest.getCookies();
        if (oldCookies == null || oldCookies.length == 0) {
            newCookies = new Cookie[0];
        } else {
            this.newCookies = Arrays.stream(httpRequest.getCookies())
                    .filter(a -> !toRemove.contains(a.getName()))
                    .collect(Collectors.toList())
                    .toArray(new Cookie[0]);
        }
    }

    @Override
    public Cookie[] getCookies() {
        return newCookies;
    }
}
