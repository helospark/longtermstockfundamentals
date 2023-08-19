package com.helospark.financialdata.util;

import java.util.LinkedHashMap;
import java.util.List;

import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

/**
 * Prints more result from the result of ParameterFinderBacktest.
 */
public class StockSummaryPrinter {

    private static final String INPUT = "QLYS(36), IDXX(34), MANH(30), TPL(30), MA(30), ASML(27), NVO(27), ADBE(27), MEDP(26), LSCC(26), CDNS(26), CRVL(24), FTNT(21), AAPL(20), MSFT(18), LULU(18), MELI(18), ODFL(17), VEEV(16), SNPS(16), VRTX(15), XPEL(15), FICO(13), ADP(13), PBT(13), PAYX(13), MPWR(13), MTD(12), ANET(12), ADSK(12), CTAS(11), DECK(11), IT(11), OFLX(11), BKNG(11), VRSK(10), ENPH(10), INTU(10), NOW(10), FC(10), AMAT(10), XLNX(10), MGY(10), V(9), CPRX(9), ROL(9), CORT(9), EXPO(9), ABC(8), ACN(8), CHE(8), MSCI(8), FDS(8), BMI(8), AVGO(8), TER(7), LRCX(7), LII(7), RACE(7), NRC(7), CMG(7), ABMD(7), GMAB(7), DSGX(7), MNST(7), RMBS(7), WINA(6), NVDA(6), CHKP(6), ORLY(6), EW(6), SJT(6), KNSL(6), ACLS(6), SPSC(6), MSI(6), FIZZ(6), SAFM(6), CWST(6), NKE(5), VRSN(5), ZTS(5), FAST(5), MKTX(5), WAT(5), GOOG(5), ISRG(5), ULTA(5), NEX(5), LOGI(5), PCTY(5), RGEN(5), EXPD(5), SBR(5), POOL(4), WMS(4), GNE(4), LSTR(4), TEAM(4), LLY(4), KFRC(4), MPX(4), PCOM(4), MCHP(4), GWW(4), BMY(4), CPRT(4), MCO(4), GOOGL(4), LECO(4), TDC(4), SPGI(4), FIX(4), STZ(3), A(3), BTU(3), TXN(3), ALGN(3), WDFC(3), SMCI(3), RES(3), CROX(3), IVR-PC(3), FNV(3), NNN(3), PAYC(3), SHW(3), ERIE(3), HSY(3), YUM(3), HEI(3), WST(3), HD(3), TSCO(3), SBUX(3), NSP(3), AAM-PB(3), MAR(3), CNS(3), DPZ(3), MSA(3), NFLX(3), KLAC(3), SIGA(3), TREX(3), HLNE(3), TXRH(3), ROK(3), NBIX(3), CVCO(2), AVID(2), QD(2), TSLA(2), VRTS(2), CDW(2), BBY(2), HALO(2), META(2), ABBV(2), CVE(2), EPAM(2), VCEL(2), PRGS(2), CF(2), GOL(2), HAL(2), REGN(2), STRL(2), CHH(2), UI(2), FFNW(2), VG(2), HTA(2), BPOPM(2), IIPR-PA(2), ORC(2), SEIC(2), GBL(2), RHI(2), MOD(2), VEON(2), MOH(2), WSO(2), TTEK(2), SLP(2), HGH(2), POWI(2), BLKB(2), CSGP(2), BMA(2), PANW(2), GGG(2), MCK(2), ETSY(2), OXY(2), WHD(2), PJT(2), AMR(2), YELP(2), EVR(2), EXPE(2), PARR(2), ON(2), AON(2), FRPH(2), AZPN(2), EXP(2), CERN(1), PR(1), BDSI(1), SESN(1), DFS(1), MIME(1), GDDY(1), NXGN(1), BSM(1), SEAS(1), PRDO(1), HURN(1), HUBB(1), JKHY(1), HESM(1), EQC-PD(1), UZA(1), BLDR(1), ARCH(1), VICR(1), BR(1), NTAP(1), IBP(1), OMAB(1), SP(1), RDN(1), PFPT(1), IBKR(1), TYG(1), TYL(1), CL(1), ACM(1), BBAR(1), HDSN(1), INCY(1), GPC(1), PAM(1), ELF(1), ARES(1), IVR-PB(1), MLI(1), DQ(1), TNET(1), HBI(1), DX(1), WPM(1), CAMT(1), LOPE(1), EME(1), CIG(1), MMC(1), IRMD(1), TJX(1), SLCT(1), WIRE(1), ERII(1), BPOP(1), LXP-PC(1), INVA(1), DLB(1), PSB-PX(1), PSB-PY(1), CSCO(1), SPNS(1), CRAI(1), BYD(1), WLKP(1), TLK(1), DGICB(1), JHX(1), ECOM(1), WT(1), FORM(1), PTNR(1), LYTS(1), NSSC(1), LMT(1), COLL(1), LQDT(1), SKY(1), WTI(1), CLX(1), HLIT(1), WSO-B(1), QNST(1), CNBKA(1), TIPT(1), HWM(1), CVLT(1), BKR(1), TOL(1), LOW(1), VST(1), ERF(1), PGR(1), NTB(1), NVMI(1), MRK(1), MRO(1), CRUS(1), LPX(1), GLOB(1), VALU(1), PCRX(1), HMLP(1), KOS(1), CPA(1), RMR(1), AVCT(1), EDD(1), HCKT(1), TRI(1), NVR(1), SWBI(1), GLOP-PB(1), AMG(1), OSUR(1), AMP(1), ORCL(1), ATRS(1), CAR(1), HEI-A(1), IPAR(1), CHTR(1), CRM(1), ESTE(1), STLD(1), TTC(1), HLI(1), CIG-C(1), TTD(1), ALTA(1), GILT(1), DDS(1), COST(1), ALSN(1), AAON(1), PDFS(1), BAH(1), LEA(1), ACRX(1), PG(1), APH(1)";
    private static final List<String> COLUMNS_TO_PRINT = List.of("pe", "trailingPeg:tpeg", "altman", "roic", "roe", "dtoe", "grMargin:grm", "opCMargin:opm", "shareCountGrowth:shareG",
            "price10Gr:grow", "fvCalculatorMoS:mos");

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
