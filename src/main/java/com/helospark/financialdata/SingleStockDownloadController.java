package com.helospark.financialdata;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.watchlist.WatchlistPermissionDeniedException;
import com.helospark.financialdata.management.watchlist.repository.LatestPriceProvider;
import com.helospark.financialdata.management.watchlist.repository.WatchlistElement;
import com.helospark.financialdata.management.watchlist.repository.WatchlistService;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.StockDataDownloader;
import com.helospark.financialdata.util.StockDataDownloader.DownloadDateData;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SingleStockDownloadController {

    @Autowired
    private LoginController loginController;
    @Autowired
    private SymbolAtGlanceProvider symbolAtGlanceProvider;
    @Autowired
    private WatchlistService watchlistService;
    @Autowired
    private LatestPriceProvider latestPriceProvider;

    public void ensureOnlyAdminAccess(HttpServletRequest request) {
        Optional<AccountType> accountType = loginController.getAccountType(request);
        if (accountType.isEmpty() || !accountType.get().equals(AccountType.ADMIN)) {
            throw new UnauthorizedException();
        }
    }

    @GetMapping("/download")
    public DownloadDateData download(@RequestParam("stock") String stock, HttpServletRequest request, @RequestParam(name = "force", required = false) boolean forceRenew) {
        ensureOnlyAdminAccess(request);
        latestPriceProvider.removeFromCache(List.of(stock));
        return StockDataDownloader.downloadOneStock(stock, symbolAtGlanceProvider, forceRenew);
    }

    @GetMapping("/refresh-portfolio")
    public void refreshPortfolio(HttpServletRequest request) {
        ensureOnlyAdminAccess(request);

        List<String> symbols = getStocks(request);

        StockDataDownloader.downloadMultiStock(symbols, symbolAtGlanceProvider);

        latestPriceProvider.removeFromCache(symbols);
    }

    @GetMapping("/drop-price-cache")
    public void dropPriceCache(HttpServletRequest request) {
        ensureOnlyAdminAccess(request);

        List<String> symbols = getStocks(request);

        latestPriceProvider.removeFromCache(symbols);
    }

    public List<String> getStocks(HttpServletRequest request) {
        Optional<DecodedJWT> jwt = loginController.getJwt(request);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        if (loginController.getAccountType(jwt.get()) != AccountType.ADMIN) {
            throw new WatchlistPermissionDeniedException("This is only for admins");
        }
        List<WatchlistElement> watchlistElements = watchlistService.readWatchlistFromDb(jwt.get().getSubject());

        List<String> symbols = watchlistElements.stream()
                .filter(a -> a.ownedShares > 0)
                .map(a -> a.symbol)
                .collect(Collectors.toList());
        return symbols;
    }
}
