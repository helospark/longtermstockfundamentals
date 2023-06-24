package com.helospark.financialdata;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.management.user.LoginController;
import com.helospark.financialdata.management.user.repository.AccountType;
import com.helospark.financialdata.util.StockDataDownloader2;
import com.helospark.financialdata.util.StockDataDownloader2.DownloadDateData;

import jakarta.servlet.http.HttpServletRequest;

@RestController
public class SingleStockDownloadController {

    @Autowired
    private LoginController loginController;

    public void ensureOnlyAdminAccess(HttpServletRequest request) {
        Optional<AccountType> accountType = loginController.getAccountType(request);
        if (accountType.isEmpty() || !accountType.get().equals(AccountType.ADMIN)) {
            throw new UnauthorizedException();
        }
    }

    @GetMapping("/download")
    public DownloadDateData download(@RequestParam("stock") String stock, HttpServletRequest request) {
        ensureOnlyAdminAccess(request);
        return StockDataDownloader2.downloadOneStock(stock);
    }
}
