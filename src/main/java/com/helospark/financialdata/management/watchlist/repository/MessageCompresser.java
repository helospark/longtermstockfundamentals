package com.helospark.financialdata.management.watchlist.repository;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class MessageCompresser {
    @Autowired
    private ObjectMapper objectMapper;

    public ByteBuffer createCompressedValue(Object elements) {
        try {
            byte[] bytes = objectMapper.writeValueAsBytes(elements);
            return compressString(bytes);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> List<T> uncompressListOf(ByteBuffer data, Class<T> listType) {
        try {
            String rawString = uncompressString(data);

            JavaType type = objectMapper.getTypeFactory().constructCollectionType(List.class, listType);
            List<T> result = objectMapper.readValue(rawString, type);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ByteBuffer compressString(byte[] uncompressed) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPOutputStream os = new GZIPOutputStream(baos);
        os.write(uncompressed);
        os.close();
        baos.close();
        byte[] compressedBytes = baos.toByteArray();

        ByteBuffer buffer = ByteBuffer.allocate(compressedBytes.length);
        buffer.put(compressedBytes, 0, compressedBytes.length);
        buffer.position(0);
        return buffer;
    }

    public String uncompressString(ByteBuffer input) throws IOException {
        byte[] bytes = input.array();
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        GZIPInputStream is = new GZIPInputStream(bais);

        int chunkSize = 1024;
        byte[] buffer = new byte[chunkSize];
        int length = 0;
        while ((length = is.read(buffer, 0, chunkSize)) != -1) {
            baos.write(buffer, 0, length);
        }

        String result = new String(baos.toByteArray(), "UTF-8");

        is.close();
        baos.close();
        bais.close();

        return result;
    }
}
