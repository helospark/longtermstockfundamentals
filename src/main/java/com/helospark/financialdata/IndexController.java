package com.helospark.financialdata;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class IndexController {

    @GetMapping("/stock/{stock}")
    public String index(@PathVariable("stock") String stock, Model model) {
        model.addAttribute("stock", stock);
        return "index";
    }

}
