package com.helospark.financialdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.management.inspire.Inspiration;
import com.helospark.financialdata.management.inspire.InspirationClientException;
import com.helospark.financialdata.management.inspire.InspirationProvider;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class InspirationController {
    private static final Logger LOGGER = LoggerFactory.getLogger(InspirationController.class);
    public static final String PORTFOLIO_BASE_PATH = "/inspiration/portfolio/";
    public static final String ALGORITHM_BASE_PATH = "/inspiration/algorithms/";
    @Autowired
    private InspirationProvider inspirationProvider;
    @Autowired
    private LoginController loginController;

    @GetMapping("/inspiration/portfolio/{portfolioName}")
    private Inspiration getPortfolio(@PathVariable("portfolioName") String portfolioName) {
        return inspirationProvider.getPortfolio(portfolioName);
    }

    @GetMapping("/inspiration/algorithms/{portfolioName}")
    private Inspiration getAlgorithm(@PathVariable("portfolioName") String algorithmName, HttpServletRequest request) {
        AccountType type = AccountType.FREE; // or not registered
        var optionalJwt = loginController.getJwt(request);
        if (optionalJwt.isPresent()) {
            type = loginController.getAccountType(optionalJwt.get());
        }
        return inspirationProvider.getAlgorithm(algorithmName, type);
    }

    @ExceptionHandler(InspirationClientException.class)
    public void onClientError(InspirationClientException exception) {
        LOGGER.warn("Client error while getting inspirations");
    }

    @ExceptionHandler(Exception.class)
    public void onOtherError(Exception exception) {
        LOGGER.error("Error while getting inspirations", exception);
    }

}
