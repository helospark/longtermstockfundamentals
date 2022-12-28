package com.helospark.financialdata.management.config;

import java.util.List;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

public class CookieAddingWrapper extends HttpServletRequestWrapper {
    private Cookie[] newCookies;

    public CookieAddingWrapper(HttpServletRequest httpRequest, List<Cookie> toAdd) {
        super(httpRequest);

        var oldCookies = httpRequest.getCookies();
        int newSize = (oldCookies == null) ? toAdd.size() : oldCookies.length + toAdd.size();

        newCookies = new Cookie[newSize];

        if (oldCookies != null) {
            for (int i = 0; i < oldCookies.length; ++i) {
                newCookies[i] = oldCookies[i];
            }
        }
        for (int i = 0; i < toAdd.size(); ++i) {
            newCookies[oldCookies.length + i] = toAdd.get(i);
        }
    }

    @Override
    public Cookie[] getCookies() {
        return newCookies;
    }
}
