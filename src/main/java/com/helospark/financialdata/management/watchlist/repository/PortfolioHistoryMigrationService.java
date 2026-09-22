package com.helospark.financialdata.management.watchlist.repository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PortfolioHistoryMigrationService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioHistoryMigrationService.class);

    private final PortfolioPerformanceHistoryRepository oldRepository;
    private final PortfolioPerformanceHistorySegmentedRepository newRepository;
    private final MessageCompresser messageCompresser;

    public PortfolioHistoryMigrationService(PortfolioPerformanceHistoryRepository oldRepository,
            PortfolioPerformanceHistorySegmentedRepository newRepository,
            MessageCompresser messageCompresser) {
        this.oldRepository = oldRepository;
        this.newRepository = newRepository;
        this.messageCompresser = messageCompresser;
    }

    public MigrationSummary migrateAllHistory() {
        log.info("Starting Watchlist data migration...");

        List<PortfolioPerformanceHistory> legacyHistory = oldRepository.readAllHistory();

        int totalUsers = legacyHistory.size();
        int totalMigratedElements = 0;
        int failedUsersCount = 0;

        for (PortfolioPerformanceHistory legacyWatchlist : legacyHistory) {
            String email = legacyWatchlist.getEmail();

            if (legacyWatchlist.getHistory() == null) {
                log.warn("Skipping user [{}] due to missing/null watchlistRaw payload", email);
                continue;
            }

            try {
                List<PortfolioPerformanceHistoryElement> elements = messageCompresser.uncompressListOf(
                        legacyWatchlist.getHistory(),
                        PortfolioPerformanceHistoryElement.class);

                if (elements == null || elements.isEmpty()) {
                    log.info("User [{}] has an empty watchlist, skipping save.", email);
                    continue;
                }

                List<PortfolioPerformanceHistoryElement> preparedElements = new ArrayList<>();
                for (PortfolioPerformanceHistoryElement element : elements) {
                    element.email = email;
                    preparedElements.add(element);
                }

                for (PortfolioPerformanceHistoryElement element : preparedElements) {
                    newRepository.save(element);
                    totalMigratedElements++;
                }

                log.info("Successfully migrated {} elements for user [{}]", preparedElements.size(), email);

            } catch (Exception e) {
                failedUsersCount++;
                log.error("Failed to migrate watchlist for user [{}]", email, e);
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