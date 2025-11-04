package io.terrakube.client.dex;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DexCredentialAuthentication implements RequestInterceptor {

    private static final String ISSUER = "TerrakubeInternal";
    private static final String SUBJECT = "TerrakubeInternal (TOKEN)";
    private static final String EMAIL = "no-reply@terrakube.io";
    private static final String NAME = "TerrakubeInternal Client";

    private final String secretKey;
    private final DexCredentialType dexCredentialType;

    public DexCredentialAuthentication(String secretKey, DexCredentialType dexCredentialType) {
        this.secretKey = secretKey;
        this.dexCredentialType = dexCredentialType;
    }

    private String generateAccessToken() {
        log.debug("Generate Dex Authentication Private Token");
        String newToken = "";

        if (this.dexCredentialType.equals(DexCredentialType.INTERNAL)) {
            SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64URL.decode(this.secretKey));
            log.debug("Using secret key:");
            newToken = Jwts.builder()
                    .header().add("typ", "JWT").and()
                    .issuer(DexCredentialAuthentication.ISSUER)
                    .subject(DexCredentialAuthentication.SUBJECT)
                    .audience().add(DexCredentialAuthentication.ISSUER)
                    .and()
                    .claim("email", DexCredentialAuthentication.EMAIL)
                    .claim("email_verified", true)
                    .claim("name", DexCredentialAuthentication.NAME)
                    .issuedAt(Date.from(Instant.now()))
                    .expiration(Date.from(Instant.now().plus(30, ChronoUnit.DAYS)))
                    .signWith(key, SignatureAlgorithm.HS256)
                    .compact();

        } else {
            log.debug("Using pat:");
            newToken = this.secretKey;
        }

        return newToken;
    }

    @Override
    public void apply(RequestTemplate requestTemplate) {
        requestTemplate.header("Authorization", "Bearer " + generateAccessToken());
    }
}
