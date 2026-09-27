package ru.nifreebie.infoseclab1.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.nifreebie.infoseclab1.model.User;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public final class JwtService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String ISSUER = "secure-log-api";
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecretKeySpec signingKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET must contain at least 32 bytes");
        }
        if (expirationSeconds < 60 || expirationSeconds > 86_400) {
            throw new IllegalStateException("JWT expiration must be between 60 and 86400 seconds");
        }
        this.signingKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM);
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(User user) {
        long issuedAt = Instant.now().getEpochSecond();
        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", user.getUsername());
        payload.put("uid", user.getId().toString());
        payload.put("iss", ISSUER);
        payload.put("iat", issuedAt);
        payload.put("exp", issuedAt + expirationSeconds);

        try {
            String encodedHeader = encode(objectMapper.writeValueAsBytes(header));
            String encodedPayload = encode(objectMapper.writeValueAsBytes(payload));
            String unsignedToken = encodedHeader + "." + encodedPayload;
            return unsignedToken + "." + encode(sign(unsignedToken));
        } catch (GeneralSecurityException | JacksonException exception) {
            throw new IllegalStateException("Could not create JWT", exception);
        }
    }

    public JwtClaims validateAndRead(String token) {
        try {
            String[] parts = token.split("\\.", -1);
            if (parts.length != 3) {
                throw new JwtAuthenticationException();
            }

            String unsignedToken = parts[0] + "." + parts[1];
            byte[] suppliedSignature = BASE64_URL_DECODER.decode(parts[2]);
            if (!MessageDigest.isEqual(sign(unsignedToken), suppliedSignature)) {
                throw new JwtAuthenticationException();
            }

            JsonNode header = objectMapper.readTree(BASE64_URL_DECODER.decode(parts[0]));
            JsonNode payload = objectMapper.readTree(BASE64_URL_DECODER.decode(parts[1]));
            if (!"HS256".equals(header.path("alg").asText())
                    || !ISSUER.equals(payload.path("iss").asText())
                    || payload.path("exp").asLong(0) <= Instant.now().getEpochSecond()) {
                throw new JwtAuthenticationException();
            }

            String username = payload.path("sub").asText("");
            UUID userId = UUID.fromString(payload.path("uid").asText(""));
            if (username.isBlank()) {
                throw new JwtAuthenticationException();
            }
            return new JwtClaims(userId, username);
        } catch (JwtAuthenticationException exception) {
            throw exception;
        } catch (GeneralSecurityException | JacksonException | IllegalArgumentException exception) {
            throw new JwtAuthenticationException();
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    private byte[] sign(String value) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(ALGORITHM);
        mac.init(signingKey);
        return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
    }

    private String encode(byte[] value) {
        return BASE64_URL_ENCODER.encodeToString(value);
    }
}
