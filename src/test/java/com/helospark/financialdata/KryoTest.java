package com.helospark.financialdata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.VersionFieldSerializer;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;
import com.helospark.financialdata.service.SymbolAtGlanceProvider;
import com.helospark.financialdata.util.glance.AtGlanceData;

@TestMethodOrder(OrderAnnotation.class)
@Disabled
public class KryoTest {
    static final int LIMIT = 10;

    @Test
    @Order(0)
    public void testSavePerfKryo() {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();
        LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache = symbolAtGlanceProvider.getSymbolCompanyNameCache();
        Kryo kryo = new Kryo();
        kryo.register(AtGlanceData.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(LocalDate.class);
        kryo.setDefaultSerializer(VersionFieldSerializer.class);

        long start = System.currentTimeMillis();
        for (int i = 0; i < LIMIT; ++i) {
            String filename = "/tmp/atglance_" + (i % 5) + ".bin";
            saveWithKryo(symbolCompanyNameCache, kryo, filename);
        }
        long end = System.currentTimeMillis();

        System.out.println("Save kryo took: " + ((end - start) / 1000.0));
    }

    @Test
    @Order(0)
    public void testSavePerfOm() {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();
        LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache = symbolAtGlanceProvider.getSymbolCompanyNameCache();
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JSR310Module());

        long start = System.currentTimeMillis();
        for (int i = 0; i < LIMIT; ++i) {
            String filename = "/tmp/atglance_" + (i % 5) + ".json";
            saveWithOm(symbolCompanyNameCache, om, filename);
        }
        long end = System.currentTimeMillis();

        System.out.println("Save json took: " + ((end - start) / 1000.0));
    }

    @Test
    @Order(0)
    public void testSavePerfOmGz() {
        SymbolAtGlanceProvider symbolAtGlanceProvider = new SymbolAtGlanceProvider();
        LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache = symbolAtGlanceProvider.getSymbolCompanyNameCache();
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JSR310Module());

        long start = System.currentTimeMillis();
        for (int i = 0; i < LIMIT; ++i) {
            String filename = "/tmp/atglance_" + (i % 5) + ".json.gz";
            saveWithOmGz(symbolCompanyNameCache, om, filename);
        }
        long end = System.currentTimeMillis();

        System.out.println("Save json took: " + ((end - start) / 1000.0));
    }

    @Test
    @Order(10)
    public void testLoadPerfKryo() throws FileNotFoundException {
        Kryo kryo = new Kryo();
        kryo.register(AtGlanceData.class);
        kryo.register(LinkedHashMap.class);
        kryo.register(LocalDate.class);
        kryo.setDefaultSerializer(VersionFieldSerializer.class);

        long start = System.currentTimeMillis();
        for (int i = 0; i < LIMIT; ++i) {
            String filename = "/tmp/atglance_0.bin";
            LinkedHashMap<String, AtGlanceData> result = loadWithKryo(kryo, filename);
            if (result.get("AAPL") == null) {
                System.out.println("Invalid load");
            }
        }
        long end = System.currentTimeMillis();

        System.out.println("Load kryo took: " + ((end - start) / 1000.0));
    }

    @Test
    @Order(10)
    public void testLoadPerfOm() throws StreamReadException, DatabindException, IOException {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JSR310Module());

        long start = System.currentTimeMillis();
        for (int i = 0; i < LIMIT; ++i) {
            String filename = "/tmp/atglance_0.json";
            LinkedHashMap<String, AtGlanceData> result = loadWithOm(om, filename);
            if (result.get("AAPL") == null) {
                System.out.println("Invalid load");
            }
        }
        long end = System.currentTimeMillis();

        System.out.println("Load json took: " + ((end - start) / 1000.0));
    }

    @Test
    @Order(10)
    public void testLoadPerfOmGz() throws StreamReadException, DatabindException, IOException {
        ObjectMapper om = new ObjectMapper();
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.registerModule(new JSR310Module());

        long start = System.currentTimeMillis();
        for (int i = 0; i < LIMIT; ++i) {
            String filename = "/tmp/atglance_0.json.gz";
            LinkedHashMap<String, AtGlanceData> result = loadWithOmGz(om, filename);
            if (result.get("AAPL") == null) {
                System.out.println("Invalid load");
            }
        }
        long end = System.currentTimeMillis();

        System.out.println("Load json took: " + ((end - start) / 1000.0));
    }

    @Test
    public void testKryoCreationCost() {
        long start = System.currentTimeMillis();
        for (int i = 0; i < 1000; ++i) {
            Kryo kryo = new Kryo();
            kryo.register(AtGlanceData.class);
            kryo.register(LinkedHashMap.class);
            kryo.register(LocalDate.class);
            kryo.setDefaultSerializer(VersionFieldSerializer.class);
        }
        long end = System.currentTimeMillis();

        System.out.println("Kryo creation: " + ((end - start) / 1000.0));
    }

    private LinkedHashMap<String, AtGlanceData> loadWithOm(ObjectMapper om, String filename) throws StreamReadException, DatabindException, IOException {
        TypeReference<LinkedHashMap<String, AtGlanceData>> typeRef = new TypeReference<LinkedHashMap<String, AtGlanceData>>() {
        };
        return om.readValue(new File(filename), typeRef);
    }

    private LinkedHashMap<String, AtGlanceData> loadWithOmGz(ObjectMapper om, String filename) throws StreamReadException, DatabindException, IOException {
        TypeReference<LinkedHashMap<String, AtGlanceData>> typeRef = new TypeReference<LinkedHashMap<String, AtGlanceData>>() {
        };

        try (var fis = new GZIPInputStream(new FileInputStream(filename))) {
            return om.readValue(fis, typeRef);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private LinkedHashMap<String, AtGlanceData> loadWithKryo(Kryo kryo, String filename) throws FileNotFoundException {
        Input input = new Input(new FileInputStream(filename));
        LinkedHashMap<String, AtGlanceData> object2 = kryo.readObject(input, LinkedHashMap.class);
        input.close();
        return object2;
    }

    public void saveWithKryo(LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache, Kryo kryo, String filename) {
        Output output;
        try {
            output = new Output(new FileOutputStream(filename));
            kryo.writeObject(output, symbolCompanyNameCache);
            output.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void saveWithOm(LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache, ObjectMapper objectMapper, String filename) {
        File file = new File(filename);
        try {
            objectMapper.writeValue(file, symbolCompanyNameCache);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void saveWithOmGz(LinkedHashMap<String, AtGlanceData> symbolCompanyNameCache, ObjectMapper objectMapper, String filename) {
        File backtestFile = new File(filename);
        try (GZIPOutputStream fos = new GZIPOutputStream(new FileOutputStream(backtestFile))) {
            objectMapper.writeValue(fos, symbolCompanyNameCache);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
