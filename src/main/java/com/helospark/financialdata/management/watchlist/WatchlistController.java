package com.helospark.financialdata.management.watchlist;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.user.GenericResponseAccountResult;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.management.watchlist.domain.AddToWatchlistRequest;
import com.helospark.financialdata.management.watchlist.domain.WatchListResponse;
import com.helospark.financialdata.management.watchlist.repository.WatchlistService;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class WatchlistController {
    private static final Logger LOGGER = LoggerFactory.getLogger(WatchlistController.class);
    @Autowired
    private LoginController loginController;
    @Autowired
    private WatchlistService watchlistService;
    @Autowired
    private SymbolAtGlanceProvider symbolAtGlanceProvider;

    @GetMapping("/watchlist")
    public WatchListResponse getCurrentWatchList(HttpServletRequest httpRequest) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        if (!AccountType.isAtLeastStandard(loginController.getAccountType(jwt.get()))) {
            throw new WatchlistPermissionDeniedException("Watchlist feature is only available to users with standard or advanced subscription");
        }
        return watchlistService.getWatchlist(jwt.get().getSubject());
    }

    @PostMapping("/watchlist")
    public void addToWatchlist(@RequestBody AddToWatchlistRequest request, HttpServletRequest httpRequest) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        if (!symbolAtGlanceProvider.doesCompanyExists(request.symbol)) {
            throw new WatchlistBadRequestException("Symbol does not exist");
        }
        if (request.tags != null && request.tags.size() > 10) {
            throw new WatchlistBadRequestException("Maximum of 10 tag supported");
        }
        if (request.notes != null && request.notes.length() > 200) {
            throw new WatchlistBadRequestException("Maximum of 200 characters long notes supported");
        }

        watchlistService.saveToWatchlist(jwt.get().getSubject(), request, loginController.getAccountType(jwt.get()));
    }

    @ExceptionHandler(WatchlistPermissionDeniedException.class)
    @ResponseStatus(code = HttpStatus.UNAUTHORIZED)
    public GenericResponseAccountResult handlePermissionDenied(WatchlistPermissionDeniedException exception) {
        LOGGER.warn("Unauthorized exception", exception);
        return new GenericResponseAccountResult(exception.getMessage());
    }

    @ExceptionHandler(WatchlistBadRequestException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public GenericResponseAccountResult handleBadRequestException(WatchlistBadRequestException exception) {
        LOGGER.warn("Bad request exception", exception);
        return new GenericResponseAccountResult(exception.getMessage());
    }

    @ExceptionHandler
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public GenericResponseAccountResult handleInternalError(Exception exception) {
        LOGGER.error("Unexpected exception", exception);
        return new GenericResponseAccountResult("Unexpected exception while handling watchlist");
    }
}
