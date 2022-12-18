package com.helospark.financialdata;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.helospark.financialdata.service.SymbolIndexProvider;

@Controller
@RequestMapping("/")
public class ViewController {
    @Autowired
    private SymbolIndexProvider symbolIndexProvider;

    @GetMapping("/stock/{stock}")
    public String stock(@PathVariable("stock") String stock, Model model) {
        Optional<String> companyNameOptional = symbolIndexProvider.getCompanyName(stock);
        String companyName = stock;
        if (companyNameOptional.isPresent()) {
            companyName = companyNameOptional.get();
        }

        model.addAttribute("stock", stock);
        model.addAttribute("companyName", companyName);
        return "stock";
    }

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

}
