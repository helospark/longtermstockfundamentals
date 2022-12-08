package com.helospark.financialdata;

import java.time.LocalDate;

public class CommonConfig {
    public static final String BASE_FOLDER = System.getProperty("user.home") + "/Documents/financials";
    public static final String FX_BASE_FOLDER = BASE_FOLDER + "/fxratefiles";
    public static final LocalDate NOW = LocalDate.now();
}
