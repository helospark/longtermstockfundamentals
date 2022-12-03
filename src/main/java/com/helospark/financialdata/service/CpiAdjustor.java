package com.helospark.financialdata.service;

import static com.helospark.financialdata.service.Helpers.findIndexWithOrBeforeDate;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CpiData;

@Service
public class CpiAdjustor {
    List<CpiData> cpiData = new ArrayList<>();

    public CpiAdjustor() {
        cpiData = DataLoader.readListOfClassFromFile(new File(CommonConfig.BASE_FOLDER + "/info/cpi.json"), CpiData.class);
    }

    public double adjustForInflationToOldDate(double value, LocalDate oldDate, LocalDate newDate) {
        int oldIndex = findIndexWithOrBeforeDate(cpiData, oldDate);
        int newIndex = findIndexWithOrBeforeDate(cpiData, newDate);

        if (oldIndex == -1 || newIndex == -1) {
            return value;
        }

        double oldCpi = cpiData.get(oldIndex).value;
        double newCpi = cpiData.get(newIndex).value;

        double adjustment = newCpi / oldCpi;

        return value * adjustment;
    }
}
