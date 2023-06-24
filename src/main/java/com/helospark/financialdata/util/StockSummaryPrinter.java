package com.helospark.financialdata.util;

import java.util.LinkedHashMap;
import java.util.List;

import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

/**
 * Prints more result from the result of ParameterFinderBacktest.
 */
public class StockSummaryPrinter {

    private static final String INPUT = "MANH(20), QLYS(18), VRTX(18), ADBE(18), FIZZ(17), LSCC(16), DECK(16), IDXX(16), ANET(15), CORT(15), CDNS(15), TREX(14), CARG(13), MTD(13), SAFM(13), SBR(13), TER(12), NVR(12), UZA(11), WAT(11), CHE(10), SJT(10), LPX(10), CRVL(10), META(9), CHKP(9), NVDA(8), HBAN(8), LOPE(8), MCFT(8), MEDP(7), REGN(7), BPOPM(7), FC(7), ATKR(7), GBL(7), AAPL(6), EW(6), VEEV(6), AMR(6), EXPD(6), FICO(5), LRCX(5), LSTR(5), HCC(5), SEIC(5), GOOG(5), ULTA(5), CPRX(5), CPRT(5), GOOGL(5), MA(5), ALGN(4), ORLY(4), IDT(4), RHI(4), KFRC(4), SKY(4), ACLS(4), AMAT(4), NBIX(4), NIE(4), GNE(3), ENPH(3), HDSN(3), REX(3), NOW(3), MSFT(3), SHOO(3), CMG(3), AZO(3), SPSC(3), ASML(3), ABMD(3), XPEL(3), DDT(3), BKNG(3), STR(2), VRTV(2), EPAM(2), INTU(2), TXN(2), ZYXI(2), STAA(2), BBAR(2), MLI(2), PAYC(2), BXC(2), WIRE(2), EXEL(2), QIWI(2), MBUU(2), ECOM(2), WSM(2), CLR(2), NRZ(2), POWI(2), FTNT(2), FDS(2), EXLS(2), KLAC(2), WWE(2), ETSY(2), HCKT(2), LOGI(2), SIGA(2), XLNX(2), PCTY(2), MED(2), EVR(2), MNST(2), CVCO(1), ATEN(1), MIME(1), TSLA(1), BCC(1), PRDO(1), WINA(1), AEHR(1), POOL(1), VRSN(1), ARCH(1), HPQ(1), VLO(1), HQH(1), HQI(1), CF(1), ATHM(1), ACN(1), CALX(1), INCY(1), GPC(1), PRG(1), TNET(1), HCA(1), UMC(1), INVA(1), BPOPN(1), RGR(1), SYNA(1), ONTO(1), OAS(1), WLKP(1), LLY(1), HMHC(1), FORR(1), LULU(1), NSSC(1), HOLX(1), RACE(1), UMBF(1), HD(1), NRP(1), CEIX(1), MPX(1), IT(1), BSIG(1), MELI(1), ERF(1), DFIN(1), DIOD(1), HSII(1), OVV(1), TPL(1), CRUS(1), ARLP(1), PDCE(1), RMR(1), ZBRA(1), OFLX(1), DOOO(1), AMD(1), MPWR(1), HLNE(1), PJT(1), GIC(1), GMAB(1), ATRS(1), CAH(1), ROK(1), HEI-A(1), YELP(1), SAM(1), STLD(1), BIIB(1), TTD(1), NX(1), PRTA(1), XNCR(1), FFIV(1), UTHR(1), TDC(1), DDS(1), NVAX(1), SGEN(1), AGYS(1), DSGX(1), APA(1), BCOR(1)";
    private static final List<String> COLUMNS_TO_PRINT = List.of("pe", "trailingPeg:tpeg", "altman", "roic", "roe", "dtoe", "grMargin:grm", "opCMargin:opm", "shareCountGrowth:shareG",
            "price10Gr:grow", "fvCalculatorMoS:mos");

    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        String[] stocks = INPUT.split(", ");

        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();
        LinkedHashMap<String, AtGlanceData> data = symbolAtGlanceProvider.getSymbolCompanyNameCache();

        System.out.printf("%-15s\t", "stock");
        for (var column : COLUMNS_TO_PRINT) {
            String dataToPrint = column;
            if (column.contains(":")) {
                dataToPrint = column.split(":")[1];
            }
            System.out.print(dataToPrint + "\t");
        }
        System.out.println();

        for (var stock : stocks) {
            String ticker = stock.substring(0, stock.indexOf("("));
            AtGlanceData atGlance = data.get(ticker);
            if (atGlance != null) {
                System.out.printf("%-15s\t", stock);

                for (var column : COLUMNS_TO_PRINT) {
                    System.out.printf("%.2f\t", getFieldAsDouble(atGlance, column));
                }
                System.out.printf("%.1f\t", atGlance.marketCapUsd / 1000.0);
                System.out.printf("%s", atGlance.companyName);
                System.out.println();
            }
        }
    }

    public static Double getFieldAsDouble(AtGlanceData atGlance, String column) throws IllegalAccessException, NoSuchFieldException {
        if (column.contains(":")) {
            column = column.split(":")[0];
        }
        Object value = AtGlanceData.class.getField(column).get(atGlance);

        if (value.getClass().equals(Double.class)) {
            return (Double) value;
        } else if (value.getClass().equals(Float.class)) {
            return ((Float) value).doubleValue();
        } else if (value.getClass().equals(Byte.class)) {
            return ((Byte) value).doubleValue();
        } else if (value.getClass().equals(Integer.class)) {
            return ((Integer) value).doubleValue();
        } else if (value.getClass().equals(Short.class)) {
            return ((Short) value).doubleValue();
        } else {
            throw new RuntimeException("Unknown type");
        }
    }

}
