package com.helospark.financialdata.management.email;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.management.inspire.jobs.HighRoicJob;
import com.helospark.financialdata.management.user.ClearViewCountJob;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.EverythingMoneyCalculator;

@RestController
public class TestController {
    @Autowired
    EmailSender emailSender;
    @Autowired
    private ClearViewCountJob clearViewCountJob;
    @Autowired
    private EmailTemplateReader emailTemplateReader;
    @Autowired
    private HighRoicJob highRoicJob;

    @GetMapping("/test_123")
    public String test(Model model) {
        CompanyFinancials msft = DataLoader.readFinancials("AAPL");
        Optional<Double> roic = EverythingMoneyCalculator.calculateFiveYearRoic(msft, 0);
        Optional<Double> incomeGrowth = EverythingMoneyCalculator.calculate5YearNetIncomeGrowth(msft, 0);
        Optional<Double> shareGrowth = EverythingMoneyCalculator.calculate5YearShareGrowth(msft, 0);
        Optional<Double> revenueGrowth = EverythingMoneyCalculator.calculateFiveYearRevenueGrowth(msft, 0);
        Optional<Double> fcfGrowth = EverythingMoneyCalculator.calculate5YearFcfGrowth(msft, 0);
        Optional<Double> fiveYearFcf = EverythingMoneyCalculator.calculateFiveYearFcf(msft, 0);
        Optional<Double> fiveYearPe = EverythingMoneyCalculator.calculateFiveYearPe(msft, 0);
        Optional<Double> ltlPerFcf = EverythingMoneyCalculator.calculateLtlPer5YrFcf(msft, 0);

        System.out.println("roic=" + roic + " (25)");
        System.out.println("incomeGrowth=" + incomeGrowth + " (25)");
        System.out.println("shareGrowth=" + shareGrowth + " (25)");
        System.out.println("revenueGrowth=" + revenueGrowth + " (25)");
        System.out.println("fcfGrowth=" + fcfGrowth + " (25)");
        System.out.println("fiveYearFcf=" + fiveYearFcf + " (25)");
        System.out.println("fiveYearPe=" + fiveYearPe + " (25)");
        System.out.println("ltlPerFcf=" + ltlPerFcf + " (25)");

        //        CompanyFinancials aapl = DataLoader.readFinancials("AAPL");
        //        Optional<Double> aaplResult = EverythingMoneyCalculator.calculateFiveYearRoic(aapl, 0);
        //
        //        System.out.println("AAPL=" + aaplResult + " (37.9)");
        //
        //        CompanyFinancials pypl = DataLoader.readFinancials("PYPL");
        //        Optional<Double> pyplResult = EverythingMoneyCalculator.calculateFiveYearRoic(pypl, 0);
        //
        //        System.out.println("PYPL=" + pyplResult + " (9.9)");
        //
        //        CompanyFinancials googl = DataLoader.readFinancials("GOOGL");
        //        Optional<Double> googlResult = EverythingMoneyCalculator.calculateFiveYearRoic(googl, 0);
        //
        //        System.out.println("GOOGL=" + googlResult + " (14.4%)");

        return "index";
    }

}
