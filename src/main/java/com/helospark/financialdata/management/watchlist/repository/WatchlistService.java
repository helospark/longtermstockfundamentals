package com.helospark.financialdata.management.watchlist.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.checkerframework.checker.nullness.qual.PolyNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.watchlist.DeleteFromWatchlistRequest;
import com.helospark.financialdata.management.watchlist.WatchlistBadRequestException;
import com.helospark.financialdata.management.watchlist.domain.AddToWatchlistRequest;
import com.helospark.financialdata.management.watchlist.domain.CalculatorParameters;
import com.helospark.financialdata.management.watchlist.domain.PaginatedWatchListResponse;
import com.helospark.financialdata.management.watchlist.domain.datatables.DataTableRequest;
import com.helospark.financialdata.management.watchlist.domain.datatables.Order;
import com.helospark.financialdata.service.DataLoader;
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
    @Autowired
    private MessageCompresser messageCompresser;

    // Only for single server setup
    Cache<String, Optional<Watchlist>> watchlistCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.DAYS)
            .maximumSize(1000)
            .build();

    public PaginatedWatchListResponse getWatchlistColumns() {
        PaginatedWatchListResponse result = new PaginatedWatchListResponse();
        result.columns = List.of(SYMBOL_COL, NAME_COL, CURRENT_PRICE_COL, PRICE_TARGET_COL, DIFFERENCE_COL, OWNED_SHARES, TAGS_COL, NOTES_COL);

        return result;
    }

    public PaginatedWatchListResponse getWatchlistPaginated(String email, int start, int length, Optional<Integer> draw, DataTableRequest dataTableRequest) {

        PaginatedWatchListResponse result = getWatchlistColumns();

        List<WatchlistElement> watchlistElements = readWatchlistFromDb(email);
        result.recordsTotal = watchlistElements.size();

        if (dataTableRequest.search.value != null) {
            watchlistElements = watchlistElements.stream()
                    .filter(a -> matchesSearch(a, dataTableRequest.search.value))
                    .collect(Collectors.toList());
        }

        result.recordsFiltered = watchlistElements.size();

        Collections.reverse(watchlistElements);

        if (dataTableRequest.order.size() > 0) {
            Order order = dataTableRequest.order.get(0);

            Collections.sort(watchlistElements, (a, b) -> {
                Comparable first = getDataAtColumn(a, order.column, result.columns);
                Comparable second = getDataAtColumn(b, order.column, result.columns);
                if (order.dir.equals("asc")) {
                    return ObjectUtils.compare(first, second);
                } else {
                    return ObjectUtils.compare(second, first);
                }
            });
        }

        Map<String, CompletableFuture<Double>> prices = new HashMap<>();
        for (int i = start; i < watchlistElements.size() && i < start + length; ++i) {
            String symbol = watchlistElements.get(i).symbol;
            prices.put(symbol, latestPriceProvider.provideLatestPriceAsync(symbol));
        }

        for (int i = start; i < watchlistElements.size() && i < start + length; ++i) {
            WatchlistElement currentElement = watchlistElements.get(i);
            String ticker = currentElement.symbol;
            Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(ticker);
            if (symbolIndexProvider.doesCompanyExists(ticker) && optionalAtGlance.isPresent()) {
                var atGlance = optionalAtGlance.get();
                CompanyFinancials company = DataLoader.readFinancials(ticker);
                double latestPriceInTradingCurrency = getPrice(prices, ticker, company.profile.currency);
                List<String> portfolioElement = new ArrayList<>();
                portfolioElement.add(ticker);
                portfolioElement.add(Optional.ofNullable(atGlance.companyName).orElse(""));
                portfolioElement.add(formatString(latestPriceInTradingCurrency));
                portfolioElement.add(formatString(currentElement.targetPrice));
                portfolioElement.add(formatStringAsPercent(calculateTargetPercent(latestPriceInTradingCurrency, currentElement.targetPrice)));
                portfolioElement.add(formatString(atGlance.latestStockPriceUsd * currentElement.ownedShares));
                portfolioElement.add(formatTags(currentElement.tags));
                portfolioElement.add(currentElement.notes);

                if (currentElement.calculatorParameters != null) {
                    portfolioElement.add(buildCalculatorUri(currentElement.calculatorParameters, ticker));
                }

                result.data.add(portfolioElement);
            }
        }
        result.draw = draw.orElse(0);

        return result;
    }

    private boolean matchesSearch(WatchlistElement a, String value) {
        value = value.toUpperCase();

        if (a.notes != null && a.notes.toUpperCase().contains(value)) {
            return true;
        }
        if (a.tags != null) {
            for (var tag : a.tags) {
                if (tag.toUpperCase().contains(value)) {
                    return true;
                }
            }
        }
        if (a.symbol.toUpperCase().contains(value)) {
            return true;
        }
        Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(a.symbol);
        if (optionalAtGlance.isPresent()) {
            AtGlanceData glance = optionalAtGlance.get();

            if (glance.companyName != null && glance.companyName.toUpperCase().contains(value)) {
                return true;
            }
        }
        return false;
    }

    private Comparable getDataAtColumn(WatchlistElement a, int column, List<String> columns) {
        Map<String, Comparable> datas = new HashMap<>();
        Optional<AtGlanceData> optionalAtGlance = symbolIndexProvider.getAtGlanceData(a.symbol);

        if (optionalAtGlance.isEmpty()) {
            return null;
        }
        if (column >= columns.size()) {
            return null;
        }
        String columnKey = columns.get(column);
        var glance = optionalAtGlance.get();

        switch (columnKey) {
            case SYMBOL_COL:
                return a.symbol;
            case NAME_COL:
                return glance.companyName;
            case CURRENT_PRICE_COL:
                return glance.latestStockPriceTradingCur;
            case PRICE_TARGET_COL:
                return a.targetPrice;
            case DIFFERENCE_COL:
                return calculateTargetPercent(glance.latestStockPriceTradingCur, a.targetPrice);
            case OWNED_SHARES:
                return glance.latestStockPriceUsd * a.ownedShares;
            case TAGS_COL:
                return ((a.tags != null && a.tags.size() > 0) ? a.tags.get(0) : "");
            case NOTES_COL:
                return a.notes;
            default:
                return null;
        }
    }

    public List<WatchlistElement> readWatchlistFromDb(String email) {
        Optional<Watchlist> optionalWatchlist = loadWatchlist(email);
        if (!optionalWatchlist.isPresent()) {
            return List.of();
        }

        Watchlist watchlist = optionalWatchlist.get();

        List<WatchlistElement> watchlistElements = messageCompresser.uncompressListOf(watchlist.getWatchlistRaw(), WatchlistElement.class);
        return new ArrayList<>(watchlistElements);
    }

    public double getPrice(Map<String, CompletableFuture<Double>> prices, String ticker, String currency) {
        double result;
        try {
            result = prices.get(ticker).get();
        } catch (Exception e) {
            result = latestPriceProvider.provideLatestPrice(ticker);
        }

        if (currency.equals("GBp")) {
            result /= 100.0;
        }

        return result;
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

        if (calculatorParameters.startPayout != null) {
            uri += "&startPayout=" + calculatorParameters.startPayout + "&endPayout=" + calculatorParameters.endPayout;
        }
        if (calculatorParameters.type != null) {
            uri += "&type=" + calculatorParameters.type;
        }

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

    public void saveToWatchlist(String email, AddToWatchlistRequest request, AccountType accountType) {
        Optional<Watchlist> optionalWatchlist = watchlistRepository.readWatchlistByEmail(email);

        List<WatchlistElement> elements;

        if (!optionalWatchlist.isPresent()) {
            elements = new ArrayList<>();
        } else {
            elements = messageCompresser.uncompressListOf(optionalWatchlist.get().getWatchlistRaw(), WatchlistElement.class);
        }

        int index = findIndexFor(elements, request.symbol);

        WatchlistElement elementToUpdate;
        if (index == -1) {
            if (elements.size() > 500) {
                throw new WatchlistBadRequestException("Maximum of 500 watchlist element supported");
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
        if (request.moats == null ||
                (request.moats.brand == 0 && request.moats.costAdvantage == 0 && request.moats.economyOfScale == 0 && request.moats.intangibles == 0 && request.moats.networkEffect == 0
                        && request.moats.switchingCost == 0)) {
            elementToUpdate.moats = null;
        } else {
            elementToUpdate.moats = request.moats;
        }

        Watchlist toInsert = new Watchlist();
        toInsert.setEmail(email);
        toInsert.setWatchlistRaw(messageCompresser.createCompressedValue(elements));

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
        List<WatchlistElement> elements = messageCompresser.uncompressListOf(optionalWatchlist.get().getWatchlistRaw(), WatchlistElement.class);

        int index = findIndexFor(elements, request.symbol);

        if (index != -1) {
            elements.remove(index);
        }

        Watchlist toInsert = new Watchlist();
        toInsert.setEmail(email);
        toInsert.setWatchlistRaw(messageCompresser.createCompressedValue(elements));

        LOGGER.info("Remove successful, size of compressed elements={}", toInsert.getWatchlistRaw().capacity());

        watchlistRepository.save(toInsert);
        watchlistCache.invalidate(email);
    }

    public Optional<WatchlistElement> getWatchlistElement(String email, String stock) {
        Optional<Watchlist> optionalWatchlist = loadWatchlist(email);

        if (!optionalWatchlist.isPresent()) {
            return Optional.empty();
        }
        List<WatchlistElement> elements = messageCompresser.uncompressListOf(optionalWatchlist.get().getWatchlistRaw(), WatchlistElement.class);

        int index = findIndexFor(elements, stock);

        if (index != -1) {
            return Optional.of(elements.get(index));
        } else {
            return Optional.empty();
        }
    }

}
