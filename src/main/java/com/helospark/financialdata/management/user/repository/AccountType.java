package com.helospark.financialdata.management.user.repository;

import java.util.Arrays;
import java.util.Optional;

public enum AccountType {
    FREE(5),
    STANDARD(100),
    ADVANCED(Integer.MAX_VALUE),
    ADMIN(Integer.MAX_VALUE);

    public static int UNLIMITED_COUNT = Integer.MAX_VALUE;
    int allowedStocksPerMonth;

    AccountType(int allowedStocksPerMonth) {
        this.allowedStocksPerMonth = allowedStocksPerMonth;
    }

    public int getAllowedStocksPerMonth() {
        return allowedStocksPerMonth;
    }

    public static AccountType fromString(String string) {
        return fromStringOptional(string).get();
    }

    public static Optional<AccountType> fromStringOptional(String string) {
        return Arrays.stream(values())
                .filter(a -> a.name().equals(string))
                .findFirst();
    }
}
