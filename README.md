## Financial data application

Used to visualize financial fundamental data, as well as it contains many other utilities to screen and backtest investment strategies.

![screenshot](images/screenshot.png)

### Usage

First you need to download the fundamental data. You only need to do this once (and every once in a while to update the data):

 - You need a license to https://site.financialmodelingprep.com/developer/docs/ to download the financial fundamental data
 - Run com.helospark.financialdata.util.StockDataDownloader as Java application, and give `-DAPI_KEY=YOUR_API_KEY` as VM argument
    - You may want to filter what to download by commenting out unnecessary symbols. By default it downloads all known stocks (about 40 000 stocks, ~20GB).

Once you have the data, run `com.helospark.financialdata.FinancialDataApplication` as Java application. You will then be able to reach the UI on http://localhost:8080/stock/INTC

You can change the stock ticket in the URI to see different stocks.


### Utils

There are utils under `com.helospark.financialdata.util`, these are separate applications.

 - StockDataDownload - Downloads data from financialmodelingprep
 - CompanyScreener - Find stocks with certain properties or does backtest. You can find multiple implementations under analyzer package.
 
 
### Dev notes

Generate new JWT signing keys

    openssl genrsa -out jwt2.pem 2048
    openssl rsa -in jwt2.pem -pubout > jwt2.pub

