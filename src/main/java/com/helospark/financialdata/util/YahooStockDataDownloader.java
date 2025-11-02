package com.helospark.financialdata.util;

import static java.time.temporal.ChronoUnit.YEARS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.helospark.financialdata.domain.BalanceSheet;
import com.helospark.financialdata.domain.CashFlow;
import com.helospark.financialdata.domain.CompanyFinancials;
import com.helospark.financialdata.domain.FinancialsTtm;
import com.helospark.financialdata.domain.IncomeStatement;
import com.helospark.financialdata.domain.SimpleDateDataElement;
import com.helospark.financialdata.service.DataLoader;

public class YahooStockDataDownloader {
    static List<Pair> incomeStatementMapping = new ArrayList<>();
    static List<Pair> balanceSheetMapping = new ArrayList<>();
    static List<Pair> cashFlowStatementMapping = new ArrayList<>();
    static RestTemplate restTemplate = new RestTemplate();

    static void put(List<Pair> list, String key, FieldMapping mapping) {
        list.add(new Pair(key, mapping));
    }

    static {
        put(incomeStatementMapping, "quarterlyTotalRevenue", new FieldMapping("revenue"));
        put(incomeStatementMapping, "quarterlyCostOfRevenue", new FieldMapping("costOfRevenue"));
        put(incomeStatementMapping, "quarterlyGrossProfit", new FieldMapping("grossProfit"));
        put(incomeStatementMapping, "quarterlyResearchAndDevelopment", new FieldMapping("researchAndDevelopmentExpenses"));
        put(incomeStatementMapping, "quarterlyGeneralAndAdministrativeExpense", new FieldMapping("generalAndAdministrativeExpenses"));
        put(incomeStatementMapping, "quarterlySellingAndMarketingExpense", new FieldMapping("sellingAndMarketingExpenses"));
        put(incomeStatementMapping, "quarterlySellingGeneralAndAdministration", new FieldMapping("sellingGeneralAndAdministrativeExpenses"));
        put(incomeStatementMapping, "quarterlyOtherIncomeExpense", new FieldMapping("otherExpenses"));
        put(incomeStatementMapping, "quarterlyOperatingExpense", new FieldMapping("operatingExpenses"));
        put(incomeStatementMapping, "quarterlyTotalExpenses", new FieldMapping("costAndExpenses"));
        put(incomeStatementMapping, "quarterlyNetInterestIncome", new FieldMapping("interestIncome"));
        put(incomeStatementMapping, "quarterlyInterestExpense", new FieldMapping("interestExpense"));
        put(incomeStatementMapping, "quarterlyDepreciationAndAmortizationInIncomeStatement", new FieldMapping("depreciationAndAmortization"));
        put(incomeStatementMapping, "quarterlyEBITDA", new FieldMapping("ebitda"));
        put(incomeStatementMapping, "quarterlyOperatingIncome", new FieldMapping("operatingIncome"));
        put(incomeStatementMapping, "quarterlyOtherIncomeExpense", new FieldMapping("totalOtherIncomeExpensesNet"));
        put(incomeStatementMapping, "quarterlyPretaxIncome", new FieldMapping("incomeBeforeTax"));
        put(incomeStatementMapping, "quarterlyTaxProvision", new FieldMapping("incomeTaxExpense"));
        put(incomeStatementMapping, "quarterlyNetIncome", new FieldMapping("netIncome"));
        put(incomeStatementMapping, "quarterlyBasicEPS", new FieldMapping("eps"));
        put(incomeStatementMapping, "quarterlyDilutedEPS", new FieldMapping("epsdiluted"));
        put(incomeStatementMapping, "quarterlyBasicAverageShares", new FieldMapping("weightedAverageShsOut"));
        put(incomeStatementMapping, "quarterlyDilutedAverageShares", new FieldMapping("weightedAverageShsOutDil"));

        put(balanceSheetMapping, "quarterlyCashAndCashEquivalents", new FieldMapping("cashAndCashEquivalents"));
        put(balanceSheetMapping, "quarterlyCashCashEquivalentsAndShortTermInvestments", new FieldMapping("shortTermInvestments"));
        put(balanceSheetMapping, "quarterlyCashCashEquivalentsAndShortTermInvestments", new FieldMapping("cashAndShortTermInvestments"));
        put(balanceSheetMapping, "quarterlyReceivables", new FieldMapping("netReceivables"));
        put(balanceSheetMapping, "quarterlyInventory", new FieldMapping("inventory"));
        put(balanceSheetMapping, "quarterlyOtherCurrentAssets", new FieldMapping("otherCurrentAssets"));
        put(balanceSheetMapping, "quarterlyCurrentAssets", new FieldMapping("totalCurrentAssets"));
        put(balanceSheetMapping, "quarterlyNetPPE", new FieldMapping("propertyPlantEquipmentNet"));
        put(balanceSheetMapping, "quarterlyGoodwill", new FieldMapping("goodwill"));
        put(balanceSheetMapping, "quarterlyGoodwillAndOtherIntangibleAssets", new FieldMapping("intangibleAssets"));
        put(balanceSheetMapping, "quarterlyGoodwillAndOtherIntangibleAssets", new FieldMapping("goodwillAndIntangibleAssets"));
        put(balanceSheetMapping, "quarterlyInvestmentsAndAdvances", new FieldMapping("longTermInvestments"));
        put(balanceSheetMapping, "quarterlyNonCurrentDeferredTaxesAssets", new FieldMapping("taxAssets"));
        put(balanceSheetMapping, "quarterlyOtherNonCurrentAssets", new FieldMapping("otherNonCurrentAssets"));
        put(balanceSheetMapping, "quarterlyTotalNonCurrentAssets", new FieldMapping("totalNonCurrentAssets"));
        put(balanceSheetMapping, "quarterlyOtherCurrentAssets", new FieldMapping("otherAssets"));
        put(balanceSheetMapping, "quarterlyTotalAssets", new FieldMapping("totalAssets"));
        put(balanceSheetMapping, "quarterlyPayables", new FieldMapping("accountPayables"));
        put(balanceSheetMapping, "quarterlyCurrentDebt", new FieldMapping("shortTermDebt"));
        put(balanceSheetMapping, "quarterlyTotalTaxPayable", new FieldMapping("taxPayables"));
        put(balanceSheetMapping, "quarterlyNonCurrentDeferredRevenue", new FieldMapping("deferredRevenue"));
        put(balanceSheetMapping, "quarterlyOtherCurrentLiabilities", new FieldMapping("otherCurrentLiabilities"));
        put(balanceSheetMapping, "quarterlyCurrentLiabilities", new FieldMapping("totalCurrentLiabilities"));
        put(balanceSheetMapping, "quarterlyLongTermDebt", new FieldMapping("longTermDebt"));
        put(balanceSheetMapping, "quarterlyTotalRevenue", new FieldMapping("deferredRevenueNonCurrent"));
        put(balanceSheetMapping, "quarterlyCurrentDeferredTaxesLiabilities", new FieldMapping("deferredTaxLiabilitiesNonCurrent"));
        put(balanceSheetMapping, "quarterlyOtherNonCurrentLiabilities", new FieldMapping("otherNonCurrentLiabilities"));
        put(balanceSheetMapping, "quarterlyTotalNonCurrentLiabilitiesNetMinorityInterest", new FieldMapping("totalNonCurrentLiabilities"));
        put(balanceSheetMapping, "quarterlyOtherNonCurrentLiabilities", new FieldMapping("otherLiabilities"));
        put(balanceSheetMapping, "quarterlyLongTermCapitalLeaseObligation", new FieldMapping("capitalLeaseObligations"));
        put(balanceSheetMapping, "quarterlyTotalLiabilitiesNetMinorityInterest", new FieldMapping("totalLiabilities"));
        put(balanceSheetMapping, "quarterlyPreferredStock", new FieldMapping("preferredStock"));
        put(balanceSheetMapping, "quarterlyCommonStock", new FieldMapping("commonStock"));
        put(balanceSheetMapping, "quarterlyRetainedEarnings", new FieldMapping("retainedEarnings"));
        //put(balanceSheetMapping, "quarterlyTotalRevenue", new FieldMapping("accumulatedOtherComprehensiveIncomeLoss"));
        put(balanceSheetMapping, "quarterlyOtherEquityAdjustments", new FieldMapping("othertotalStockholdersEquity"));
        put(balanceSheetMapping, "quarterlyStockholdersEquity", new FieldMapping("totalStockholdersEquity"));
        // put(balanceSheetMapping, "quarterlyTotalRevenue", new FieldMapping("totalLiabilitiesAndStockholdersEquity"));
        put(balanceSheetMapping, "quarterlyMinorityInterest", new FieldMapping("minorityInterest"));
        put(balanceSheetMapping, "quarterlyTotalEquityGrossMinorityInterest", new FieldMapping("totalEquity"));
        // put(balanceSheetMapping, "quarterlyTotalRevenue", new FieldMapping("totalLiabilitiesAndTotalEquity"));
        put(balanceSheetMapping, "quarterlyInvestmentsAndAdvances", new FieldMapping("totalInvestments"));
        put(balanceSheetMapping, "quarterlyTotalDebt", new FieldMapping("totalDebt"));
        put(balanceSheetMapping, "quarterlyNetDebt", new FieldMapping("netDebt"));

        put(cashFlowStatementMapping, "quarterlyNetIncomeFromContinuingOperations", new FieldMapping("netIncome"));
        put(cashFlowStatementMapping, "quarterlyDepreciationAndAmortization", new FieldMapping("depreciationAndAmortization"));
        put(cashFlowStatementMapping, "quarterlyDeferredIncomeTax", new FieldMapping("deferredIncomeTax"));
        put(cashFlowStatementMapping, "quarterlyStockBasedCompensation", new FieldMapping("stockBasedCompensation"));
        put(cashFlowStatementMapping, "quarterlyChangeInWorkingCapital", new FieldMapping("changeInWorkingCapital"));
        put(cashFlowStatementMapping, "quarterlyChangesInAccountReceivables", new FieldMapping("accountsReceivables"));
        put(cashFlowStatementMapping, "quarterlyChangeInInventory", new FieldMapping("inventory"));
        put(cashFlowStatementMapping, "quarterlyChangeInPayablesAndAccruedExpense", new FieldMapping("accountsPayables"));
        put(cashFlowStatementMapping, "quarterlyChangeInOtherWorkingCapital", new FieldMapping("otherWorkingCapital"));
        put(cashFlowStatementMapping, "quarterlyOtherNonCashItems", new FieldMapping("otherNonCashItems"));
        put(cashFlowStatementMapping, "quarterlyOperatingCashFlow", new FieldMapping("netCashProvidedByOperatingActivities"));
        put(cashFlowStatementMapping, "quarterlyPurchaseOfPPE", new FieldMapping("investmentsInPropertyPlantAndEquipment"));
        put(cashFlowStatementMapping, "quarterlyPurchaseOfBusiness", new FieldMapping("acquisitionsNet"));
        //put(cashFlowStatementMapping, "quarterlyCashAndCashEquivalents", new FieldMapping("purchasesOfInvestments"));
        put(cashFlowStatementMapping, "quarterlySaleOfInvestment", new FieldMapping("salesMaturitiesOfInvestments"));
        put(cashFlowStatementMapping, "quarterlyNetOtherInvestingChanges", new FieldMapping("otherInvestingActivites"));
        put(cashFlowStatementMapping, "quarterlyInvestingCashFlow", new FieldMapping("netCashUsedForInvestingActivites"));
        put(cashFlowStatementMapping, "quarterlyNetIssuancePaymentsOfDebt", new FieldMapping("debtRepayment"));
        put(cashFlowStatementMapping, "quarterlyCommonStockIssuance", new FieldMapping("commonStockIssued"));
        put(cashFlowStatementMapping, "quarterlyRepurchaseOfCapitalStock", new FieldMapping("commonStockRepurchased"));
        put(cashFlowStatementMapping, "quarterlyCashDividendsPaid", new FieldMapping("dividendsPaid"));
        put(cashFlowStatementMapping, "quarterlyFinancingCashFlow", new FieldMapping("otherFinancingActivites"));
        put(cashFlowStatementMapping, "quarterlyCashFlowFromContinuingFinancingActivities", new FieldMapping("netCashUsedProvidedByFinancingActivities"));
        //put(cashFlowStatementMapping, "quarterlyCashAndCashEquivalents", new FieldMapping("effectOfForexChangesOnCash"));
        put(cashFlowStatementMapping, "quarterlyCashAndCashEquivalents", new FieldMapping("netChangeInCash"));
        put(cashFlowStatementMapping, "quarterlyEndCashPosition", new FieldMapping("cashAtEndOfPeriod"));
        put(cashFlowStatementMapping, "quarterlyBeginningCashPosition", new FieldMapping("cashAtBeginningOfPeriod"));
        put(cashFlowStatementMapping, "quarterlyOperatingCashFlow", new FieldMapping("operatingCashFlow"));
        put(cashFlowStatementMapping, "quarterlyCapitalExpenditure", new FieldMapping("capitalExpenditure"));
        put(cashFlowStatementMapping, "quarterlyFreeCashFlow", new FieldMapping("freeCashFlow"));
    }

    public static void main(String[] args) throws Exception {
        String symbol = "UNH";

        StatementMetadata metadata = readStatementMetadata(symbol);
        List<IncomeStatement> incomeStatements = new ArrayList<>();
        List<BalanceSheet> balanceSheets = new ArrayList<>();
        List<CashFlow> cashFlows = new ArrayList<>();

        System.out.println(metadata);

        for (var date : metadata.availableDates) {
            incomeStatements.add(readIncomeStatementFull(date, symbol, metadata.reportingCurrency));
            balanceSheets.add(readBalanceSheetFull(date, symbol, metadata.reportingCurrency));
            cashFlows.add(readCashFlowStatementFull(date, symbol, metadata.reportingCurrency));
        }

        IncomeStatement incomeStatement = incomeStatements.get(incomeStatements.size() - 1);
        BalanceSheet balanceSheet = balanceSheets.get(balanceSheets.size() - 1);
        CashFlow cashFlowStatement = cashFlows.get(cashFlows.size() - 1);

        CompanyFinancials company = DataLoader.readFinancials(symbol);
        FinancialsTtm financialsTtm = company.financials.get(1);
        IncomeStatement realIncomeStatement = financialsTtm.incomeStatement;

        for (var field : IncomeStatement.class.getFields()) {
            Object val1 = field.get(incomeStatement);
            Object val2 = field.get(realIncomeStatement);
            System.out.println(field.getName() + "\n" + val1 + "\n" + val2 + "\n\n");
        }

        System.out.println();
        System.out.println("--------------------");
        System.out.println();

        BalanceSheet realBalanceSheet = financialsTtm.balanceSheet;

        for (var field : BalanceSheet.class.getFields()) {
            Object val1 = field.get(balanceSheet);
            Object val2 = field.get(realBalanceSheet);
            System.out.println(field.getName() + "\n" + val1 + "\n" + val2 + "\n\n");
        }

        System.out.println();
        System.out.println("--------------------");
        System.out.println();

        CashFlow realCashFlow = financialsTtm.cashFlow;

        for (var field : CashFlow.class.getFields()) {
            Object val1 = field.get(cashFlowStatement);
            Object val2 = field.get(realCashFlow);
            System.out.println(field.getName() + "\n" + val1 + "\n" + val2 + "\n\n");
        }

        var prices = downloadPrices(symbol);

        System.out.println(prices.currency);
        for (var price : prices.prices) {
            //    System.out.println(price.date + " " + price.value);
        }
    }

    private static PriceToCurrencyPair downloadPrices(String symbol) throws JsonMappingException, JsonProcessingException, FileNotFoundException, IOException {
        String url = getPricesUrl(symbol);
        String cacheKey = "prices2";

        JsonNode dataDeserialized = readUrlToTree(symbol, url, cacheKey);

        JsonNode data = dataDeserialized.get("chart").get("result").get(0);
        ArrayNode timestampNode = (ArrayNode) data.get("timestamp");

        List<Long> timestampList = new ArrayList<>();
        List<Double> pricesList = new ArrayList<>();

        for (var element : timestampNode) {
            timestampList.add(element.asLong());
        }

        ArrayNode pricesNode = (ArrayNode) data.get("indicators").get("quote").get(0).get("close");

        for (var element : pricesNode) {
            pricesList.add(element.asDouble());
        }

        List<SimpleDateDataElement> prices = new ArrayList<>();
        for (int i = 0; i < timestampList.size(); ++i) {
            LocalDate timestamp = LocalDate.ofInstant(Instant.ofEpochSecond(timestampList.get(i)), ZoneId.of("UTC"));
            double price = pricesList.get(i);
            prices.add(new SimpleDateDataElement(timestamp, price));
        }

        String currency = data.get("meta").get("currency").asText();

        return new PriceToCurrencyPair(currency, prices);
    }

    private static String getPricesUrl(String symbol) {
        String result = "https://query2.finance.yahoo.com/v8/finance/chart/{symbol}?period1={startDate}&period2={endDate}&interval=1d&"
                + "includePrePost=true&events=div|split|earn&lang=en-US&region=US&source=cosaic";

        LocalDateTime currentDateTime = LocalDateTime.now();
        Instant currentDate = currentDateTime.toInstant(ZoneOffset.ofHours(0));
        Instant previousDate = currentDateTime.minus(5, YEARS).toInstant(ZoneOffset.ofHours(0));

        result = result.replace("{symbol}", symbol);
        result = result.replace("{endDate}", String.valueOf(currentDate.toEpochMilli() / 1000L));
        result = result.replace("{startDate}", String.valueOf(previousDate.toEpochMilli() / 1000L));

        return result;
    }

    private static String getReportingCurrency(JsonNode dataDeserialized) throws JsonMappingException, JsonProcessingException, FileNotFoundException, IOException {
        ArrayNode node = getOperatingExpenseNode(dataDeserialized);

        if (node == null) {
            return "USD";
        }

        return node.get(0).get("currencyCode").asText();
    }

    private static List<String> getAvailableDates(JsonNode dataDeserialized) throws JsonMappingException, JsonProcessingException, FileNotFoundException, IOException {
        ArrayNode node = getOperatingExpenseNode(dataDeserialized);

        if (node == null) {
            return List.of();
        }

        List<String> result = new ArrayList<>();

        for (var a : node) {
            result.add(a.get("asOfDate").asText());
        }

        return result;
    }

    public static ArrayNode getOperatingExpenseNode(JsonNode dataDeserialized) {
        ArrayNode list = (ArrayNode) dataDeserialized.get("timeseries").get("result");

        ArrayNode node = null;
        for (var a : list) {
            node = (ArrayNode) a.get("quarterlyOperatingExpense");

            if (node != null) {
                break;
            }
        }
        return node;
    }

    public static StatementMetadata readStatementMetadata(String symbol) throws JsonMappingException, JsonProcessingException, FileNotFoundException, IOException {
        String url = getIncomeStatementUri(symbol);
        String cacheKey = "income-statement";

        JsonNode dataDeserialized = readUrlToTree(symbol, url, cacheKey);

        List<String> availableDates = getAvailableDates(dataDeserialized);
        String reportingCurrency = getReportingCurrency(dataDeserialized);

        return new StatementMetadata(availableDates, reportingCurrency);
    }

    public static IncomeStatement readIncomeStatementFull(String date, String symbol, String currency)
            throws IOException, FileNotFoundException, JsonProcessingException, JsonMappingException, Exception {
        long multiplier = 1_000_000;
        String url = getIncomeStatementUri(symbol);
        String cacheKey = "income-statement";

        JsonNode dataDeserialized = readUrlToTree(symbol, url, cacheKey);

        IncomeStatement incomeStatement = readIncomeStatement(date, dataDeserialized, multiplier);

        incomeStatement.date = LocalDate.parse(date);
        incomeStatement.reportedCurrency = currency;

        return incomeStatement;
    }

    public static BalanceSheet readBalanceSheetFull(String date, String symbol, String currency) throws IOException, FileNotFoundException, JsonProcessingException, JsonMappingException, Exception {
        long multiplier = 1_000_000;
        String url = getBalanceSheetUri(symbol);
        String cacheKey = "balance-sheet";

        JsonNode dataDeserialized = readUrlToTree(symbol, url, cacheKey);

        BalanceSheet incomeStatement = readBalanceSheet(date, dataDeserialized, multiplier);
        incomeStatement.date = LocalDate.parse(date);
        incomeStatement.reportedCurrency = currency;
        return incomeStatement;
    }

    public static CashFlow readCashFlowStatementFull(String date, String symbol, String currency) throws IOException, FileNotFoundException, JsonProcessingException, JsonMappingException, Exception {
        long multiplier = 1_000_000;
        String url = getCashFlowUri(symbol);
        String cacheKey = "cash-flow";

        JsonNode dataDeserialized = readUrlToTree(symbol, url, cacheKey);

        CashFlow incomeStatement = readCashFlow(date, dataDeserialized, multiplier);
        incomeStatement.date = LocalDate.parse(date);
        incomeStatement.reportedCurrency = currency;
        return incomeStatement;
    }

    public static JsonNode readUrlToTree(String symbol, String url, String cacheKey) throws IOException, FileNotFoundException, JsonProcessingException, JsonMappingException {
        String fileName = "/tmp/yahoo-" + cacheKey + "-" + symbol + ".json";
        String fileContent = "";
        File file = new File(fileName);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                fileContent = new String(fis.readAllBytes());
            }
        } else {
            String uri = url;
            fileContent = callWithRestTemplate(uri, symbol);

            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(fileContent.getBytes());
            }
        }

        JsonNode dataDeserialized = parseOutData(fileContent);
        return dataDeserialized;
    }

    private static IncomeStatement readIncomeStatement(String date, JsonNode dataDeserialized, long multiplier) throws Exception {
        IncomeStatement result = new IncomeStatement();
        ArrayNode list = (ArrayNode) dataDeserialized.get("timeseries").get("result");

        for (var entry : incomeStatementMapping) {
            String key = entry.key;
            String fieldName = entry.mapping.field;

            double foundValue = findValue(list, key, date);

            updateSingleData(result, fieldName, foundValue);
        }

        return result;
    }

    private static CashFlow readCashFlow(String date, JsonNode dataDeserialized, long multiplier) throws Exception {
        CashFlow result = new CashFlow();
        ArrayNode list = (ArrayNode) dataDeserialized.get("timeseries").get("result");

        for (var entry : cashFlowStatementMapping) {
            String key = entry.key;
            String fieldName = entry.mapping.field;

            double foundValue = findValue(list, key, date);

            updateSingleData(result, fieldName, foundValue);
        }

        return result;
    }

    private static BalanceSheet readBalanceSheet(String date, JsonNode dataDeserialized, long multiplier) throws Exception {
        BalanceSheet result = new BalanceSheet();
        ArrayNode list = (ArrayNode) dataDeserialized.get("timeseries").get("result");

        for (var entry : balanceSheetMapping) {
            String key = entry.key;
            String fieldName = entry.mapping.field;

            double foundValue = findValue(list, key, date);

            updateSingleData(result, fieldName, foundValue);
        }

        return result;
    }

    private static double findValue(ArrayNode list, String key, String date) {
        for (var element : list) {
            ArrayNode node = (ArrayNode) element.get(key);

            if (node != null) {
                JsonNode lastElement = null;
                for (var asd : node) {
                    if (asd.get("asOfDate").asText().equals(date)) {
                        lastElement = asd;
                    }
                }
                if (lastElement == null) {
                    return 0.0;
                }

                return lastElement.get("reportedValue").get("raw").asDouble();
            }
        }

        return 0.0;
    }

    public static String callWithRestTemplate(String uri, String symbol) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("user-agent", "Mozilla/5.0 (Linux; Android 10; SM-G996U Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Mobile Safari/537.36");
        headers.set("Accept", "application/json");
        headers.set("origin", "https://finance.yahoo.com");
        headers.set("referer", "https://finance.yahoo.com/quote/" + symbol + "/financials/?guccounter=1");
        headers.set("accept-language", "en-US,en;q=0.9");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        HttpEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        return response.getBody();
    }

    private static String getCashFlowUri(String symbol) {
        String result = "https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}?merge=false&padTimeSeries=true&period1=493590046&period2={currentTimestamp}&"
                + "type=quarterlyForeignSales,trailingForeignSales,quarterlyDomesticSales,trailingDomesticSales,quarterlyAdjustedGeographySegmentData,trailingAdjustedGeographySegmentData,quarterlyFreeCashFlow,"
                + "trailingFreeCashFlow,quarterlyRepurchaseOfCapitalStock,trailingRepurchaseOfCapitalStock,quarterlyRepaymentOfDebt,trailingRepaymentOfDebt,quarterlyIssuanceOfDebt,trailingIssuanceOfDebt,"
                + "quarterlyIssuanceOfCapitalStock,trailingIssuanceOfCapitalStock,quarterlyCapitalExpenditure,trailingCapitalExpenditure,quarterlyInterestPaidSupplementalData,trailingInterestPaidSupplementalData,"
                + "quarterlyIncomeTaxPaidSupplementalData,trailingIncomeTaxPaidSupplementalData,quarterlyEndCashPosition,trailingEndCashPosition,quarterlyOtherCashAdjustmentOutsideChangeinCash,"
                + "trailingOtherCashAdjustmentOutsideChangeinCash,quarterlyBeginningCashPosition,trailingBeginningCashPosition,quarterlyEffectOfExchangeRateChanges,trailingEffectOfExchangeRateChanges,"
                + "quarterlyChangesInCash,trailingChangesInCash,quarterlyOtherCashAdjustmentInsideChangeinCash,trailingOtherCashAdjustmentInsideChangeinCash,quarterlyCashFlowFromDiscontinuedOperation,"
                + "trailingCashFlowFromDiscontinuedOperation,quarterlyFinancingCashFlow,trailingFinancingCashFlow,quarterlyCashFromDiscontinuedFinancingActivities,trailingCashFromDiscontinuedFinancingActivities,"
                + "quarterlyCashFlowFromContinuingFinancingActivities,trailingCashFlowFromContinuingFinancingActivities,quarterlyNetOtherFinancingCharges,trailingNetOtherFinancingCharges,quarterlyInterestPaidCFF,"
                + "trailingInterestPaidCFF,quarterlyProceedsFromStockOptionExercised,trailingProceedsFromStockOptionExercised,quarterlyCashDividendsPaid,trailingCashDividendsPaid,quarterlyPreferredStockDividendPaid,"
                + "trailingPreferredStockDividendPaid,quarterlyCommonStockDividendPaid,trailingCommonStockDividendPaid,quarterlyNetPreferredStockIssuance,trailingNetPreferredStockIssuance,quarterlyPreferredStockPayments,"
                + "trailingPreferredStockPayments,quarterlyPreferredStockIssuance,trailingPreferredStockIssuance,quarterlyNetCommonStockIssuance,trailingNetCommonStockIssuance,quarterlyCommonStockPayments,"
                + "trailingCommonStockPayments,quarterlyCommonStockIssuance,trailingCommonStockIssuance,quarterlyNetIssuancePaymentsOfDebt,trailingNetIssuancePaymentsOfDebt,quarterlyNetShortTermDebtIssuance,"
                + "trailingNetShortTermDebtIssuance,quarterlyShortTermDebtPayments,trailingShortTermDebtPayments,quarterlyShortTermDebtIssuance,trailingShortTermDebtIssuance,quarterlyNetLongTermDebtIssuance,"
                + "trailingNetLongTermDebtIssuance,quarterlyLongTermDebtPayments,trailingLongTermDebtPayments,quarterlyLongTermDebtIssuance,trailingLongTermDebtIssuance,quarterlyInvestingCashFlow,trailingInvestingCashFlow,"
                + "quarterlyCashFromDiscontinuedInvestingActivities,trailingCashFromDiscontinuedInvestingActivities,quarterlyCashFlowFromContinuingInvestingActivities,trailingCashFlowFromContinuingInvestingActivities,"
                + "quarterlyNetOtherInvestingChanges,trailingNetOtherInvestingChanges,quarterlyInterestReceivedCFI,trailingInterestReceivedCFI,quarterlyDividendsReceivedCFI,trailingDividendsReceivedCFI,"
                + "quarterlyNetInvestmentPurchaseAndSale,trailingNetInvestmentPurchaseAndSale,quarterlySaleOfInvestment,trailingSaleOfInvestment,quarterlyPurchaseOfInvestment,trailingPurchaseOfInvestment,"
                + "quarterlyNetInvestmentPropertiesPurchaseAndSale,trailingNetInvestmentPropertiesPurchaseAndSale,quarterlySaleOfInvestmentProperties,trailingSaleOfInvestmentProperties,"
                + "quarterlyPurchaseOfInvestmentProperties,trailingPurchaseOfInvestmentProperties,quarterlyNetBusinessPurchaseAndSale,trailingNetBusinessPurchaseAndSale,quarterlySaleOfBusiness,trailingSaleOfBusiness,"
                + "quarterlyPurchaseOfBusiness,trailingPurchaseOfBusiness,quarterlyNetIntangiblesPurchaseAndSale,trailingNetIntangiblesPurchaseAndSale,quarterlySaleOfIntangibles,trailingSaleOfIntangibles,"
                + "quarterlyPurchaseOfIntangibles,trailingPurchaseOfIntangibles,quarterlyNetPPEPurchaseAndSale,trailingNetPPEPurchaseAndSale,quarterlySaleOfPPE,trailingSaleOfPPE,quarterlyPurchaseOfPPE,"
                + "trailingPurchaseOfPPE,quarterlyCapitalExpenditureReported,trailingCapitalExpenditureReported,quarterlyOperatingCashFlow,trailingOperatingCashFlow,quarterlyCashFromDiscontinuedOperatingActivities,"
                + "trailingCashFromDiscontinuedOperatingActivities,quarterlyCashFlowFromContinuingOperatingActivities,trailingCashFlowFromContinuingOperatingActivities,quarterlyTaxesRefundPaid,trailingTaxesRefundPaid,"
                + "quarterlyInterestReceivedCFO,trailingInterestReceivedCFO,quarterlyInterestPaidCFO,trailingInterestPaidCFO,quarterlyDividendReceivedCFO,trailingDividendReceivedCFO,quarterlyDividendPaidCFO,"
                + "trailingDividendPaidCFO,quarterlyChangeInWorkingCapital,trailingChangeInWorkingCapital,quarterlyChangeInOtherWorkingCapital,trailingChangeInOtherWorkingCapital,quarterlyChangeInOtherCurrentLiabilities,"
                + "trailingChangeInOtherCurrentLiabilities,quarterlyChangeInOtherCurrentAssets,trailingChangeInOtherCurrentAssets,quarterlyChangeInPayablesAndAccruedExpense,trailingChangeInPayablesAndAccruedExpense,"
                + "quarterlyChangeInAccruedExpense,trailingChangeInAccruedExpense,quarterlyChangeInInterestPayable,trailingChangeInInterestPayable,quarterlyChangeInPayable,trailingChangeInPayable,"
                + "quarterlyChangeInDividendPayable,trailingChangeInDividendPayable,quarterlyChangeInAccountPayable,trailingChangeInAccountPayable,quarterlyChangeInTaxPayable,trailingChangeInTaxPayable,"
                + "quarterlyChangeInIncomeTaxPayable,trailingChangeInIncomeTaxPayable,quarterlyChangeInPrepaidAssets,trailingChangeInPrepaidAssets,quarterlyChangeInInventory,trailingChangeInInventory,"
                + "quarterlyChangeInReceivables,trailingChangeInReceivables,quarterlyChangesInAccountReceivables,trailingChangesInAccountReceivables,quarterlyOtherNonCashItems,trailingOtherNonCashItems,"
                + "quarterlyExcessTaxBenefitFromStockBasedCompensation,trailingExcessTaxBenefitFromStockBasedCompensation,quarterlyStockBasedCompensation,trailingStockBasedCompensation,"
                + "quarterlyUnrealizedGainLossOnInvestmentSecurities,trailingUnrealizedGainLossOnInvestmentSecurities,quarterlyProvisionandWriteOffofAssets,trailingProvisionandWriteOffofAssets,"
                + "quarterlyAssetImpairmentCharge,trailingAssetImpairmentCharge,quarterlyAmortizationOfSecurities,trailingAmortizationOfSecurities,quarterlyDeferredTax,trailingDeferredTax,"
                + "quarterlyDeferredIncomeTax,trailingDeferredIncomeTax,quarterlyDepreciationAmortizationDepletion,trailingDepreciationAmortizationDepletion,quarterlyDepletion,trailingDepletion,"
                + "quarterlyDepreciationAndAmortization,trailingDepreciationAndAmortization,quarterlyAmortizationCashFlow,trailingAmortizationCashFlow,quarterlyAmortizationOfIntangibles,"
                + "trailingAmortizationOfIntangibles,quarterlyDepreciation,trailingDepreciation,quarterlyOperatingGainsLosses,trailingOperatingGainsLosses,quarterlyPensionAndEmployeeBenefitExpense,"
                + "trailingPensionAndEmployeeBenefitExpense,quarterlyEarningsLossesFromEquityInvestments,trailingEarningsLossesFromEquityInvestments,quarterlyGainLossOnInvestmentSecurities,"
                + "trailingGainLossOnInvestmentSecurities,quarterlyNetForeignCurrencyExchangeGainLoss,trailingNetForeignCurrencyExchangeGainLoss,quarterlyGainLossOnSaleOfPPE,trailingGainLossOnSaleOfPPE,"
                + "quarterlyGainLossOnSaleOfBusiness,trailingGainLossOnSaleOfBusiness,quarterlyNetIncomeFromContinuingOperations,trailingNetIncomeFromContinuingOperations,"
                + "quarterlyCashFlowsfromusedinOperatingActivitiesDirect,trailingCashFlowsfromusedinOperatingActivitiesDirect,quarterlyTaxesRefundPaidDirect,trailingTaxesRefundPaidDirect,"
                + "quarterlyInterestReceivedDirect,trailingInterestReceivedDirect,quarterlyInterestPaidDirect,trailingInterestPaidDirect,quarterlyDividendsReceivedDirect,trailingDividendsReceivedDirect,"
                + "quarterlyDividendsPaidDirect,trailingDividendsPaidDirect,quarterlyClassesofCashPayments,trailingClassesofCashPayments,quarterlyOtherCashPaymentsfromOperatingActivities,"
                + "trailingOtherCashPaymentsfromOperatingActivities,quarterlyPaymentsonBehalfofEmployees,trailingPaymentsonBehalfofEmployees,quarterlyPaymentstoSuppliersforGoodsandServices,"
                + "trailingPaymentstoSuppliersforGoodsandServices,quarterlyClassesofCashReceiptsfromOperatingActivities,trailingClassesofCashReceiptsfromOperatingActivities,quarterlyOtherCashReceiptsfromOperatingActivities,"
                + "trailingOtherCashReceiptsfromOperatingActivities,quarterlyReceiptsfromGovernmentGrants,trailingReceiptsfromGovernmentGrants,quarterlyReceiptsfromCustomers,trailingReceiptsfromCustomers&lang=en-US&region=US";

        long now = Instant.now().toEpochMilli() / 1000L;

        result = result.replace("{symbol}", symbol);
        result = result.replace("{currentTimestamp}", String.valueOf(now));

        return result;
    }

    public static String getIncomeStatementUri(String symbol) {
        String result = "https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/{symbol}?merge=false&padTimeSeries=true&period1=493590046&period2={currentTimestamp}&"
                + "type=quarterlyTaxEffectOfUnusualItems,trailingTaxEffectOfUnusualItems,quarterlyTaxRateForCalcs,trailingTaxRateForCalcs,quarterlyNormalizedEBITDA,trailingNormalizedEBITDA,"
                + "quarterlyNormalizedDilutedEPS,trailingNormalizedDilutedEPS,quarterlyNormalizedBasicEPS,trailingNormalizedBasicEPS,quarterlyTotalUnusualItems,trailingTotalUnusualItems,"
                + "quarterlyTotalUnusualItemsExcludingGoodwill,trailingTotalUnusualItemsExcludingGoodwill,quarterlyNetIncomeFromContinuingOperationNetMinorityInterest,"
                + "trailingNetIncomeFromContinuingOperationNetMinorityInterest,quarterlyReconciledDepreciation,trailingReconciledDepreciation,quarterlyReconciledCostOfRevenue,"
                + "trailingReconciledCostOfRevenue,quarterlyEBITDA,trailingEBITDA,quarterlyEBIT,trailingEBIT,quarterlyNetInterestIncome,trailingNetInterestIncome,"
                + "quarterlyInterestExpense,trailingInterestExpense,quarterlyInterestIncome,trailingInterestIncome,quarterlyContinuingAndDiscontinuedDilutedEPS,"
                + "trailingContinuingAndDiscontinuedDilutedEPS,quarterlyContinuingAndDiscontinuedBasicEPS,trailingContinuingAndDiscontinuedBasicEPS,quarterlyNormalizedIncome,"
                + "trailingNormalizedIncome,quarterlyNetIncomeFromContinuingAndDiscontinuedOperation,trailingNetIncomeFromContinuingAndDiscontinuedOperation,quarterlyTotalExpenses,"
                + "trailingTotalExpenses,quarterlyRentExpenseSupplemental,trailingRentExpenseSupplemental,quarterlyReportedNormalizedDilutedEPS,trailingReportedNormalizedDilutedEPS,"
                + "quarterlyReportedNormalizedBasicEPS,trailingReportedNormalizedBasicEPS,quarterlyTotalOperatingIncomeAsReported,trailingTotalOperatingIncomeAsReported,quarterlyDividendPerShare,"
                + "trailingDividendPerShare,quarterlyDilutedAverageShares,trailingDilutedAverageShares,quarterlyBasicAverageShares,trailingBasicAverageShares,quarterlyDilutedEPS,trailingDilutedEPS%2"
                + "CquarterlyDilutedEPSOtherGainsLosses,trailingDilutedEPSOtherGainsLosses,quarterlyTaxLossCarryforwardDilutedEPS,trailingTaxLossCarryforwardDilutedEPS,quarterlyDilutedAccountingChange,"
                + "trailingDilutedAccountingChange,quarterlyDilutedExtraordinary,trailingDilutedExtraordinary,quarterlyDilutedDiscontinuousOperations,trailingDilutedDiscontinuousOperations,"
                + "quarterlyDilutedContinuousOperations,trailingDilutedContinuousOperations,quarterlyBasicEPS,trailingBasicEPS,quarterlyBasicEPSOtherGainsLosses,trailingBasicEPSOtherGainsLosses,"
                + "quarterlyTaxLossCarryforwardBasicEPS,trailingTaxLossCarryforwardBasicEPS,quarterlyBasicAccountingChange,trailingBasicAccountingChange,quarterlyBasicExtraordinary,"
                + "trailingBasicExtraordinary,quarterlyBasicDiscontinuousOperations,trailingBasicDiscontinuousOperations,quarterlyBasicContinuousOperations,trailingBasicContinuousOperations,"
                + "quarterlyDilutedNIAvailtoComStockholders,trailingDilutedNIAvailtoComStockholders,quarterlyAverageDilutionEarnings,trailingAverageDilutionEarnings,"
                + "quarterlyNetIncomeCommonStockholders,trailingNetIncomeCommonStockholders,quarterlyOtherunderPreferredStockDividend,trailingOtherunderPreferredStockDividend,"
                + "quarterlyPreferredStockDividends,trailingPreferredStockDividends,quarterlyNetIncome,trailingNetIncome,quarterlyMinorityInterests,trailingMinorityInterests,"
                + "quarterlyNetIncomeIncludingNoncontrollingInterests,trailingNetIncomeIncludingNoncontrollingInterests,quarterlyNetIncomeFromTaxLossCarryforward,trailingNetIncomeFromTaxLossCarryforward,"
                + "quarterlyNetIncomeExtraordinary,trailingNetIncomeExtraordinary,quarterlyNetIncomeDiscontinuousOperations,trailingNetIncomeDiscontinuousOperations,quarterlyNetIncomeContinuousOperations,"
                + "trailingNetIncomeContinuousOperations,quarterlyEarningsFromEquityInterestNetOfTax,trailingEarningsFromEquityInterestNetOfTax,quarterlyTaxProvision,"
                + "trailingTaxProvision,quarterlyPretaxIncome,trailingPretaxIncome,quarterlyOtherIncomeExpense,trailingOtherIncomeExpense,quarterlyOtherNonOperatingIncomeExpenses,"
                + "trailingOtherNonOperatingIncomeExpenses,quarterlySpecialIncomeCharges,trailingSpecialIncomeCharges,quarterlyGainOnSaleOfPPE,trailingGainOnSaleOfPPE,quarterlyGainOnSaleOfBusiness,"
                + "trailingGainOnSaleOfBusiness,quarterlyOtherSpecialCharges,trailingOtherSpecialCharges,quarterlyWriteOff,trailingWriteOff,quarterlyImpairmentOfCapitalAssets,"
                + "trailingImpairmentOfCapitalAssets,quarterlyRestructuringAndMergernAcquisition,trailingRestructuringAndMergernAcquisition,"
                + "quarterlySecuritiesAmortization,trailingSecuritiesAmortization,quarterlyEarningsFromEquityInterest,trailingEarningsFromEquityInterest,quarterlyGainOnSaleOfSecurity,"
                + "trailingGainOnSaleOfSecurity,quarterlyNetNonOperatingInterestIncomeExpense,trailingNetNonOperatingInterestIncomeExpense,quarterlyTotalOtherFinanceCost,"
                + "trailingTotalOtherFinanceCost,quarterlyInterestExpenseNonOperating,trailingInterestExpenseNonOperating,quarterlyInterestIncomeNonOperating,"
                + "trailingInterestIncomeNonOperating,quarterlyOperatingIncome,trailingOperatingIncome,quarterlyOperatingExpense,trailingOperatingExpense,"
                + "quarterlyOtherOperatingExpenses,trailingOtherOperatingExpenses,quarterlyOtherTaxes,trailingOtherTaxes,quarterlyProvisionForDoubtfulAccounts,"
                + "trailingProvisionForDoubtfulAccounts,quarterlyDepreciationAmortizationDepletionIncomeStatement,trailingDepreciationAmortizationDepletionIncomeStatement,"
                + "quarterlyDepletionIncomeStatement,trailingDepletionIncomeStatement,quarterlyDepreciationAndAmortizationInIncomeStatement,trailingDepreciationAndAmortizationInIncomeStatement,"
                + "quarterlyAmortization,trailingAmortization,quarterlyAmortizationOfIntangiblesIncomeStatement,trailingAmortizationOfIntangiblesIncomeStatement,quarterlyDepreciationIncomeStatement,"
                + "trailingDepreciationIncomeStatement,quarterlyResearchAndDevelopment,trailingResearchAndDevelopment,quarterlySellingGeneralAndAdministration,trailingSellingGeneralAndAdministration,"
                + "quarterlySellingAndMarketingExpense,trailingSellingAndMarketingExpense,quarterlyGeneralAndAdministrativeExpense,trailingGeneralAndAdministrativeExpense,"
                + "quarterlyOtherGandA,trailingOtherGandA,quarterlyInsuranceAndClaims,trailingInsuranceAndClaims,quarterlyRentAndLandingFees,trailingRentAndLandingFees,"
                + "quarterlySalariesAndWages,trailingSalariesAndWages,quarterlyGrossProfit,trailingGrossProfit,quarterlyCostOfRevenue,trailingCostOfRevenue,quarterlyTotalRevenue,"
                + "trailingTotalRevenue,quarterlyExciseTaxes,trailingExciseTaxes,quarterlyOperatingRevenue,trailingOperatingRevenue&lang=en-US&region=US";

        long now = Instant.now().toEpochMilli() / 1000L;

        result = result.replace("{symbol}", symbol);
        result = result.replace("{currentTimestamp}", String.valueOf(now));

        return result;
    }

    public static String getBalanceSheetUri(String symbol) {
        String result = "https://query1.finance.yahoo.com/ws/fundamentals-timeseries/v1/finance/timeseries/UNH?merge=false&padTimeSeries=true&period1=493590046&period2=1756580399&type=quarterlyTreasurySharesNumber,"
                + "quarterlyPreferredSharesNumber,quarterlyOrdinarySharesNumber,quarterlyShareIssued,quarterlyNetDebt,quarterlyTotalDebt,quarterlyTangibleBookValue,quarterlyInvestedCapital,quarterlyWorkingCapital,"
                + "quarterlyNetTangibleAssets,quarterlyCapitalLeaseObligations,quarterlyCommonStockEquity,quarterlyPreferredStockEquity,quarterlyTotalCapitalization,quarterlyTotalEquityGrossMinorityInterest,"
                + "quarterlyMinorityInterest,quarterlyStockholdersEquity,quarterlyOtherEquityInterest,quarterlyGainsLossesNotAffectingRetainedEarnings,quarterlyOtherEquityAdjustments,quarterlyFixedAssetsRevaluationReserve,"
                + "quarterlyForeignCurrencyTranslationAdjustments,quarterlyMinimumPensionLiabilities,quarterlyUnrealizedGainLoss,quarterlyTreasuryStock,quarterlyRetainedEarnings,quarterlyAdditionalPaidInCapital,"
                + "quarterlyCapitalStock,quarterlyOtherCapitalStock,quarterlyCommonStock,quarterlyPreferredStock,quarterlyTotalPartnershipCapital,quarterlyGeneralPartnershipCapital,quarterlyLimitedPartnershipCapital,"
                + "quarterlyTotalLiabilitiesNetMinorityInterest,quarterlyTotalNonCurrentLiabilitiesNetMinorityInterest,quarterlyOtherNonCurrentLiabilities,quarterlyLiabilitiesHeldforSaleNonCurrent,"
                + "quarterlyRestrictedCommonStock,quarterlyPreferredSecuritiesOutsideStockEquity,quarterlyDerivativeProductLiabilities,quarterlyEmployeeBenefits,quarterlyNonCurrentPensionAndOtherPostretirementBenefitPlans,"
                + "quarterlyNonCurrentAccruedExpenses,quarterlyDuetoRelatedPartiesNonCurrent,quarterlyTradeandOtherPayablesNonCurrent,quarterlyNonCurrentDeferredLiabilities,quarterlyNonCurrentDeferredRevenue,"
                + "quarterlyNonCurrentDeferredTaxesLiabilities,quarterlyLongTermDebtAndCapitalLeaseObligation,quarterlyLongTermCapitalLeaseObligation,quarterlyLongTermDebt,quarterlyLongTermProvisions,"
                + "quarterlyCurrentLiabilities,quarterlyOtherCurrentLiabilities,quarterlyCurrentDeferredLiabilities,quarterlyCurrentDeferredRevenue,quarterlyCurrentDeferredTaxesLiabilities,"
                + "quarterlyCurrentDebtAndCapitalLeaseObligation,quarterlyCurrentCapitalLeaseObligation,quarterlyCurrentDebt,quarterlyOtherCurrentBorrowings,quarterlyLineOfCredit,quarterlyCommercialPaper,"
                + "quarterlyCurrentNotesPayable,quarterlyPensionandOtherPostRetirementBenefitPlansCurrent,quarterlyCurrentProvisions,quarterlyPayablesAndAccruedExpenses,quarterlyCurrentAccruedExpenses,"
                + "quarterlyInterestPayable,quarterlyPayables,quarterlyOtherPayable,quarterlyDuetoRelatedPartiesCurrent,quarterlyDividendsPayable,quarterlyTotalTaxPayable,quarterlyIncomeTaxPayable,"
                + "quarterlyAccountsPayable,quarterlyTotalAssets,quarterlyTotalNonCurrentAssets,quarterlyOtherNonCurrentAssets,quarterlyDefinedPensionBenefit,quarterlyNonCurrentPrepaidAssets,"
                + "quarterlyNonCurrentDeferredAssets,quarterlyNonCurrentDeferredTaxesAssets,quarterlyDuefromRelatedPartiesNonCurrent,quarterlyNonCurrentNoteReceivables,quarterlyNonCurrentAccountsReceivable,"
                + "quarterlyFinancialAssets,quarterlyInvestmentsAndAdvances,quarterlyOtherInvestments,quarterlyInvestmentinFinancialAssets,quarterlyHeldToMaturitySecurities,quarterlyAvailableForSaleSecurities,"
                + "quarterlyFinancialAssetsDesignatedasFairValueThroughProfitorLossTotal,quarterlyTradingSecurities,quarterlyLongTermEquityInvestment,quarterlyInvestmentsinJointVenturesatCost,"
                + "quarterlyInvestmentsInOtherVenturesUnderEquityMethod,quarterlyInvestmentsinAssociatesatCost,quarterlyInvestmentsinSubsidiariesatCost,quarterlyInvestmentProperties,"
                + "quarterlyGoodwillAndOtherIntangibleAssets,quarterlyOtherIntangibleAssets,quarterlyGoodwill,quarterlyNetPPE,quarterlyAccumulatedDepreciation,quarterlyGrossPPE,quarterlyLeases,"
                + "quarterlyConstructionInProgress,quarterlyOtherProperties,quarterlyMachineryFurnitureEquipment,quarterlyBuildingsAndImprovements,quarterlyLandAndImprovements,quarterlyProperties,"
                + "quarterlyCurrentAssets,quarterlyOtherCurrentAssets,quarterlyHedgingAssetsCurrent,quarterlyAssetsHeldForSaleCurrent,quarterlyCurrentDeferredAssets,quarterlyCurrentDeferredTaxesAssets,"
                + "quarterlyRestrictedCash,quarterlyPrepaidAssets,quarterlyInventory,quarterlyInventoriesAdjustmentsAllowances,quarterlyOtherInventories,quarterlyFinishedGoods,quarterlyWorkInProcess,"
                + "quarterlyRawMaterials,quarterlyReceivables,quarterlyReceivablesAdjustmentsAllowances,quarterlyOtherReceivables,quarterlyDuefromRelatedPartiesCurrent,quarterlyTaxesReceivable,"
                + "quarterlyAccruedInterestReceivable,quarterlyNotesReceivable,quarterlyLoansReceivable,quarterlyAccountsReceivable,quarterlyAllowanceForDoubtfulAccountsReceivable,"
                + "quarterlyGrossAccountsReceivable,quarterlyCashCashEquivalentsAndShortTermInvestments,quarterlyOtherShortTermInvestments,quarterlyCashAndCashEquivalents,quarterlyCashEquivalents,"
                + "quarterlyCashFinancial&lang=en-US&region=US";

        long now = Instant.now().toEpochMilli() / 1000L;

        result = result.replace("{symbol}", symbol);
        result = result.replace("{currentTimestamp}", String.valueOf(now));

        return result;
    }

    public static JsonNode parseOutData(String fileContent) throws JsonProcessingException, JsonMappingException {
        ObjectMapper om = new ObjectMapper();
        return om.readTree(fileContent);
    }

    private static void updateSingleData(Object incomeStatement, String fieldName, double value) throws Exception {
        Field field = incomeStatement.getClass().getField(fieldName);
        field.setAccessible(true);
        var type = field.getType();
        if (type.equals(long.class)) {
            field.set(incomeStatement, (long) (value));
        }
        if (type.equals(double.class)) {
            field.set(incomeStatement, value);
        }

    }

    private static void updateData(Object incomeStatement, Map<String, FieldMapping> mapping, String fieldName, String data, String dateKey, long multiplier) throws Exception {

        for (var entry : mapping.entrySet()) {
            if (fieldName.contains(entry.getKey())) {
                FieldMapping setterFieldName = entry.getValue();
                Field field = incomeStatement.getClass().getField(setterFieldName.field);
                field.setAccessible(true);
                var type = field.getType();

                long multiplierToUse = setterFieldName.useMultiplier ? multiplier : 1;

                double dataDouble;
                if (!data.isEmpty()) {
                    dataDouble = Double.parseDouble(data);
                } else {
                    dataDouble = 0.0;
                }
                if (type.equals(long.class)) {
                    field.set(incomeStatement, (long) (dataDouble * multiplierToUse));
                }
                if (type.equals(double.class)) {
                    field.set(incomeStatement, dataDouble * multiplierToUse);
                }
            }
        }
    }

    static class FieldMapping {
        String field;
        boolean useMultiplier = true;

        public FieldMapping(String field) {
            this.field = field;
        }

        public FieldMapping(String field, boolean useMultiplier) {
            this.field = field;
            this.useMultiplier = useMultiplier;
        }

    }

    static class MacroTrendData {
        String field_name;
    }

    static class Pair {
        public String key;
        public FieldMapping mapping;

        public Pair(String key, FieldMapping mapping) {
            this.key = key;
            this.mapping = mapping;
        }

        @Override
        public String toString() {
            return "Pair [key=" + key + ", mapping=" + mapping + "]";
        }

    }

    static class PriceToCurrencyPair {
        String currency;
        List<SimpleDateDataElement> prices;

        public PriceToCurrencyPair(String currency, List<SimpleDateDataElement> prices) {
            this.currency = currency;
            this.prices = prices;
        }

        @Override
        public String toString() {
            return "PriceToCurrencyPair [currency=" + currency + ", prices=" + prices + "]";
        }

    }

    static class StatementMetadata {
        List<String> availableDates;
        String reportingCurrency;

        public StatementMetadata(List<String> availableDates, String reportingCurrency) {
            this.availableDates = availableDates;
            this.reportingCurrency = reportingCurrency;
        }

        @Override
        public String toString() {
            return "StatementMetadata [availableDates=" + availableDates + ", reportingCurrency=" + reportingCurrency + "]";
        }

    }

}
