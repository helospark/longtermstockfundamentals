package com.helospark.financialdata;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.helospark.financialdata.management.watchlist.repository.MessageCompresser;
import com.helospark.financialdata.management.watchlist.repository.WatchlistElement;

public class MessageConverterCompressionRatioTest {

    @Test
    public void testSmallList() throws IOException {
        MessageCompresser messageCompresser = new MessageCompresser();
        List<WatchlistElement> watchlistElements = new ArrayList<>();

        var amazon = new WatchlistElement();
        amazon.symbol = "AMZN";
        amazon.targetPrice = 100.0;
        amazon.notes = "Great company, even this application is running there. Risk is high PE.";
        amazon.tags = List.of("BIG_TECH");

        var mbuu = new WatchlistElement();
        mbuu.symbol = "MBUU";
        mbuu.targetPrice = 25.0;
        mbuu.notes = "Cheap company building boats. Consistent high growth with low PE. Risk is small marketcap, recession slowdown in high-price recreational vehicles.";
        mbuu.tags = List.of("GROWTH", "CHEAP");

        var ibp = new WatchlistElement();
        ibp.symbol = "IBP";
        ibp.targetPrice = 100.0;
        ibp.notes = "Cheap company in the building space. Risk is a possible housing slowdown caused by increasing mortgage rates. Low PE and high growth.";
        ibp.tags = List.of("GROWTH", "CHEAP", "HOUSING");

        watchlistElements.add(amazon);
        watchlistElements.add(mbuu);
        watchlistElements.add(ibp);

        ObjectMapper om = new ObjectMapper();
        byte[] uncompressed = om.writeValueAsBytes(watchlistElements);

        System.out.println("Uncompressed=" + uncompressed.length);

        byte[] compressed = messageCompresser.compressString(uncompressed).array();

        System.out.println("Compressed=" + compressed.length);
    }

    @Test
    public void testEmptyNotes() throws IOException {
        MessageCompresser messageCompresser = new MessageCompresser();
        List<WatchlistElement> watchlistElements = new ArrayList<>();

        var amazon = new WatchlistElement();
        amazon.symbol = "AMZN";

        var mbuu = new WatchlistElement();
        mbuu.symbol = "MBUU";

        var ibp = new WatchlistElement();
        ibp.symbol = "IBP";

        watchlistElements.add(amazon);
        watchlistElements.add(mbuu);
        watchlistElements.add(ibp);

        ObjectMapper om = new ObjectMapper();
        byte[] uncompressed = om.writeValueAsBytes(watchlistElements);

        System.out.println("Uncompressed=" + uncompressed.length);

        byte[] compressed = messageCompresser.compressString(uncompressed).array();

        System.out.println("Compressed=" + compressed.length);
    }

}
