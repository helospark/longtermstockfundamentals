package com.helospark.financialdata.management.screener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.helospark.financialdata.management.screener.annotation.ScreenerElement;
import com.helospark.financialdata.management.screener.domain.ScreenerDescription;
import com.helospark.financialdata.management.screener.domain.ScreenerResult;
import com.helospark.financialdata.management.screener.strategy.ScreenerStrategy;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

@RestController
@RequestMapping("/screener")
public class ScreenerController {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScreenerController.class);
    public static final int MAX_RESULTS = 100;
    Map<String, ScreenerDescription> idToDescription = new LinkedHashMap<>();

    @Autowired
    private SymbolAtGlanceProvider symbolAtGlanceProvider;

    @Autowired
    private List<ScreenerStrategy> screenerStrategies;

    public ScreenerController() {
        for (var field : AtGlanceData.class.getDeclaredFields()) {
            ScreenerElement screenerElement = field.getAnnotation(ScreenerElement.class);
            if (screenerElement != null) {
                String name = screenerElement.name();
                ScreenerDescription description = new ScreenerDescription();
                description.readableName = name;
                description.format = screenerElement.format();

                idToDescription.put(field.getName(), description);
            }
        }
    }

    @GetMapping("/descriptions")
    public Map<String, ScreenerDescription> getScreenerDescriptions() {
        return idToDescription;
    }

    @GetMapping("/operations")
    public List<ScreenerOperator> getScreenerOperators() {
        List<ScreenerOperator> result = new ArrayList<ScreenerOperator>();
        for (var element : screenerStrategies) {
            ScreenerOperator operator = new ScreenerOperator();
            operator.symbol = element.getSymbol();
            operator.numberOfArguments = element.getNumberOfArguments();

            result.add(operator);
        }
        return result;
    }

    @PostMapping("/perform")
    public ScreenerResult screenStocks(@RequestBody ScreenerRequest request) {
        if (request.operations.size() > 10) {
            throw new ScreenerClientSideException("Maximum of 10 elements allowed");
        }
        for (var element : request.operations) {
            if (!idToDescription.containsKey(element.id)) {
                throw new ScreenerClientSideException(element.id + " is not a valid screener condition");
            }
            ScreenerStrategy screenerStrategy = findScreenerStrategy(element.operation);
            if (screenerStrategies.stream().noneMatch(a -> a.getSymbol().equals(element.operation))) {
                throw new ScreenerClientSideException(element.operation + " is not a valid operation");
            }

            if (screenerStrategy.getNumberOfArguments() == 1 && element.number1 == null) {
                throw new ScreenerClientSideException(element.operation + " requires number1 to be filled");
            }

            if (screenerStrategy.getNumberOfArguments() == 2 && (element.number1 == null || element.number2 == null)) {
                throw new ScreenerClientSideException(element.operation + " requires number1 and number2 to be filled");
            }

        }

        List<AtGlanceData> matchedStocks = new ArrayList<>();
        LinkedHashMap<String, AtGlanceData> data = symbolAtGlanceProvider.getSymbolCompanyNameCache();
        for (var entry : data.entrySet()) {
            boolean allMatch = true;
            for (var operation : request.operations) {
                Double value = getValue(entry.getValue(), operation);
                if (value == null) {
                    allMatch = false;
                    break;
                }
                ScreenerStrategy screenerStrategy = findScreenerStrategy(operation.operation);
                if (!screenerStrategy.matches(value, operation)) {
                    allMatch = false;
                    break;
                }
            }
            if (allMatch) {
                matchedStocks.add(entry.getValue());
                if (matchedStocks.size() >= MAX_RESULTS) {
                    break;
                }
            }
        }

        ScreenerResult result = new ScreenerResult();
        result.hasMoreResults = (matchedStocks.size() >= MAX_RESULTS);

        result.columns.add("Symbol");
        result.columns.add("Company");

        for (var element : request.operations) {
            result.columns.add(idToDescription.get(element.id).readableName);
        }

        for (var stock : matchedStocks) {
            Map<String, String> columnResult = new HashMap<>();
            columnResult.put("Symbol", createSymbolLink(stock.symbol));
            columnResult.put("Company", stock.companyName);

            for (var element : request.operations) {
                ScreenerDescription screenerDescription = idToDescription.get(element.id);
                String columnName = screenerDescription.readableName;
                Double value = getValue(stock, element);
                columnResult.put(columnName, screenerDescription.format.format(value));
            }
            result.portfolio.add(columnResult);
        }

        return result;
    }

    public Double getValue(AtGlanceData glance, ScreenerOperation operation) {
        try {
            return (Double) glance.getClass().getField(operation.id).get(glance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ScreenerStrategy findScreenerStrategy(String operation) {
        return screenerStrategies.stream()
                .filter(a -> a.getSymbol().equals(operation))
                .findFirst()
                .orElse(null);
    }

    public String createSymbolLink(String ticker) {
        return "<a href=\"/stock/" + ticker + "\">" + ticker + "</a>";
    }

    @ExceptionHandler(ScreenerClientSideException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public String exceptionHandler(ScreenerClientSideException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
    public String exceptionHandler(Exception exception) {
        LOGGER.error("Unexpected error while doing screeners", exception);
        return exception.getMessage();
    }

}
