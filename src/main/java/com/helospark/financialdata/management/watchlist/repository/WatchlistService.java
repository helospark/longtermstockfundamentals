package com.helospark.financialdata.management.watchlist.repository;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.watchlist.DeleteFromWatchlistRequest;
import com.helospark.financialdata.management.watchlist.WatchlistBadRequestException;
import com.helospark.financialdata.management.watchlist.domain.AddToWatchlistRequest;
import com.helospark.financialdata.management.watchlist.domain.CalculatorParameters;
import com.helospark.financialdata.management.watchlist.domain.WatchListResponse;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

@Service
public class WatchlistService {
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchlistService.class);
    private static final String NOTES_COL = "Notes";
    private static final String DIFFERENCE_COL = "Margin of safety";
    private static final String PRICE_TARGET_COL = "Price target";
    private static final String CURRENT_PRICE_COL = "Current price";
    private static final String NAME_COL = "Name";
    private static final String SYMBOL_COL = "Symbol";
    private static final String OWNED_SHARES = "Owned ($)";
    private static final String TAGS_COL = "Tags";

    private static final List<String> COLOR_CODES = List.of(
            "darkred", "green", "blue", "purple", "grey", "coral", "deeppink", "maroon", "sienna", "darkgoldenrod", "darkcyan", "darkmagenta", "darkviolet");

    @Autowired
    private WatchlistRepository watchlistRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private SymbolAtGlanceProvider symbolIndexProvider;
    @Autowired
    private LatestPriceProvider latestPriceProvider;

    // Only for single server setup
    Cache<String, Optional<Watchlist>> watchlistCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(1000)
            .build();

    public WatchListResponse getWatchlist(String email, boolean onlyOwned) {

        WatchListResponse result = new WatchListResponse();
        result.columns = List.of(SYMBOL_COL, NAME_COL, CURRENT_PRICE_COL, PRICE_TARGET_COL, DIFFERENCE_COL, OWNED_SHARES, TAGS_COL, NOTES_COL);
        result.portfolio = new ArrayList<>();

        List<WatchlistElement> watchlistElements = readWatchlistFromDb(email);

        Map<String, CompletableFuture<Double>> prices = new HashMap<>();
        for (int i = 0; i < watchlistElements.size(); ++i) {
            String symbol = watchlistElements.get(i).symbol;
            prices.put(symbol, latestPriceProvider.provideLatestPriceAsync(symbol));
        }

        for (int i = 0; i < watchlistElements.size(); ++i) {
            WatchlistElement currentElement = watchlistElements.get(i);
            String ticker = currentElement.symbol;
            Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(ticker);
            if (symbolIndexProvider.doesCompanyExists(ticker) && optionalAtGlance.isPresent() && (onlyOwned == false || currentElement.ownedShares > 0)) {
                var atGlance = optionalAtGlance.get();
                double latestPriceInTradingCurrency = getPrice(prices, ticker);
                Map<String, String> portfolioElement = new HashMap<>();
                portfolioElement.put(SYMBOL_COL, ticker);
                portfolioElement.put(NAME_COL, Optional.ofNullable(atGlance.companyName).orElse(""));
                portfolioElement.put(CURRENT_PRICE_COL, formatString(latestPriceInTradingCurrency));
                portfolioElement.put(PRICE_TARGET_COL, formatString(currentElement.targetPrice));
                portfolioElement.put(DIFFERENCE_COL, formatStringAsPercent(calculateTargetPercent(latestPriceInTradingCurrency, currentElement.targetPrice)));
                portfolioElement.put(OWNED_SHARES, formatString(atGlance.latestStockPriceUsd * currentElement.ownedShares));
                portfolioElement.put(TAGS_COL, formatTags(currentElement.tags));
                portfolioElement.put(NOTES_COL, currentElement.notes);

                if (currentElement.calculatorParameters != null) {
                    portfolioElement.put("CALCULATOR_URI", buildCalculatorUri(currentElement.calculatorParameters, ticker));
                }

                result.portfolio.add(portfolioElement);
            }
        }

        return result;
    }

    public List<WatchlistElement> readWatchlistFromDb(String email) {
        Optional<Watchlist> optionalWatchlist = loadWatchlist(email);
        if (!optionalWatchlist.isPresent()) {
            return List.of();
        }

        Watchlist watchlist = optionalWatchlist.get();

        List<WatchlistElement> watchlistElements = decodeWatchlist(watchlist);
        return watchlistElements;
    }

    public Double getPrice(Map<String, CompletableFuture<Double>> prices, String ticker) {
        try {
            return prices.get(ticker).get();
        } catch (Exception e) {
            return latestPriceProvider.provideLatestPrice(ticker);
        }
    }

    public @PolyNull Optional<Watchlist> loadWatchlist(String email) {
        return watchlistCache.get(email, email2 -> watchlistRepository.readWatchlistByEmail(email2));
    }

    public String buildCalculatorUri(CalculatorParameters calculatorParameters, String symbol) {
        String uri = "/calculator/" + symbol;
        uri += "?startMargin=" + calculatorParameters.startMargin + "&endMargin=" + calculatorParameters.endMargin;
        uri += "&startGrowth=" + calculatorParameters.startGrowth + "&endGrowth=" + calculatorParameters.endGrowth;
        uri += "&startShareChange=" + calculatorParameters.startShChange + "&endShareChange=" + calculatorParameters.endShChange;
        uri += "&discount=" + calculatorParameters.discount + "&endMultiple=" + calculatorParameters.endMultiple;

        return uri;
    }

    private String formatTags(List<String> tags) {
        String result = "";
        for (var tag : tags) {
            int colorIndex = Math.abs(tag.hashCode()) % COLOR_CODES.size();
            String colorToUse = COLOR_CODES.get(colorIndex);
            result += "<span class=\"badge badge-pill\" style=\"background-color: " + colorToUse + "\">" + tag + "</span>";
        }
        return result;
    }

    private String formatStringAsPercent(Double value) {
        if (value == null) {
            return "-";
        } else {
            if (value < 0) {
                return String.format("<div style=\"color:red;  font-weight: 600;\">%.2f %%</div>", value);
            } else {
                return String.format("<div style=\"color:green; font-weight: 600;\">%.2f %%</div>", value);
            }
        }
    }

    private Double calculateTargetPercent(double latestStockPrice, Double targetPrice) {
        if (targetPrice == null) {
            return null;
        }
        return (targetPrice / latestStockPrice - 1.0) * 100.0;
    }

    public String formatString(Double value) {
        if (value == null) {
            return "-";
        } else {
            return String.format(Locale.US, "%,.2f", value);
        }
    }

    public List<WatchlistElement> decodeWatchlist(Watchlist watchlist) {
        try {
            String rawString = MessageCompresser.uncompressString(watchlist.getWatchlistRaw());

            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, WatchlistElement.class);
            List<WatchlistElement> result = objectMapper.readValue(rawString, type);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void saveToWatchlist(String email, AddToWatchlistRequest request, AccountType accountType) {
        Optional<Watchlist> optionalWatchlist = watchlistRepository.readWatchlistByEmail(email);

        List<WatchlistElement> elements;

        if (!optionalWatchlist.isPresent()) {
            elements = new ArrayList<>();
        } else {
            elements = decodeWatchlist(optionalWatchlist.get());
        }

        int index = findIndexFor(elements, request.symbol);

        WatchlistElement elementToUpdate;
        if (index == -1) {
            if (elements.size() > 200) {
                throw new WatchlistBadRequestException("Maximum of 200 watchlist element supported");
            }
            if (elements.size() > 30 && accountType.equals(AccountType.FREE)) {
                throw new WatchlistBadRequestException("Maximum of 30 watchlist element supported for free subscription");
            }

            elementToUpdate = new WatchlistElement();
            elements.add(elementToUpdate);
        } else {
            elementToUpdate = elements.get(index);
        }
        elementToUpdate.notes = escapeSymbols(request.notes);
        elementToUpdate.symbol = request.symbol;
        elementToUpdate.tags = escapeSymbols(stripTags(request.tags));
        elementToUpdate.targetPrice = request.priceTarget;
        elementToUpdate.ownedShares = request.ownedShares;
        if (request.calculatorParameters != null) {
            elementToUpdate.calculatorParameters = request.calculatorParameters;
        }

        Watchlist toInsert = new Watchlist();
        toInsert.setEmail(email);
        toInsert.setWatchlistRaw(createCompressedValue(elements));

        LOGGER.info("Inserting into watchlist, size of compressed elements={}", toInsert.getWatchlistRaw().capacity());

        watchlistRepository.save(toInsert);
        watchlistCache.invalidate(email);
    }

    private List<String> stripTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }
        return tags.stream()
                .map(tag -> tag.strip())
                .collect(Collectors.toList());
    }

    private ByteBuffer createCompressedValue(List<WatchlistElement> elements) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(elements);
            return MessageCompresser.compressString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<String> escapeSymbols(List<String> tags) {
        return tags.stream()
                .map(a -> StringEscapeUtils.escapeHtml4(a))
                .collect(Collectors.toList());
    }

    private String escapeSymbols(String text) {
        return StringEscapeUtils.escapeHtml4(text);
    }

    private int findIndexFor(List<WatchlistElement> elements, String symbol) {
        for (int i = 0; i < elements.size(); ++i) {
            if (elements.get(i).symbol.equals(symbol)) {
                return i;
            }
        }
        return -1;
    }

    public void deleteFromWatchlist(String email, DeleteFromWatchlistRequest request) {
        Optional<Watchlist> optionalWatchlist = watchlistRepository.readWatchlistByEmail(email);

        if (!optionalWatchlist.isPresent()) {
            return;
        }
        List<WatchlistElement> elements = decodeWatchlist(optionalWatchlist.get());

        int index = findIndexFor(elements, request.symbol);

        if (index != -1) {
            elements.remove(index);
        }

        Watchlist toInsert = new Watchlist();
        toInsert.setEmail(email);
        toInsert.setWatchlistRaw(createCompressedValue(elements));

        LOGGER.info("Remove successful, size of compressed elements={}", toInsert.getWatchlistRaw().capacity());

        watchlistRepository.save(toInsert);
        watchlistCache.invalidate(email);
    }

    public Optional<WatchlistElement> getWatchlistElement(String email, String stock) {
        Optional<Watchlist> optionalWatchlist = loadWatchlist(email);

        if (!optionalWatchlist.isPresent()) {
            return Optional.empty();
        }
        List<WatchlistElement> elements = decodeWatchlist(optionalWatchlist.get());

        int index = findIndexFor(elements, stock);

        if (index != -1) {
            return Optional.of(elements.get(index));
        } else {
            return Optional.empty();
        }
    }

}
