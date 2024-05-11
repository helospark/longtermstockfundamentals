package com.helospark.financialdata.util.spconstituents;

public class PortfolioCompareData {
    public String name;
    public String sp;
    public String port;
    public boolean spWinner;
    public boolean rightSeparator;

    public PortfolioCompareData(String name, String sp, String port, boolean spWinner, boolean rightSeparator) {
        this.name = name;
        this.sp = sp;
        this.port = port;
        this.spWinner = spWinner;
        this.rightSeparator = rightSeparator;
    }

}
