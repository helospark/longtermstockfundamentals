package com.helospark.financialdata;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.domain.SearchElement;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;

@RestController
public class SearchSuggestionsController {
    @Autowired
    private SymbolAtGlanceProvider symbolIndexProvider;

    @RequestMapping("/suggest")
    public List<SearchElement> suggestSearch(@RequestParam("search") String search) {
        return symbolIndexProvider.getTopResult(search);
    }

}
