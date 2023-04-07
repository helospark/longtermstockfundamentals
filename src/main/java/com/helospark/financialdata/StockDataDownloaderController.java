package com.helospark.financialdata;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.util.StockDataDownloader2;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/stockdatadownloader")
public class StockDataDownloaderController {
    private ExecutorService executor;
    private CompletableFuture<Void> future;

    @Autowired
    private LoginController loginController;

    @GetMapping("/start")
    public void start(HttpServletRequest request) {
        ensureOnlyAdminAccess(request);
        if (executor == null && !StockDataDownloader2.isRunning()) {
            executor = Executors.newSingleThreadExecutor();
            future = CompletableFuture.runAsync(() -> {
                try {
                    StockDataDownloader2.main(null);
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }, executor);
        }
    }

    public void ensureOnlyAdminAccess(HttpServletRequest request) {
        Optional<AccountType> accountType = loginController.getAccountType(request);
        if (accountType.isEmpty() || !accountType.get().equals(AccountType.ADMIN)) {
            throw new UnauthorizedException();
        }
    }

    @GetMapping("/stop")
    public void stop(HttpServletRequest request) {
        ensureOnlyAdminAccess(request);
        if (StockDataDownloader2.isRunning()) {
            StockDataDownloader2.stop();
        }
        try {
            future.get(20000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
            for (int i = 0; i < 10; ++i) {
                boolean isCancelled = future.cancel(true);
                if (isCancelled) {
                    break;
                }
                sleep();
            }
        }
        future = null;
        executor.shutdownNow();
        executor = null;
    }

    public void sleep() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/status")
    public String status(HttpServletRequest request) {
        ensureOnlyAdminAccess(request);
        if (!StockDataDownloader2.isRunning()) {
            return "Not running";
        }
        if (future == null) {
            return "Not running (2)";
        }
        String responseString = StockDataDownloader2.getStatusMessage();
        double progress = StockDataDownloader2.getProgress();
        if (progress != Double.NaN) {
            responseString += " " + String.format("%.2f%%", progress);
        }
        return responseString;
    }

    @ExceptionHandler(UnauthorizedException.class)
    @ResponseStatus(value = HttpStatus.UNAUTHORIZED)
    public String handleUnauthorized(UnauthorizedException e) {
        return "Unauthorized";
    }
}
