package com.helospark.financialdata.service;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.DataValuePairData;

public class FedRateProvider {
    public static List<DataValuePairData> fedFundsRate = new ArrayList<>();

    static {
        fedFundsRate = DataLoader.readListOfClassFromFile(new File(CommonConfig.BASE_FOLDER + "/info/federalFunds.json"), DataValuePairData.class);
    }

    public static double getFedFundsRate(LocalDate date) {
        int resultIndex = Helpers.findIndexWithOrBeforeDateSafe(fedFundsRate, date);
        return fedFundsRate.get(resultIndex).value;
    }
}
