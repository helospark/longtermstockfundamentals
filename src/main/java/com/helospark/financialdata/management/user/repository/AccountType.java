package com.helospark.financialdata.management.user.repository;

import java.util.Arrays;
import java.util.Optional;

public enum AccountType {
    FREE(5, 0),
    STANDARD(100, 5),
    ADVANCED(Integer.MAX_VALUE, 10),
    ADMIN(Integer.MAX_VALUE, 0);

    public static int UNLIMITED_COUNT = Integer.MAX_VALUE;
    int allowedStocksPerMonth;
    int price;

    AccountType(int allowedStocksPerMonth, int price) {
        this.allowedStocksPerMonth = allowedStocksPerMonth;
        this.price = price;
    }

    public int getAllowedStocksPerMonth() {
        return allowedStocksPerMonth;
    }

    public int getPrice() {
        return price;
    }

    public static AccountType fromString(String string) {
        return fromStringOptional(string).get();
    }

    public static Optional<AccountType> fromStringOptional(String string) {
        return Arrays.stream(values())
                .filter(a -> a.name().equals(string))
                .findFirst();
    }

    public static boolean isAtLeastStandard(AccountType accountType) {
        return accountType == AccountType.STANDARD || accountType == AccountType.ADVANCED || accountType == AccountType.ADMIN;
    }
}
