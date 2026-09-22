package com.helospark.financialdata.management.watchlist.repository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WatchlistExpectationMigrationService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistExpectationMigrationService.class);

    private final WatchlistExpectationHistoryRepository oldRepository;
    private final WatchlistExpectationSegmentedHistoryRepository newRepository;
    private final MessageCompresser messageCompresser;

    public WatchlistExpectationMigrationService(WatchlistExpectationHistoryRepository oldRepository,
            WatchlistExpectationSegmentedHistoryRepository newRepository,
            MessageCompresser messageCompresser) {
        this.oldRepository = oldRepository;
        this.newRepository = newRepository;
        this.messageCompresser = messageCompresser;
    }

    public MigrationSummary migrateAllExpectations() {
        log.info("Starting Watchlist data migration...");

        List<WatchlistExpectationHistory> legacyWatchlists = oldRepository.readAllExpectations();

        int totalUsers = legacyWatchlists.size();
        int totalMigratedElements = 0;
        int failedUsersCount = 0;

        for (WatchlistExpectationHistory legacyWatchlist : legacyWatchlists) {
            String emailSymbol = legacyWatchlist.getEmailSymbol();

            if (legacyWatchlist.getWatchlistExpectationListRaw() == null) {
                log.warn("Skipping user [{}] due to missing/null watchlistRaw payload", emailSymbol);
                continue;
            }

            try {
                List<WatchlistExpectationHistoryElement> elements = messageCompresser.uncompressListOf(
                        legacyWatchlist.getWatchlistExpectationListRaw(),
                        WatchlistExpectationHistoryElement.class);

                if (elements == null || elements.isEmpty()) {
                    log.info("User [{}] has an empty watchlist, skipping save.", emailSymbol);
                    continue;
                }

                List<WatchlistExpectationHistoryElement> preparedElements = new ArrayList<>();
                for (WatchlistExpectationHistoryElement element : elements) {
                    if (element.symbol != null && !element.symbol.isBlank()) {
                        element.emailSymbolKey = emailSymbol;
                        preparedElements.add(element);
                    } else {
                        log.warn("Found element with missing symbol for user [{}], skipping item", emailSymbol);
                    }
                }

                for (WatchlistExpectationHistoryElement element : preparedElements) {
                    newRepository.save(element);
                    totalMigratedElements++;
                }

                log.info("Successfully migrated {} elements for user [{}]", preparedElements.size(), emailSymbol);

            } catch (Exception e) {
                failedUsersCount++;
                log.error("Failed to migrate watchlist for user [{}]", emailSymbol, e);
            }
        }

        log.info("Migration finished. Total users processed: {}, Total elements migrated: {}, Failed users: {}",
                totalUsers, totalMigratedElements, failedUsersCount);

        return new MigrationSummary(totalUsers, totalMigratedElements, failedUsersCount);
    }

    // Simple DTO for reporting migration outcome
    public record MigrationSummary(int totalUsersProcessed, int totalElementsMigrated, int failedUsers) {
    }
}