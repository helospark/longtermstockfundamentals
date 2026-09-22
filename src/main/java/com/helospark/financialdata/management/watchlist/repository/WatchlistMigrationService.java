package com.helospark.financialdata.management.watchlist.repository;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class WatchlistMigrationService {

    private static final Logger log = LoggerFactory.getLogger(WatchlistMigrationService.class);

    private final WatchlistRepository oldRepository;
    private final WatchlistSegmentedRepository newRepository;
    private final MessageCompresser messageCompresser;

    public WatchlistMigrationService(WatchlistRepository oldRepository,
            WatchlistSegmentedRepository newRepository,
            MessageCompresser messageCompresser) {
        this.oldRepository = oldRepository;
        this.newRepository = newRepository;
        this.messageCompresser = messageCompresser;
    }

    public MigrationSummary migrateAllWatchlists() {
        log.info("Starting Watchlist data migration...");

        List<Watchlist> legacyWatchlists = oldRepository.readAllWatchlists();

        int totalUsers = legacyWatchlists.size();
        int totalMigratedElements = 0;
        int failedUsersCount = 0;

        for (Watchlist legacyWatchlist : legacyWatchlists) {
            String email = legacyWatchlist.getEmail();

            if (legacyWatchlist.getWatchlistRaw() == null) {
                log.warn("Skipping user [{}] due to missing/null watchlistRaw payload", email);
                continue;
            }

            try {
                List<WatchlistElement> elements = messageCompresser.uncompressListOf(
                        legacyWatchlist.getWatchlistRaw(),
                        WatchlistElement.class);

                if (elements == null || elements.isEmpty()) {
                    log.info("User [{}] has an empty watchlist, skipping save.", email);
                    continue;
                }

                List<WatchlistElement> preparedElements = new ArrayList<>();
                for (WatchlistElement element : elements) {
                    if (element.symbol != null && !element.symbol.isBlank()) {
                        element.email = email;
                        preparedElements.add(element);
                    } else {
                        log.warn("Found element with missing symbol for user [{}], skipping item", email);
                    }
                }

                for (WatchlistElement element : preparedElements) {
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