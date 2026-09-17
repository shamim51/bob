package com.chatwoot.api.label.controller;

import com.chatwoot.api.security.CurrentUserService;
import com.chatwoot.api.shared.dto.PayloadResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class LabelsController {

    private final CurrentUserService currentUserService;

    public LabelsController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/labels")
    public PayloadResponse<List<Object>> labels(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return new PayloadResponse<>(List.of());
    }
}
