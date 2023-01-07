package com.helospark.financialdata.util.analyzer;

import static com.helospark.financialdata.service.DataLoader.readFinancials;
import static com.helospark.financialdata.service.GrowthAnalyzer.isProfitableEveryYearSince;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Optional;
import java.util.Set;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.AltmanZCalculator;
import com.helospark.financialdata.service.RoicCalculator;
import com.helospark.financialdata.service.TrailingPegCalculator;

public class HighRoicScreener {
    public static final String RESULT_FILE_NAME = CommonConfig.BASE_FOLDER + "/info/screeners/high_roic.csv";
    private static final double PEG = 1.3;
    private static final double ROIC = 0.32;
    private static final double PROFITABLE = 4.0;
    private static final double ALTMAN = 5.2;

    public void analyze(Set<String> symbols) {
        StringBuilder csv = new StringBuilder();
        csv.append("Symbol;ROIC;Trailing PEG;AltmanZ;Name\n");
        System.out.printf("Symbol\tROIC\tPEG\tCompany\n");
        for (var symbol : symbols) {
            if (symbol.equals("FB")) {
                continue; // replaced by META
            }
            CompanyFinancials company = readFinancials(symbol);
            var financials = company.financials;

            if (financials.isEmpty()) {
                continue;
            }
            int yearsAgo = 0;
            int index = 0;

            double latestPrice = company.latestPrice;

            boolean continouslyProfitable = isProfitableEveryYearSince(financials, PROFITABLE + yearsAgo, yearsAgo);
            //                boolean continouslyProfitableFcf = GrowthAnalyzer.isCashFlowProfitableEveryYearSince(financials, 7.0 + yearsAgo, yearsAgo);
            double altmanZ = AltmanZCalculator.calculateAltmanZScore(financials.get(index), latestPrice);

            if (altmanZ > ALTMAN && continouslyProfitable) {
                Optional<Double> roic = RoicCalculator.getAverageRoic(company.financials, yearsAgo);
                Optional<Double> trailingPeg = TrailingPegCalculator.calculateTrailingPegWithLatestPrice(company, yearsAgo, latestPrice);

                if (roic.isPresent() && roic.get() > ROIC && trailingPeg.isPresent() && trailingPeg.get() < PEG) {
                    System.out.printf("%s\t%.2f\t%.2f\t%s | %s\n", symbol, roic.get(), trailingPeg.get(), company.profile.companyName, company.profile.industry);
                    csv.append(String.format("%s;%.2f%%;%.2f;%.2f;%s\n", symbol, (roic.get() * 100), trailingPeg.get(), altmanZ, company.profile.companyName));
                }
            }

        }

        File file = new File(RESULT_FILE_NAME);
        try (var fos = new FileOutputStream(file)) {
            fos.write(csv.toString().getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
