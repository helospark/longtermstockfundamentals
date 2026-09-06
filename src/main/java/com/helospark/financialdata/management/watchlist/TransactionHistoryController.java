package com.helospark.financialdata.management.watchlist;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/portfolio-transactions/{ticker}")
    public List<PortfolioTransaction> getTransactions(HttpServletRequest httpRequest, @PathVariable("ticker") String ticker) {
        User user = loginController.findUserOrThrow(httpRequest);
        List<PortfolioTransaction> result = portfolioTransactionRepository.getTransactionsByUserEmail(user.getEmail())
                .stream()
                .filter(a -> a.getSymbol().equals(ticker))
                .collect(Collectors.toList());

        //        var result2 = new ArrayList<>(result);
        //        result2.add(new PortfolioTransaction(user.getEmail(), "2021-01-02", "ADBE", 2.0, 64.0, 123.0, 123.0, "USD"));
        //        result2.add(new PortfolioTransaction(user.getEmail(), "2022-01-02", "ADBE", -2.0, 111.0, 111.0, 111.0, "USD"));

        return result;
    }
}
