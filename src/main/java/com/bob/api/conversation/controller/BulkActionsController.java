package com.bob.api.conversation.controller;

import com.bob.api.account.model.AccountUser;
import com.bob.api.conversation.dto.BulkActionRejectedResponse;
import com.bob.api.conversation.dto.BulkActionRequest;
import com.bob.api.conversation.service.BulkActionsJob;
import com.bob.api.security.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}/bulk_actions")
public class BulkActionsController {

    private final CurrentUserService currentUserService;
    private final BulkActionsJob bulkActionsJob;

    public BulkActionsController(CurrentUserService currentUserService, BulkActionsJob bulkActionsJob) {
        this.currentUserService = currentUserService;
        this.bulkActionsJob = bulkActionsJob;
    }

    @PostMapping
    public ResponseEntity<?> create(
            @PathVariable Integer accountId,
            @RequestBody BulkActionRequest body
    ) {
        AccountUser membership = currentUserService.requireMembership(accountId);
        if (!"Conversation".equals(BulkActionsJob.camelize(body == null ? null : body.type()))) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(new BulkActionRejectedResponse(false));
        }
        bulkActionsJob.perform(accountId, membership.getUser(), membership, body);
        return ResponseEntity.ok().build();
    }
}
