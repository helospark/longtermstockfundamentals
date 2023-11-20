package com.helospark.financialdata.management.watchlist;

import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.helospark.financialdata.management.user.repository.User;
import com.helospark.financialdata.management.user.repository.UserRepository;
import com.helospark.financialdata.management.watchlist.domain.Portfolio;
import com.helospark.financialdata.management.watchlist.repository.JobLastRunData;
import com.helospark.financialdata.management.watchlist.repository.JobLastRunRepository;
import com.helospark.financialdata.management.watchlist.repository.MessageCompresser;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistory;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistoryElement;
import com.helospark.financialdata.management.watchlist.repository.PortfolioPerformanceHistoryRepository;
import com.helospark.financialdata.management.watchlist.repository.SimpleHolding;
import com.helospark.financialdata.management.watchlist.repository.WatchlistElement;
import com.helospark.financialdata.management.watchlist.repository.WatchlistService;

@Service
public class SavePortfolioHistoryJob {
    private static final String JOB_NAME = "portfolio_history";
    private static final Logger LOGGER = LoggerFactory.getLogger(SavePortfolioHistoryJob.class);

    @Autowired
    private JobLastRunRepository jobLastRunRepository;
    @Autowired
    private PortfolioPerformanceHistoryRepository portfolioPerformanceHistoryRepository;
    @Autowired
    private PortfolioController portfolioController;
    @Autowired
    private WatchlistService watchlistService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MessageCompresser messageCompresser;

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.DAYS)
    public void savePortfolioHistory() {
        LocalDate currentDate = LocalDate.now();
        Optional<JobLastRunData> result = jobLastRunRepository.readJobLastRanByName(JOB_NAME);

        if (result.isEmpty() || jobRanMoreThanAMonthAgo(currentDate, result.get().getDate())) {
            LOGGER.info("Running portfolio performance history job");
            runJob(currentDate);
            jobLastRunRepository.save(new JobLastRunData(JOB_NAME, currentDate));
            LOGGER.info("Finished portfolio performance history job");
        }
    }

    public void runJob(LocalDate currentDate) {
        List<User> users = userRepository.getAllUsers();

        for (var user : users) {
            runJobForUser(user, currentDate);
        }
    }

    public void runJobForUser(User user, LocalDate currentDate) {
        try {
            List<WatchlistElement> watchlistElements = watchlistService.readWatchlistFromDb(user.getEmail())
                    .stream()
                    .filter(element -> element.ownedShares > 0)
                    .collect(Collectors.toList());

            PortfolioPerformanceHistory data = portfolioPerformanceHistoryRepository.readHistoricalPortfolio(user.getEmail()).orElse(createNewPortfolioHistory(user));

            List<PortfolioPerformanceHistoryElement> currentHistory = new ArrayList<>(messageCompresser.uncompressListOf(data.getHistory(), PortfolioPerformanceHistoryElement.class));

            if (watchlistElements.size() > 0) {
                Portfolio result = portfolioController.createSummaryTable(true, watchlistElements);

                PortfolioPerformanceHistoryElement toSave = convert(user, result, watchlistElements, currentDate);

                currentHistory.add(toSave);

                data.setHistory(messageCompresser.createCompressedValue(currentHistory));

                portfolioPerformanceHistoryRepository.save(data);
                LOGGER.info("Performance history data saved for {}", user.getEmail());
            }
        } catch (Exception e) {
            LOGGER.error("Unable to save portfolio performance for {}", user.getEmail(), e);
        }
    }

    public PortfolioPerformanceHistory createNewPortfolioHistory(User user) throws IOException {
        PortfolioPerformanceHistory result = new PortfolioPerformanceHistory();
        result.setEmail(user.getEmail());
        result.setHistory(messageCompresser.compressString("[]".getBytes()));
        return result;
    }

    private PortfolioPerformanceHistoryElement convert(User user, Portfolio portfolio, List<WatchlistElement> watchlistElements, LocalDate currentDate) {
        PortfolioPerformanceHistoryElement result = new PortfolioPerformanceHistoryElement();
        result.setDate(currentDate);
        result.setEmail(user.getEmail());
        result.setEps(portfolio.totalEarnings);
        result.setFcf(portfolio.totalFcf);
        result.setTotal(portfolio.totalPrice);
        result.setTotalEquity(portfolio.totalNetAssets);
        result.setHoldings(convertHoldings(watchlistElements));

        return result;
    }

    private List<SimpleHolding> convertHoldings(List<WatchlistElement> watchlistElements) {
        return watchlistElements.stream()
                .map(a -> new SimpleHolding(a.symbol, a.ownedShares))
                .collect(Collectors.toList());
    }

    private boolean jobRanMoreThanAMonthAgo(LocalDate currentDate, LocalDate lastRanDate) {
        return ChronoUnit.MONTHS.between(lastRanDate, currentDate) >= 1;
    }

}
