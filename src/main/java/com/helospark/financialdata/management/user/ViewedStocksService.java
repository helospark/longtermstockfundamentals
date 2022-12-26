package com.helospark.financialdata.management.user;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.user.repository.FreeStockRepository;
import com.helospark.financialdata.management.user.repository.ViewedStocks;
import com.helospark.financialdata.management.user.repository.ViewedStocksRepository;

@Service
public class ViewedStocksService {
    @Autowired
    private ViewedStocksRepository repository;
    @Autowired
    private FreeStockRepository freeStockRepository;

    Cache<String, Set<String>> viewedStocksCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();

    public boolean getAndUpdateAllowViewStocks(String email, AccountType accountType, String symbol) {
        Set<String> viewedSet = viewedStocksCache.getIfPresent(email);
        if (viewedSet != null && viewedSet.contains(symbol)) {
            return true;
        }
        int allowedStocksPerMonth = accountType.getAllowedStocksPerMonth();

        if (allowedStocksPerMonth == AccountType.UNLIMITED_COUNT) {
            return true;
        }
        if (freeStockRepository.getFreeSockList().contains(symbol)) {
            return true;
        }

        Optional<ViewedStocks> viewedStocksOptional = repository.getViewedStocks(email);
        int currentlyViewedPerMonth = viewedStocksOptional.map(a -> a.getStocks().size()).orElse(0);

        if (allowedStocksPerMonth < (currentlyViewedPerMonth + 1)) {
            return false;
        }

        Set<String> newSet = new HashSet<>();

        if (!viewedStocksOptional.isPresent()) {
            newSet.add(symbol);

            ViewedStocks newViewedStocks = new ViewedStocks();
            newViewedStocks.setEmail(email);
            newViewedStocks.setStocks(newSet);
            repository.save(newViewedStocks);
        } else {
            ViewedStocks viewedStocksToUpdate = viewedStocksOptional.get();

            newSet.addAll(viewedStocksToUpdate.getStocks());
            newSet.add(symbol);

            viewedStocksToUpdate.setStocks(newSet);

            repository.save(viewedStocksToUpdate);
        }
        viewedStocksCache.put(email, newSet);

        return true;
    }

    public int getViewCount(String email) {
        Optional<ViewedStocks> viewedStocksOptional = repository.getViewedStocks(email);
        return viewedStocksOptional.map(a -> a.getStocks().size()).orElse(0);
    }

    public int getAllowedViewCount(AccountType accountType) {
        return accountType.getAllowedStocksPerMonth();
    }
}
