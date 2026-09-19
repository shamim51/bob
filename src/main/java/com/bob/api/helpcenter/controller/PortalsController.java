package com.bob.api.helpcenter.controller;

import com.bob.api.helpcenter.dto.PortalsIndexResponse;
import com.bob.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class PortalsController {

    private final CurrentUserService currentUserService;

    public PortalsController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/portals")
    public PortalsIndexResponse index(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return new PortalsIndexResponse(
                List.of(),
                new PortalsIndexResponse.PortalsMeta(1, 0)
        );
    }
}
