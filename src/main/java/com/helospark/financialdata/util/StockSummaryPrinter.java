package com.helospark.financialdata.util;

import java.util.LinkedHashMap;
import java.util.List;

import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

/**
 * Prints more result from the result of ParameterFinderBacktest.
 */
public class StockSummaryPrinter {

    private static final String INPUT = "IDXX(39), MANH(36), QLYS(33), CRVL(30), ADBE(30), NVDA(27), TPL(27), MEDP(26), LSCC(24), FICO(24), CDNS(24), MTD(24), ANET(23), VRTX(23), DECK(22), SNPS(22), PBT(21), ASML(21), OFLX(21), MA(21), XPEL(20), FTNT(19), NVO(19), XLNX(19), MKTX(17), LULU(17), MSFT(16), SJT(16), VEEV(16), CORT(16), MNST(16), IT(15), EXPO(15), AAPL(15), GMAB(15), BKNG(15), SPSC(14), GWW(14), ROL(14), WINA(13), ADP(13), MSCI(13), EW(13), GOOG(13), PAYX(13), ODFL(13), INTU(13), NOW(13), FDS(13), GOOGL(13), MPWR(13), SAFM(13), ACLS(12), AMAT(12), ABMD(12), DSGX(12), NRC(12), ULTA(12), CPRT(12), TER(11), CTAS(11), FAST(11), CHE(11), EPAM(11), CPRX(11), PCTY(11), ENPH(10), V(10), LRCX(10), ELF(10), CGNX(10), MELI(10), BMI(10), TTD(10), KEYS(10), TXN(10), LOPE(10), ACN(9), PAYC(9), EXEL(9), CMG(9), POWI(9), CNS(9), EXPD(9), ALGN(9), LII(9), TDC(9), RMBS(9), WING(8), CHKP(8), ISRG(8), NBIX(8), META(8), VRSK(8), VRSN(8), NSSC(8), NVR(8), TREX(8), SBR(8), TSLA(7), WMS(7), HDSN(7), ITW(7), FC(7), MCFT(7), WST(7), KLAC(7), NKE(7), UZA(7), REGN(7), WAT(7), GBL(7), LLY(7), ATEN(6), POOL(6), GNE(6), SMCI(6), INFY(6), EA(6), USLM(6), ATKR(6), KFRC(6), HD(6), SKY(6), SLP(6), ROK(6), CWST(6), MGY(6), VRTV(6), PCOM(6), ANSS(6), FIZZ(6), ETSY(6), SAIA(6), AVGO(6), FRPH(6), MIME(5), NVEC(5), INCY(5), RES(5), HSY(5), HTA(5), NVMI(5), DPZ(5), NFLX(5), MCK(5), MCO(5), TRI(5), PJT(5), MED(5), FFIV(5), LSTR(5), MLI(5), UI(5), SEIC(5), KNSL(5), BIDU(5), LECO(5), WHD(5), DDS(5), AZPN(5), WDAY(5), FIX(5), AVID(4), NXGN(4), SLAB(4), ABC(4), ALXN(4), TNET(4), NTCT(4), WIRE(4), ONTO(4), TROW(4), FORR(4), WSM(4), WSO(4), HOLX(4), WSO-B(4), MCHP(4), CSGP(4), VALU(4), NEX(4), ADSK(4), YELP(4), CRM(4), GDDY(4), JKHY(4), BLDR(4), STAA(4), TEAM(4), PRG(4), ECOM(4), MOH(4), COLL(4), RACE(4), PVG(4), CARG(4), MPX(4), OLED(4), QCOM(4), PANW(4), MSI(4), GGG(4), HCKT(4), HLI(4), COST(4), TEL(4), CVCO(3), NOVT(3), TTMI(3), VCEL(3), A(3), FLT(3), WNS(3), WDFC(3), PAC(3), ITRN(3), FNV(3), FN(3), TLK(3), LQDT(3), HLIT(3), LMAT(3), WWE(3), RMD(3), BMY(3), AMBA(3), LOGI(3), SIGA(3), AMR(3), VRNT(3), SQM(3), TSM(3), ANF(3), EXPI(3), PYPL(3), SSD(3), AOS(3), BCPC(3), CERN(3), VRTS(3), STZ(3), ATVI(3), ESGR(3), TYL(3), HZNP(3), CROX(3), CAMT(3), ERII(3), EVTC(3), PERI(3), SHOO(3), BATRA(3), HGH(3), NSP(3), EAF(3), BATRK(3), AZO(3), SITE(3), AVCT(3), ZBRA(3), OSUR(3), BIIB(3), DDT(3), SPGI(3), BBY(2), BCC(2), IQV(2), EQC-PD(2), H(2), DHR(2), ZTS(2), MTCH(2), HRB(2), ADI(2), ITT(2), WPM(2), ACGL(2), CIG(2), IRMD(2), MVIS(2), BPOPM(2), NTES(2), RHI(2), SJM(2), HSTM(2), FORM(2), VEON(2), PEN(2), CLR(2), WTI(2), NTGR(2), ODP(2), SPOT(2), CVLT(2), MSGN(2), ERF(2), MAR(2), CNX(2), BMA(2), GLOB(2), CPA(2), CHRW(2), NEM(2), AMD(2), AMN(2), BOX(2), HEI-A(2), WRLD(2), TTC(2), CIG-C(2), AGYS(2), ON(2), AAON(2), APH(2), EXP(2), CTS(2), AMGN(2), STM(2), KLIC(2), PRDO(2), IAC(2), KEX(2), VICR(2), FELE(2), COHU(2), HTBK(2), TK(2), IVR-PC(2), IVR-PB(2), PZZA(2), SCCO(2), PSB(2), MMC(2), ERIE(2), TECH(2), PTC(2), NTUS(2), QIWI(2), JHX(2), HEI(2), LMT(2), FORTY(2), CYBE(2), AVAV(2), PHYS(2), TGNA(2), LPX(2), LNTH(2), CSWI(2)";
    private static final List<String> COLUMNS_TO_PRINT = List.of("pe", "trailingPeg:tpeg", "altman", "roic", "roe", "dtoe", "grMargin:grm", "opCMargin:opm", "shareCountGrowth:shareG",
            "price10Gr:grow", "fvCalculatorMoS:mos");
    private static final Double FAIR_VALUE_FILTER_THRESHOLD = null;

    public static void main(String[] args) throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        printStockSummary(INPUT);
    }

    public static void printStockSummary(String input) throws IllegalAccessException, NoSuchFieldException {
        String[] stocks = input.split(", ");

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
            String ticker;
            if (stock.indexOf("(") != -1) {
                ticker = stock.substring(0, stock.indexOf("("));
            } else {
                ticker = stock;
            }
            AtGlanceData atGlance = data.get(ticker);
            if (atGlance != null && applyFilter(atGlance)) {
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

    private static boolean applyFilter(AtGlanceData atGlance) {
        return FAIR_VALUE_FILTER_THRESHOLD == null || atGlance.fvCalculatorMoS > FAIR_VALUE_FILTER_THRESHOLD;
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
