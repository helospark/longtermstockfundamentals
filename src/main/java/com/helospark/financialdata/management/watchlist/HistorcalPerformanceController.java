package com.helospark.financialdata.management.watchlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.domain.SimpleDataElement;
import com.helospark.financialdata.management.user.GenericResponseAccountResult;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.watchlist.repository.MessageCompresser;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistory;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistoryElement;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistoryRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/historical-performance")
public class HistorcalPerformanceController {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistorcalPerformanceController.class);

    @Autowired
    private PortfolioPerformanceHistoryRepository portfolioHistoricalRepository;
    @Autowired
    private LoginController loginController;
    @Autowired
    private MessageCompresser messageCompresser;

    @GetMapping("/eps")
    public List<SimpleDataElement> getEps(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getEps());
    }

    @GetMapping("/fcf")
    public List<SimpleDataElement> getFcf(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getFcf());
    }

    @GetMapping("/total")
    public List<SimpleDataElement> getTotal(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getTotal());
    }

    @GetMapping("/equity")
    public List<SimpleDataElement> getEquity(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> element.getTotalEquity());
    }

    @GetMapping("/number-of-holdings")
    public List<SimpleDataElement> getNumberOfHoldings(HttpServletRequest request, HttpServletResponse response) {
        return getSimplePortfolioResult(request, element -> (double) element.getHoldings().size());
    }

    public List<SimpleDataElement> getSimplePortfolioResult(HttpServletRequest request, Function<PortfolioPerformanceHistoryElement, Double> function) {
        Optional<DecodedJWT> user = loginController.getJwt(request);
        if (!user.isPresent()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        List<PortfolioPerformanceHistoryElement> historicalPerformance = getHistoricalPerformance(user);

        List<SimpleDataElement> result = new ArrayList<>();
        for (var element : historicalPerformance) {
            result.add(new SimpleDataElement(element.getDate().toString(), function.apply(element)));
        }

        return result;
    }

    public List<PortfolioPerformanceHistoryElement> getHistoricalPerformance(Optional<DecodedJWT> user) {
        Optional<PortfolioPerformanceHistory> historicalPerformance = portfolioHistoricalRepository.readHistoricalPortfolio(user.get().getSubject());

        if (historicalPerformance.isEmpty()) {
            return List.of();
        }

        ArrayList<PortfolioPerformanceHistoryElement> result = new ArrayList<>(messageCompresser.uncompressListOf(historicalPerformance.get().getHistory(), PortfolioPerformanceHistoryElement.class));

        Collections.sort(result, (a, b) -> a.getDate().compareTo(b.getDate()));

        return result;
    }

    @ExceptionHandler(WatchlistPermissionDeniedException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public GenericResponseAccountResult handlePermissionDenied(WatchlistPermissionDeniedException exception) {
        LOGGER.warn("Unauthorized exception", exception);
        return new GenericResponseAccountResult(exception.getMessage());
    }

}
