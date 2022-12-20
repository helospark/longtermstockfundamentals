package com.helospark.financialdata.management.config;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.auth0.jwt.algorithms.Algorithm;

@Component
public class JwtService {
    private Algorithm algorithm;

    public JwtService(@Value("classpath:jwt/private_key.key") Resource privateKeyFile, @Value("classpath:jwt/certificate.crt") Resource publicKeyFile) {
        try {
            java.security.Security.addProvider(
                    new org.bouncycastle.jce.provider.BouncyCastleProvider());

            RSAPublicKey publicKey = (RSAPublicKey) PemUtils.readPublicKeyFromFile(publicKeyFile.getInputStream(), "RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) PemUtils.readPrivateKeyFromFile(privateKeyFile.getInputStream(), "RSA");

            algorithm = Algorithm.RSA256(publicKey, privateKey);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] readFile(Resource privateKeyFile) throws IOException {
        return privateKeyFile.getInputStream().readAllBytes();
    }

    public Algorithm getAlgorithm() {
        return algorithm;
    }

}
