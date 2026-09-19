package com.bob.api.integration.facebook.controller;

import com.bob.api.integration.facebook.service.FacebookWebhookProcessor;
import com.bob.api.integration.facebook.service.FacebookWebhookVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class FacebookWebhookController {

    private static final Logger log = LoggerFactory.getLogger(FacebookWebhookController.class);

    private final FacebookWebhookVerifier verifier;
    private final FacebookWebhookProcessor processor;
    private final JsonMapper jsonMapper;

    public FacebookWebhookController(
            FacebookWebhookVerifier verifier,
            FacebookWebhookProcessor processor,
            JsonMapper jsonMapper
    ) {
        this.verifier = verifier;
        this.processor = processor;
        this.jsonMapper = jsonMapper;
    }

    @GetMapping(value = "/bot", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge
    ) {
        log.info("FB_WEBHOOK action=verify mode={} verifyTokenPresent={} challengePresent={}",
                mode, token != null && !token.isBlank(), challenge != null);
        if ("subscribe".equals(mode) && verifier.validVerifyToken(token) && challenge != null) {
            log.info("FB_WEBHOOK action=verify result=OK challengeLength={}", challenge.length());
            return ResponseEntity.ok(challenge);
        }
        String reason = verifyFailureReason(mode, token, challenge);
        log.warn("FB_WEBHOOK action=verify result=FORBIDDEN reason={}", reason);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PostMapping(value = "/bot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] body
    ) {
        int bodyBytes = body == null ? 0 : body.length;
        log.info("FB_WEBHOOK action=receive bodyBytes={} signaturePresent={} signatureFormat={}",
                bodyBytes,
                signature != null && !signature.isBlank(),
                signatureFormat(signature));
        if (!verifier.validSignature(body, signature)) {
            log.warn("FB_WEBHOOK action=receive result=FORBIDDEN reason=invalid_signature");
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        try {
            JsonNode payload = jsonMapper.readTree(body);
            String object = payload.path("object").isMissingNode() || payload.path("object").isNull()
                    ? null
                    : payload.path("object").asText();
            int entryCount = payload.path("entry").isArray() ? payload.path("entry").size() : -1;
            log.info("FB_WEBHOOK action=receive result=ACCEPTED object={} entryCount={}", object, entryCount);
            processor.process(payload);
            log.info("FB_WEBHOOK action=receive result=PROCESSED object={} entryCount={}", object, entryCount);
        } catch (JacksonException ex) {
            log.warn("FB_WEBHOOK action=receive result=BAD_REQUEST reason=invalid_json message={}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok().build();
    }

    private static String verifyFailureReason(String mode, String token, String challenge) {
        if (!"subscribe".equals(mode)) {
            return "invalid_mode";
        }
        if (challenge == null) {
            return "missing_challenge";
        }
        if (token == null || token.isBlank()) {
            return "missing_verify_token";
        }
        return "verify_token_mismatch";
    }

    private static String signatureFormat(String signature) {
        if (signature == null || signature.isBlank()) {
            return "missing";
        }
        if (signature.startsWith("sha256=")) {
            return "sha256";
        }
        return "unexpected";
    }
}
