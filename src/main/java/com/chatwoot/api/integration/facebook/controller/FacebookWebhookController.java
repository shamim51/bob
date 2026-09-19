package com.chatwoot.api.integration.facebook.controller;

import com.chatwoot.api.integration.facebook.service.FacebookWebhookProcessor;
import com.chatwoot.api.integration.facebook.service.FacebookWebhookVerifier;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
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
        if ("subscribe".equals(mode) && verifier.validVerifyToken(token) && challenge != null) {
            return ResponseEntity.ok(challenge);
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }

    @PostMapping(value = "/bot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> receive(
            @RequestHeader(value = "X-Hub-Signature-256", required = false) String signature,
            @RequestBody byte[] body
    ) {
        if (!verifier.validSignature(body, signature)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        try {
            JsonNode payload = jsonMapper.readTree(body);
            processor.process(payload);
        } catch (JacksonException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return ResponseEntity.ok().build();
    }
}
