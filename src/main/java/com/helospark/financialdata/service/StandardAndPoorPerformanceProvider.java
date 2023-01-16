package com.helospark.financialdata.service;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.helospark.financialdata.CommonConfig;
import com.helospark.financialdata.domain.HistoricalPriceElement;

public class StandardAndPoorPerformanceProvider {
    public static List<HistoricalPriceElement> prices = new ArrayList<>();
    public static Map<Integer, Double> dividendsPaidPerShare = new HashMap<>();

    static {
        prices = DataLoader.loadHistoricalFile(new File(CommonConfig.BASE_FOLDER + "/info/s&p500_price.json"));

        // source: https://www.multpl.com/s-p-500-dividend-yield/table/by-year
        dividendsPaidPerShare.put(2022, calculateDividendsPaid(2022, 1.71));
        dividendsPaidPerShare.put(2021, calculateDividendsPaid(2021, 1.29));
        dividendsPaidPerShare.put(2020, calculateDividendsPaid(2020, 1.58));
        dividendsPaidPerShare.put(2019, calculateDividendsPaid(2019, 1.83));
        dividendsPaidPerShare.put(2018, calculateDividendsPaid(2018, 2.09));
        dividendsPaidPerShare.put(2017, calculateDividendsPaid(2017, 1.84));
        dividendsPaidPerShare.put(2016, calculateDividendsPaid(2016, 2.03));
        dividendsPaidPerShare.put(2015, calculateDividendsPaid(2015, 2.11));
        dividendsPaidPerShare.put(2014, calculateDividendsPaid(2014, 1.92));
        dividendsPaidPerShare.put(2013, calculateDividendsPaid(2013, 1.94));
        dividendsPaidPerShare.put(2012, calculateDividendsPaid(2012, 2.20));
        dividendsPaidPerShare.put(2011, calculateDividendsPaid(2011, 2.13));
        dividendsPaidPerShare.put(2010, calculateDividendsPaid(2010, 1.83));
        dividendsPaidPerShare.put(2009, calculateDividendsPaid(2009, 2.02));
        dividendsPaidPerShare.put(2008, calculateDividendsPaid(2008, 3.23));
        dividendsPaidPerShare.put(2007, calculateDividendsPaid(2007, 1.87));
        dividendsPaidPerShare.put(2006, calculateDividendsPaid(2006, 1.76));
        dividendsPaidPerShare.put(2005, calculateDividendsPaid(2005, 1.76));
        dividendsPaidPerShare.put(2004, calculateDividendsPaid(2004, 1.62));
        dividendsPaidPerShare.put(2003, calculateDividendsPaid(2003, 1.61));
        dividendsPaidPerShare.put(2002, calculateDividendsPaid(2002, 1.79));
        dividendsPaidPerShare.put(2001, calculateDividendsPaid(2001, 1.37));
        dividendsPaidPerShare.put(2000, calculateDividendsPaid(2000, 1.22));
        dividendsPaidPerShare.put(1999, calculateDividendsPaid(1999, 1.17));
        dividendsPaidPerShare.put(1998, calculateDividendsPaid(1998, 1.36));
        dividendsPaidPerShare.put(1997, calculateDividendsPaid(1997, 1.61));
        dividendsPaidPerShare.put(1996, calculateDividendsPaid(1996, 2.00));
        dividendsPaidPerShare.put(1995, calculateDividendsPaid(1995, 2.24));
        dividendsPaidPerShare.put(1994, calculateDividendsPaid(1994, 2.89));
        dividendsPaidPerShare.put(1993, calculateDividendsPaid(1993, 2.70));
        dividendsPaidPerShare.put(1992, calculateDividendsPaid(1992, 2.84));
        dividendsPaidPerShare.put(1991, calculateDividendsPaid(1991, 3.14));
        dividendsPaidPerShare.put(1990, calculateDividendsPaid(1990, 3.68));
        dividendsPaidPerShare.put(1989, calculateDividendsPaid(1989, 3.17));
        dividendsPaidPerShare.put(1988, calculateDividendsPaid(1988, 3.53));
        dividendsPaidPerShare.put(1987, calculateDividendsPaid(1987, 3.66));
        dividendsPaidPerShare.put(1986, calculateDividendsPaid(1986, 3.33));
        dividendsPaidPerShare.put(1985, calculateDividendsPaid(1985, 3.81));
        dividendsPaidPerShare.put(1984, calculateDividendsPaid(1984, 4.58));
        dividendsPaidPerShare.put(1983, calculateDividendsPaid(1983, 4.31));
        dividendsPaidPerShare.put(1982, calculateDividendsPaid(1982, 4.93));
        dividendsPaidPerShare.put(1981, calculateDividendsPaid(1981, 5.36));
        dividendsPaidPerShare.put(1980, calculateDividendsPaid(1980, 4.61));
        dividendsPaidPerShare.put(1979, calculateDividendsPaid(1979, 5.24));
        dividendsPaidPerShare.put(1978, calculateDividendsPaid(1978, 5.28));
        dividendsPaidPerShare.put(1977, calculateDividendsPaid(1977, 4.98));
        dividendsPaidPerShare.put(1976, calculateDividendsPaid(1976, 3.87));
        dividendsPaidPerShare.put(1975, calculateDividendsPaid(1975, 4.15));
        dividendsPaidPerShare.put(1974, calculateDividendsPaid(1974, 5.37));
        dividendsPaidPerShare.put(1973, calculateDividendsPaid(1973, 3.57));
        dividendsPaidPerShare.put(1972, calculateDividendsPaid(1972, 2.68));
        dividendsPaidPerShare.put(1971, calculateDividendsPaid(1971, 3.10));
        dividendsPaidPerShare.put(1970, calculateDividendsPaid(1970, 3.49));
        dividendsPaidPerShare.put(1969, calculateDividendsPaid(1969, 3.47));
        dividendsPaidPerShare.put(1968, calculateDividendsPaid(1968, 2.88));
        dividendsPaidPerShare.put(1967, calculateDividendsPaid(1967, 3.06));
        dividendsPaidPerShare.put(1966, calculateDividendsPaid(1966, 3.53));
        dividendsPaidPerShare.put(1965, calculateDividendsPaid(1965, 2.97));
        dividendsPaidPerShare.put(1964, calculateDividendsPaid(1964, 2.98));
        dividendsPaidPerShare.put(1963, calculateDividendsPaid(1963, 3.07));
        dividendsPaidPerShare.put(1962, calculateDividendsPaid(1962, 3.40));
        dividendsPaidPerShare.put(1961, calculateDividendsPaid(1961, 2.82));
        dividendsPaidPerShare.put(1960, calculateDividendsPaid(1960, 3.43));
        dividendsPaidPerShare.put(1959, calculateDividendsPaid(1959, 3.10));
        dividendsPaidPerShare.put(1958, calculateDividendsPaid(1958, 3.27));
        dividendsPaidPerShare.put(1957, calculateDividendsPaid(1957, 4.44));
        dividendsPaidPerShare.put(1956, calculateDividendsPaid(1956, 3.75));
        dividendsPaidPerShare.put(1955, calculateDividendsPaid(1955, 3.61));
        dividendsPaidPerShare.put(1954, calculateDividendsPaid(1954, 4.40));
        dividendsPaidPerShare.put(1953, calculateDividendsPaid(1953, 5.84));
        dividendsPaidPerShare.put(1952, calculateDividendsPaid(1952, 5.41));
        dividendsPaidPerShare.put(1951, calculateDividendsPaid(1951, 6.02));
        dividendsPaidPerShare.put(1950, calculateDividendsPaid(1950, 7.44));
        dividendsPaidPerShare.put(1949, calculateDividendsPaid(1949, 6.89));
        dividendsPaidPerShare.put(1948, calculateDividendsPaid(1948, 6.12));
        dividendsPaidPerShare.put(1947, calculateDividendsPaid(1947, 5.59));
        dividendsPaidPerShare.put(1946, calculateDividendsPaid(1946, 4.69));
        dividendsPaidPerShare.put(1945, calculateDividendsPaid(1945, 3.81));
        dividendsPaidPerShare.put(1944, calculateDividendsPaid(1944, 4.89));
        dividendsPaidPerShare.put(1943, calculateDividendsPaid(1943, 5.31));
        dividendsPaidPerShare.put(1942, calculateDividendsPaid(1942, 6.20));
        dividendsPaidPerShare.put(1941, calculateDividendsPaid(1941, 8.11));
        dividendsPaidPerShare.put(1940, calculateDividendsPaid(1940, 6.36));
        dividendsPaidPerShare.put(1939, calculateDividendsPaid(1939, 5.01));
        dividendsPaidPerShare.put(1938, calculateDividendsPaid(1938, 4.02));
        dividendsPaidPerShare.put(1937, calculateDividendsPaid(1937, 7.26));
        dividendsPaidPerShare.put(1936, calculateDividendsPaid(1936, 4.22));
        dividendsPaidPerShare.put(1935, calculateDividendsPaid(1935, 3.60));
        dividendsPaidPerShare.put(1934, calculateDividendsPaid(1934, 4.86));
        dividendsPaidPerShare.put(1933, calculateDividendsPaid(1933, 4.41));
        dividendsPaidPerShare.put(1932, calculateDividendsPaid(1932, 7.33));
        dividendsPaidPerShare.put(1931, calculateDividendsPaid(1931, 9.72));
        dividendsPaidPerShare.put(1930, calculateDividendsPaid(1930, 6.32));
        dividendsPaidPerShare.put(1929, calculateDividendsPaid(1929, 4.53));
        dividendsPaidPerShare.put(1928, calculateDividendsPaid(1928, 3.67));
        dividendsPaidPerShare.put(1927, calculateDividendsPaid(1927, 4.41));
        dividendsPaidPerShare.put(1926, calculateDividendsPaid(1926, 5.11));
        dividendsPaidPerShare.put(1925, calculateDividendsPaid(1925, 4.82));
        dividendsPaidPerShare.put(1924, calculateDividendsPaid(1924, 5.41));
        dividendsPaidPerShare.put(1923, calculateDividendsPaid(1923, 6.20));
        dividendsPaidPerShare.put(1922, calculateDividendsPaid(1922, 5.81));
        dividendsPaidPerShare.put(1921, calculateDividendsPaid(1921, 6.29));
        dividendsPaidPerShare.put(1920, calculateDividendsPaid(1920, 7.49));
        dividendsPaidPerShare.put(1919, calculateDividendsPaid(1919, 5.94));
        dividendsPaidPerShare.put(1918, calculateDividendsPaid(1918, 7.22));
        dividendsPaidPerShare.put(1917, calculateDividendsPaid(1917, 10.1));
        dividendsPaidPerShare.put(1916, calculateDividendsPaid(1916, 5.71));
        dividendsPaidPerShare.put(1915, calculateDividendsPaid(1915, 4.54));
        dividendsPaidPerShare.put(1914, calculateDividendsPaid(1914, 5.71));
        dividendsPaidPerShare.put(1913, calculateDividendsPaid(1913, 5.97));
        dividendsPaidPerShare.put(1912, calculateDividendsPaid(1912, 5.12));
        dividendsPaidPerShare.put(1911, calculateDividendsPaid(1911, 5.16));
        dividendsPaidPerShare.put(1910, calculateDividendsPaid(1910, 5.19));
        dividendsPaidPerShare.put(1909, calculateDividendsPaid(1909, 4.27));
        dividendsPaidPerShare.put(1908, calculateDividendsPaid(1908, 4.43));
        dividendsPaidPerShare.put(1907, calculateDividendsPaid(1907, 6.70));
        dividendsPaidPerShare.put(1906, calculateDividendsPaid(1906, 4.07));
        dividendsPaidPerShare.put(1905, calculateDividendsPaid(1905, 3.46));
        dividendsPaidPerShare.put(1904, calculateDividendsPaid(1904, 3.76));
        dividendsPaidPerShare.put(1903, calculateDividendsPaid(1903, 5.33));
        dividendsPaidPerShare.put(1902, calculateDividendsPaid(1902, 4.10));
        dividendsPaidPerShare.put(1901, calculateDividendsPaid(1901, 4.03));
        dividendsPaidPerShare.put(1900, calculateDividendsPaid(1900, 4.37));
        dividendsPaidPerShare.put(1899, calculateDividendsPaid(1899, 3.49));
        dividendsPaidPerShare.put(1898, calculateDividendsPaid(1898, 3.54));
        dividendsPaidPerShare.put(1897, calculateDividendsPaid(1897, 3.79));
        dividendsPaidPerShare.put(1896, calculateDividendsPaid(1896, 4.27));
        dividendsPaidPerShare.put(1895, calculateDividendsPaid(1895, 4.40));
        dividendsPaidPerShare.put(1894, calculateDividendsPaid(1894, 4.88));
        dividendsPaidPerShare.put(1893, calculateDividendsPaid(1893, 5.67));
        dividendsPaidPerShare.put(1892, calculateDividendsPaid(1892, 4.36));
        dividendsPaidPerShare.put(1891, calculateDividendsPaid(1891, 4.07));
        dividendsPaidPerShare.put(1890, calculateDividendsPaid(1890, 4.78));
        dividendsPaidPerShare.put(1889, calculateDividendsPaid(1889, 4.14));
        dividendsPaidPerShare.put(1888, calculateDividendsPaid(1888, 4.47));
        dividendsPaidPerShare.put(1887, calculateDividendsPaid(1887, 4.74));
        dividendsPaidPerShare.put(1886, calculateDividendsPaid(1886, 3.90));
        dividendsPaidPerShare.put(1885, calculateDividendsPaid(1885, 4.62));
        dividendsPaidPerShare.put(1884, calculateDividendsPaid(1884, 7.14));
        dividendsPaidPerShare.put(1883, calculateDividendsPaid(1883, 6.18));
        dividendsPaidPerShare.put(1882, calculateDividendsPaid(1882, 5.48));
        dividendsPaidPerShare.put(1881, calculateDividendsPaid(1881, 5.32));
        dividendsPaidPerShare.put(1880, calculateDividendsPaid(1880, 4.45));
        dividendsPaidPerShare.put(1879, calculateDividendsPaid(1879, 4.07));
        dividendsPaidPerShare.put(1878, calculateDividendsPaid(1878, 5.22));
        dividendsPaidPerShare.put(1877, calculateDividendsPaid(1877, 5.85));
        dividendsPaidPerShare.put(1876, calculateDividendsPaid(1876, 8.38));
        dividendsPaidPerShare.put(1875, calculateDividendsPaid(1875, 6.86));
        dividendsPaidPerShare.put(1874, calculateDividendsPaid(1874, 7.27));
        dividendsPaidPerShare.put(1873, calculateDividendsPaid(1873, 7.47));
        dividendsPaidPerShare.put(1872, calculateDividendsPaid(1872, 5.92));
        dividendsPaidPerShare.put(1871, calculateDividendsPaid(1871, 5.49));
    }

    private static Double calculateDividendsPaid(int year, double dividendYield) {
        Double price = getPriceAt(LocalDate.of(year, 12, 31));
        if (price == null) {
            return null;
        } else {
            return price * (dividendYield / 100.0);
        }
    }

    public static double getGrowth(double yearsAgo) {
        int oldIndex = Helpers.findIndexWithOrBeforeDate(prices, CommonConfig.NOW.minusMonths((long) (yearsAgo * 12.0)));
        if (oldIndex >= prices.size() || oldIndex == -1) {
            oldIndex = prices.size() - 1;
        }

        HistoricalPriceElement latestPrice = prices.get(0);
        HistoricalPriceElement oldPrice = prices.get(oldIndex);

        return GrowthCalculator.calculateGrowth(latestPrice.close, oldPrice.close, yearsAgo);
    }

    public static double getLatestPrice() {
        return prices.get(0).close;
    }

    public static Double getPriceAt(LocalDate date) {
        int oldIndex = Helpers.findIndexWithOrBeforeDate(prices, date);
        if (oldIndex == -1) {
            return null;
        }
        return prices.get(oldIndex).close;
    }

    public static Double getDividendsPaidInYear(int i) {
        return dividendsPaidPerShare.get(i);
    }
}
