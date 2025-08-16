package com.helospark.financialdata.management.watchlist;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.helospark.financialdata.management.watchlist.repository.Watchlist;
import com.helospark.financialdata.management.watchlist.repository.WatchlistRepository;

@Component
public class WatchlistAlertJob {
    @Autowired
    WatchlistRepository watchlistRepository;

    public void runWatchlistAlertJobForTicker(String ticker) {
        List<Watchlist> allWatchlists = watchlistRepository.readAllWatchlists();

        // TODO: update alerts
    }

}
