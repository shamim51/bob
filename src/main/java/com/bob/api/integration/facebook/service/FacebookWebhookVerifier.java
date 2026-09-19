package com.bob.api.integration.facebook.service;

import com.bob.api.integration.facebook.config.FacebookProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class FacebookWebhookVerifier {

    private final FacebookProperties properties;

    public FacebookWebhookVerifier(FacebookProperties properties) {
        this.properties = properties;
    }

    public boolean validVerifyToken(String token) {
        String expected = properties.verifyToken();
        return expected != null && !expected.isBlank() && expected.equals(token);
    }

    public boolean validSignature(byte[] body, String signatureHeader) {
        if (signatureHeader == null || !signatureHeader.startsWith("sha256=")) {
            return false;
        }
        String secret = properties.appSecret();
        if (secret == null || secret.isBlank()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = "sha256=" + HexFormat.of().formatHex(mac.doFinal(body));
            return MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.US_ASCII),
                    signatureHeader.getBytes(StandardCharsets.US_ASCII)
            );
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            return false;
        }
    }
}
