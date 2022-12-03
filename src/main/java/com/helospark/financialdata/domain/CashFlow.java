package com.helospark.financialdata.domain;

import java.io.Serializable;
import java.time.LocalDate;

public class CashFlow implements DateAware, Serializable {
    public LocalDate date; //2022-09-30,
    public String reportedCurrency; //USD,
    public String period; //Q4,
    public long netIncome; //5492000.0,
    public long depreciationAndAmortization; //1729000.0,
    public long deferredIncomeTax; //-38493000,
    public long stockBasedCompensation; //7821000.0,
    public long changeInWorkingCapital; //33001000,
    public long accountsReceivables; //7218000.0,
    public long inventory; //-13058000,
    public long accountsPayables; //14329000,
    public long otherWorkingCapital; //0.0,
    public long otherNonCashItems; //-26772000,
    public long netCashProvidedByOperatingActivities; //-17222000,
    public long investmentsInPropertyPlantAndEquipment; //-11356000,
    public long acquisitionsNet; //0.0,
    public long purchasesOfInvestments; //0.0,
    public long salesMaturitiesOfInvestments; //0.0,
    public long otherInvestingActivites; //-3117000.0,
    public long netCashUsedForInvestingActivites; //-14473000,
    public long debtRepayment; //0.0,
    public long commonStockIssued; //0.0,
    public long commonStockRepurchased; //0.0,
    public long dividendsPaid; //0.0,
    public long otherFinancingActivites; //18368000,
    public long netCashUsedProvidedByFinancingActivities; //18368000,
    public long effectOfForexChangesOnCash; //-182000.0,
    public long netChangeInCash; //-13509000,
    public long cashAtEndOfPeriod; //14220000,
    public long cashAtBeginningOfPeriod; //27729000,
    public long operatingCashFlow; //-17222000,
    public long capitalExpenditure; //18758000,
    public long freeCashFlow; //1536000.0,

    @Override
    public LocalDate getDate() {
        return date;
    }
}
