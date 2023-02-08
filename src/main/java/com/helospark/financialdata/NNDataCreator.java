package com.helospark.financialdata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.util.glance.AtGlanceData;

public class NNDataCreator {

    public static void main(String[] args) {
        List<List<String>> csv = new ArrayList<>();
        int atYear = 2012;
        Map<String, AtGlanceData> data = DataLoader.loadHistoricalAtGlanceData(atYear).get();

        for (var entry : data.entrySet()) {

        }
    }

}
