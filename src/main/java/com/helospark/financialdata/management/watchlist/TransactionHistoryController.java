package com.helospark.financialdata.management.watchlist;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.watchlist.repository.PortfolioTransaction;
import com.helospark.financialdata.management.watchlist.repository.PortfolioTransactionRepository;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class TransactionHistoryController {
    @Autowired
    private LoginController loginController;
    @Autowired
    private PortfolioTransactionRepository portfolioTransactionRepository;

    @GetMapping("/portfolio-transactions")
    public List<PortfolioTransaction> getTransactions(HttpServletRequest httpRequest) {
        User user = loginController.findUserOrThrow(httpRequest);
        return portfolioTransactionRepository.getTransactionsByUserEmail(user.getEmail());
    }
}
