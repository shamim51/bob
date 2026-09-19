package com.bob.api.team.controller;

import com.bob.api.security.CurrentUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class TeamsController {

    private final CurrentUserService currentUserService;

    public TeamsController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/teams")
    public List<Object> teams(@PathVariable Integer accountId) {
        currentUserService.requireMembership(accountId);
        return List.of();
    }
}
