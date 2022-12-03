package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class IncomeStatement implements DateAware, Serializable {
    public LocalDate date; //2022-09-30,
    public String reportedCurrency; //USD,
    public String period; //Q4,
    public long revenue; //90146000000,
    public long costOfRevenue; //52051000000,
    public long grossProfit; //38095000000,
    @NoTtmNeeded
    public double grossProfitRatio; //0.4225922392563175,
    public long researchAndDevelopmentExpenses; //6761000000,
    public long generalAndAdministrativeExpenses; //0.0,
    public long sellingAndMarketingExpenses; //0.0,
    public long sellingGeneralAndAdministrativeExpenses; //6440000000,
    public long otherExpenses; //0.0,
    public long operatingExpenses; //13201000000,
    public long costAndExpenses; //65252000000,
    public long interestIncome; //753000000,
    public long interestExpense; //827000000,
    public long depreciationAndAmortization; //2865000000,
    public long ebitda; //28349000000,
    @NoTtmNeeded
    public double ebitdaratio; //0.3144787344973709,
    public long operatingIncome; //24894000000,
    @NoTtmNeeded
    public double operatingIncomeRatio; //0.27615202005635303,
    public long totalOtherIncomeExpensesNet; //-237000000,
    public long incomeBeforeTax; //24657000000,
    @NoTtmNeeded
    public long incomeBeforeTaxRatio; //0.27352295165620216,
    public long incomeTaxExpense; //3936000000,
    public long netIncome; //20721000000,
    @NoTtmNeeded
    public double netIncomeRatio; //0.22986044860559537,
    public double eps; //1.29,
    public double epsdiluted; //1.29,
    @NoTtmNeeded
    public long weightedAverageShsOut; //16030382000,
    @NoTtmNeeded
    public long weightedAverageShsOutDil; //16118465000,

    @Override
    public LocalDate getDate() {
        return date;
    }
}
