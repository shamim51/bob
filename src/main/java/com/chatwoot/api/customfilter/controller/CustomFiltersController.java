package com.chatwoot.api.customfilter.controller;

import com.chatwoot.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class CustomFiltersController {

    private final CurrentUserService currentUserService;

    public CustomFiltersController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/custom_filters")
    public List<Object> customFilters(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return List.of();
    }
}
