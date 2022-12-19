package com.helospark.financialdata;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.service.DataLoader;
import com.helospark.financialdata.service.GrowthCalculator;
import com.helospark.financialdata.service.MarginCalculator;
import com.helospark.financialdata.service.SymbolIndexProvider;

@Controller
@RequestMapping("/")
public class ViewController {
    @Autowired
    private SymbolIndexProvider symbolIndexProvider;

    @GetMapping("/stock/{stock}")
    public String stock(@PathVariable("stock") String stock, Model model) {
        fillModelWithCommonStockData(stock, model);
        return "stock";
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @GetMapping("/calculator/{stock}")
    public String calculator(@PathVariable("stock") String stock, Model model) {
        fillModelWithCommonStockData(stock, model);
        CompanyFinancials company = DataLoader.readFinancials(stock);
        if (company.financials.size() > 0) {
            double revenueGrowth = GrowthCalculator.getMedianRevenueGrowth(company.financials, 8, 0.0).orElse(10.0);
            double margin = MarginCalculator.getAvgNetMargin(company.financials, 0);
            double shareCountGrowth = GrowthCalculator.getShareCountGrowthInInterval(company.financials, 5, 0).orElse(0.0);
            double endGrowth = revenueGrowth * 0.5;
            double endMultiple = 12;

            if (endGrowth > 12) {
                endMultiple = endGrowth;
            }
            if (endMultiple > 24) {
                endMultiple = 24;
            }

            model.addAttribute("revenue", (double) company.financials.get(0).incomeStatementTtm.revenue / 1_000_000);
            model.addAttribute("startGrowth", String.format("%.2f", revenueGrowth));
            model.addAttribute("endGrowth", String.format("%.2f", endGrowth));
            model.addAttribute("startMargin", String.format("%.2f", margin * 100.0));
            model.addAttribute("endMargin", String.format("%.2f", margin * 100.0));
            model.addAttribute("shareChange", String.format("%.2f", shareCountGrowth));
            model.addAttribute("shareCount", company.financials.get(0).incomeStatementTtm.weightedAverageShsOut / 1000);
            model.addAttribute("endMultiple", String.format("%.2f", endMultiple));
        }

        model.addAttribute("latestPrice", company.latestPrice);

        return "calculator";
    }

    public void fillModelWithCommonStockData(String stock, Model model) {
        Optional<String> companyNameOptional = symbolIndexProvider.getCompanyName(stock);
        String companyName = stock;
        if (companyNameOptional.isPresent()) {
            companyName = companyNameOptional.get();
        }

        model.addAttribute("stock", stock);
        model.addAttribute("companyName", companyName);
    }

}
