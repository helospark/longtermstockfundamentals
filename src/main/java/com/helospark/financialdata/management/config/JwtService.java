package com.helospark.financialdata.management.config;

import java.io.IOException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.helospark.financialdata.management.user.repository.AccountType;

@Component
public class JwtService {
    public static final String ACCOUNT_TYPE_CLAIM = "type";
    private static final Logger LOGGER = LoggerFactory.getLogger(JwtService.class);
    public static final String ISSUER = "longTermStockFundamentals";
    private Algorithm algorithm;
    private JWTVerifier verifier;

    public JwtService(@Value("classpath:jwt/jwt2.pem") Resource privateKeyFile, @Value("classpath:jwt/jwt2.pub") Resource publicKeyFile) {
        try {
            java.security.Security.addProvider(
                    new org.bouncycastle.jce.provider.BouncyCastleProvider());

            RSAPublicKey publicKey = (RSAPublicKey) PemUtils.readPublicKeyFromFile(publicKeyFile.getInputStream(), "RSA");
            RSAPrivateKey privateKey = (RSAPrivateKey) PemUtils.readPrivateKeyFromFile(privateKeyFile.getInputStream(), "RSA");

            algorithm = Algorithm.RSA256(publicKey, privateKey);

            verifier = JWT.require(algorithm)
                    .withIssuer(ISSUER)
                    .build();
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

    public boolean validateJwt(String token) {
        return getDecodedJwt(token).isPresent();
    }

    public Optional<DecodedJWT> getDecodedJwt(String token) {
        try {
            DecodedJWT decodedJWT = verifier.verify(token);
            LocalDateTime expiry = convertToLocalDateTimeViaInstant(decodedJWT.getExpiresAt());
            LocalDateTime now = LocalDateTime.now();
            if (now.isAfter(expiry)) {
                LOGGER.error("Token expired for '{}', now={}, expiry={}", decodedJWT.getSubject(), now, expiry);
                return Optional.empty();
            }
            String accountTypeClaim = decodedJWT.getClaim(ACCOUNT_TYPE_CLAIM).asString();
            if (!AccountType.fromStringOptional(accountTypeClaim).isPresent()) {
                LOGGER.error("Unknown account type '{}'", accountTypeClaim);
                return Optional.empty();
            }
            return Optional.of(decodedJWT);
        } catch (Exception exception) {
            LOGGER.error("Cannot validate token '{}'", token, exception);
            return Optional.empty();
        }
    }

    public LocalDateTime convertToLocalDateTimeViaInstant(Date dateToConvert) {
        return dateToConvert.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

}
