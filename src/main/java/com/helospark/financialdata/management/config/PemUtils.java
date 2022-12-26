package com.helospark.financialdata.management.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PemUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(PemUtils.class);

    private static byte[] parsePEMFile(InputStream stream) throws IOException {
        PemReader reader = new PemReader(new InputStreamReader(stream));
        PemObject pemObject = reader.readPemObject();
        byte[] content = pemObject.getContent();
        reader.close();
        return content;
    }

    private static PublicKey getPublicKey(byte[] keyBytes, String algorithm) {
        PublicKey publicKey = null;
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            publicKey = kf.generatePublic(keySpec);
        } catch (NoSuchAlgorithmException e) {
            LOGGER.error("Could not reconstruct the public key, the given algorithm could not be found.", e);
            throw new RuntimeException(e);
        } catch (InvalidKeySpecException e) {
            LOGGER.error("Could not reconstruct the public key", e);
            throw new RuntimeException(e);
        }

        return publicKey;
    }

    private static PrivateKey getPrivateKey(byte[] keyBytes, String algorithm) {
        PrivateKey privateKey = null;
        try {
            KeyFactory kf = KeyFactory.getInstance(algorithm);
            EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            privateKey = kf.generatePrivate(keySpec);
        } catch (NoSuchAlgorithmException e) {
            System.out.println("Could not reconstruct the private key, the given algorithm could not be found.");
        } catch (InvalidKeySpecException e) {
            System.out.println("Could not reconstruct the private key");
        }

        return privateKey;
    }

    public static PublicKey readPublicKeyFromFile(InputStream stream, String algorithm) throws IOException {
        byte[] bytes = PemUtils.parsePEMFile(stream);
        return PemUtils.getPublicKey(bytes, algorithm);
    }

    public static PrivateKey readPrivateKeyFromFile(InputStream inputStream, String algorithm) throws IOException {
        byte[] bytes = PemUtils.parsePEMFile(inputStream);
        return PemUtils.getPrivateKey(bytes, algorithm);
    }

}
