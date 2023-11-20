package com.helospark.financialdata.management.watchlist;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.helospark.financialdata.management.config.ratelimit.RateLimit;
import com.helospark.financialdata.management.user.GenericResponseAccountResult;
import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.watchlist.domain.AddToWatchlistRequest;
import com.helospark.financialdata.management.watchlist.domain.WatchListResponse;
import com.helospark.financialdata.management.watchlist.repository.WatchlistElement;
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
    @RateLimit(requestPerMinute = 30)
    public WatchListResponse getCurrentWatchList(HttpServletRequest httpRequest, @RequestParam(name = "onlyOwned", defaultValue = "false") boolean onlyOwned) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        return watchlistService.getWatchlist(jwt.get().getSubject(), onlyOwned);
    }

    @GetMapping("/watchlist/{stock}")
    @RateLimit(requestPerMinute = 30)
    public WatchlistElement getCurrentWatchList(@PathVariable("stock") String stock, HttpServletRequest httpRequest) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        Optional<WatchlistElement> element = watchlistService.getWatchlistElement(jwt.get().getSubject(), stock);

        if (element.isPresent()) {
            return element.get();
        } else {
            var emptyElement = new WatchlistElement();
            emptyElement.symbol = stock;

            return emptyElement;
        }
    }

    @PostMapping("/watchlist")
    @RateLimit(requestPerMinute = 30)
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
        if (request.notes != null && request.notes.length() > 250) {
            throw new WatchlistBadRequestException("Maximum of 250 characters long notes supported");
        }
        if (request.tags != null) {
            for (var tag : request.tags) {
                if (tag.strip().length() > 15) {
                    throw new WatchlistBadRequestException("Each tag can be maximum of 15 characters");
                }
            }
        }
        if (request.ownedShares < 0) {
            throw new WatchlistBadRequestException("Cannot own less than 0 shares");
        }
        if (request.calculatorParameters != null && request.calculatorParameters.type != null
                && !(request.calculatorParameters.type.equals("eps") || request.calculatorParameters.type.equals("fcf"))) {
            throw new WatchlistBadRequestException("Invalid calculator type");
        }

        watchlistService.saveToWatchlist(jwt.get().getSubject(), request, loginController.getAccountType(jwt.get()));
    }

    @DeleteMapping("/watchlist")
    @RateLimit(requestPerMinute = 30)
    public void addToWatchlist(@RequestBody DeleteFromWatchlistRequest request, HttpServletRequest httpRequest) {
        Optional<DecodedJWT> jwt = loginController.getJwt(httpRequest);
        if (jwt.isEmpty()) {
            throw new WatchlistPermissionDeniedException("Not logged in");
        }
        if (!symbolAtGlanceProvider.doesCompanyExists(request.symbol)) {
            throw new WatchlistBadRequestException("Symbol does not exist");
        }

        watchlistService.deleteFromWatchlist(jwt.get().getSubject(), request);
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
