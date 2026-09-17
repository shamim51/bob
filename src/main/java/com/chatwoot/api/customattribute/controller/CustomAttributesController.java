package com.chatwoot.api.customattribute.controller;

import com.chatwoot.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class CustomAttributesController {

    private final CurrentUserService currentUserService;

    public CustomAttributesController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/custom_attribute_definitions")
    public List<Object> customAttributes(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return List.of();
    }
}
