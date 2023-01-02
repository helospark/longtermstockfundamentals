package com.helospark.financialdata;

import java.time.LocalDate;

public class CommonConfig {
    public static final String BASE_FOLDER;
    public static final String FX_BASE_FOLDER;;
    public static final LocalDate NOW = LocalDate.now();

    static {
        if (System.getProperty("BASE_FOLDER") != null) {
            BASE_FOLDER = System.getProperty("BASE_FOLDER");
        } else {
            BASE_FOLDER = System.getProperty("user.home") + "/Documents/financials";
        }
        FX_BASE_FOLDER = BASE_FOLDER + "/fxratefiles";
    }
}
