package com.helospark.financialdata;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.helospark.financialdata.domain.Profile;
import com.helospark.financialdata.service.DataLoader;

public class GetAllSectors {

    @Test
    public void asd() {
        Set<String> symbols = DataLoader.provideAllSymbols();
        Map<String, Integer> sectorToCount = new HashMap<>();

        for (var symbol : symbols) {
            List<Profile> company = DataLoader.readFinancialFile(symbol, "profile.json", Profile.class);

            if (company != null && company.size() > 0 && company.get(0).sector != null) {
                sectorToCount.merge(company.get(0).sector, 1, Integer::sum);
            }
        }

        System.out.println(sectorToCount);
    }

}
