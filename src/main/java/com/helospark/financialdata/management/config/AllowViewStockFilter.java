package com.helospark.financialdata.management.config;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.ViewedStocksService;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.FreeStockRepository;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
@Order(3)
public class AllowViewStockFilter implements Filter {
    private static final Pattern FINANCIALS_URI = Pattern.compile("/(.*?)/financials/.*");

    @Autowired
    private LoginController loginController;
    @Autowired
    private SymbolAtGlanceProvider symbolIndexProvider;
    @Autowired
    private ViewedStocksService viewedStocksService;
    @Autowired
    private FreeStockRepository freeStockRepository;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        var httpRequest = ((HttpServletRequest) request);
        var httpResponse = (HttpServletResponse) response;

        Matcher matcher = FINANCIALS_URI.matcher(httpRequest.getRequestURI());

        boolean proceed = true;
        if (matcher.matches()) {
            String stockSymbol = matcher.group(1);

            if (symbolIndexProvider.getCompanyName(stockSymbol).isPresent()) {
                Optional<DecodedJWT> jwtOptional = loginController.getJwt(httpRequest);

                if (jwtOptional.isPresent()) {
                    DecodedJWT jwt = jwtOptional.get();
                    AccountType accountType = loginController.getAccountType(jwt);
                    boolean allowed = viewedStocksService.getAndUpdateAllowViewStocks(jwt.getSubject(), accountType, stockSymbol);

                    if (!allowed) {
                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "{\"error\": \"Account type limit reached\"}");
                        proceed = false;
                    }
                } else {
                    List<String> freeSockList = freeStockRepository.getFreeSockList();
                    if (!freeSockList.contains(stockSymbol)) {
                        httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "{\"error\": \"Free account cannot view this stock\"}");
                        proceed = false;
                    }
                }
            } else {
                httpResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "{\"error\": \"Symbol not found\"}");
                proceed = false;
            }
        }

        if (proceed) {
            chain.doFilter(request, response);
        }
    }

}
