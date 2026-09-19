package com.chatwoot.api.integration.facebook.controller;

import com.chatwoot.api.inbox.dto.InboxResponse;
import com.chatwoot.api.inbox.mapper.InboxMapper;
import com.chatwoot.api.inbox.model.Inbox;
import com.chatwoot.api.inbox.repository.InboxRepository;
import com.chatwoot.api.integration.facebook.dto.FacebookInboxDataResponse;
import com.chatwoot.api.integration.facebook.dto.FacebookPageDetailResponse;
import com.chatwoot.api.integration.facebook.dto.FacebookPagesRequest;
import com.chatwoot.api.integration.facebook.dto.FacebookPagesResponse;
import com.chatwoot.api.integration.facebook.dto.FacebookRegisterInboxResponse;
import com.chatwoot.api.integration.facebook.dto.ReauthorizePageRequest;
import com.chatwoot.api.integration.facebook.dto.RegisterFacebookPageRequest;
import com.chatwoot.api.integration.facebook.model.FacebookPage;
import com.chatwoot.api.integration.facebook.repository.FacebookPageRepository;
import com.chatwoot.api.integration.facebook.service.FacebookGraphClient;
import com.chatwoot.api.security.CurrentUserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/accounts/{accountId}")
public class CallbacksController {

    private static final Logger log = LoggerFactory.getLogger(CallbacksController.class);

    private final CurrentUserService currentUserService;
    private final FacebookGraphClient graph;
    private final FacebookPageRepository facebookPages;
    private final InboxRepository inboxes;
    private final InboxMapper inboxMapper;

    public CallbacksController(
            CurrentUserService currentUserService,
            FacebookGraphClient graph,
            FacebookPageRepository facebookPages,
            InboxRepository inboxes,
            InboxMapper inboxMapper
    ) {
        this.currentUserService = currentUserService;
        this.graph = graph;
        this.facebookPages = facebookPages;
        this.inboxes = inboxes;
        this.inboxMapper = inboxMapper;
    }

    @PostMapping({"/callbacks/facebook_pages", "/callbacks/facebook_pages.json"})
    public FacebookPagesResponse facebookPages(
            @PathVariable Integer accountId,
            @RequestBody FacebookPagesRequest request
    ) {
        currentUserService.requireMembership(accountId);
        if (request == null || request.omniauthToken() == null || request.omniauthToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "omniauth_token is required");
        }
        String userAccessToken = longLivedToken(request.omniauthToken());
        List<FacebookGraphClient.FacebookAccountPage> pages = graph.listPages(userAccessToken);
        Set<String> existing = new HashSet<>(facebookPages.findPageIdsByAccountId(accountId));
        List<FacebookPageDetailResponse> details = pages.stream()
                .filter(page -> page.id() != null)
                .map(page -> new FacebookPageDetailResponse(
                        page.id(),
                        page.name(),
                        page.accessToken(),
                        existing.contains(page.id())
                ))
                .toList();
        return new FacebookPagesResponse(new FacebookPagesResponse.Data(details, userAccessToken));
    }

    private String longLivedToken(String omniauthToken) {
        try {
            String exchanged = graph.exchangeLongLivedToken(omniauthToken);
            if (exchanged != null && !exchanged.isBlank()) {
                return exchanged;
            }
        } catch (RuntimeException ex) {
            log.error("Error in long_lived_token: {}", ex.getMessage());
        }
        return omniauthToken;
    }

    @PostMapping("/callbacks/register_facebook_page")
    @Transactional
    public FacebookRegisterInboxResponse registerFacebookPage(
            @PathVariable Integer accountId,
            @RequestBody RegisterFacebookPageRequest request
    ) {
        currentUserService.requireMembership(accountId);
        if (request == null
                || blank(request.pageId())
                || blank(request.pageAccessToken())
                || blank(request.userAccessToken())
                || blank(request.inboxName())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "page_id, tokens, and inbox_name are required");
        }
        if (facebookPages.existsByAccountIdAndPageId(accountId, request.pageId())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Facebook page is already connected");
        }
        FacebookPage page = new FacebookPage();
        page.setAccountId(accountId);
        page.setPageId(request.pageId());
        page.setUserAccessToken(request.userAccessToken());
        page.setPageAccessToken(request.pageAccessToken());
        page = facebookPages.save(page);

        syncPageDetails(page);
        subscribe(page);

        Inbox inbox = new Inbox();
        inbox.setAccountId(accountId);
        inbox.setName(request.inboxName());
        inbox.setChannelType(Inbox.CHANNEL_FACEBOOK);
        inbox.setChannelId(page.getId());
        inbox = inboxes.save(inbox);

        return new FacebookRegisterInboxResponse(
                inbox.getId(),
                inbox.getChannelId(),
                inbox.getName(),
                inbox.getChannelType(),
                "",
                page.getPageId(),
                inbox.getEnableAutoAssignment()
        );
    }

    @PostMapping("/callbacks/reauthorize_page")
    @Transactional
    public FacebookInboxDataResponse reauthorizePage(
            @PathVariable Integer accountId,
            @RequestBody ReauthorizePageRequest request
    ) {
        currentUserService.requireMembership(accountId);
        if (request == null || request.inboxId() == null || blank(request.omniauthToken())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        Inbox inbox = inboxes.findByIdAndAccountId(request.inboxId(), accountId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY));
        if (!inbox.facebookChannel()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        FacebookPage page = facebookPages.findById(inbox.getChannelId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY));
        try {
            String userAccessToken = graph.exchangeLongLivedToken(request.omniauthToken());
            List<FacebookGraphClient.FacebookAccountPage> pages = graph.listPages(userAccessToken);
            FacebookGraphClient.FacebookAccountPage match = pages.stream()
                    .filter(candidate -> page.getPageId().equals(candidate.id()))
                    .findFirst()
                    .orElse(null);
            if (match == null || blank(match.accessToken())) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
            }
            page.setUserAccessToken(userAccessToken);
            page.setPageAccessToken(match.accessToken());
            page.setReauthorizationRequired(false);
            facebookPages.save(page);
            syncPageDetails(page);
            subscribe(page);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.error("Error in reauthorize_page: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY);
        }
        InboxResponse mapped = inboxMapper.inbox(inbox);
        return new FacebookInboxDataResponse(mapped);
    }

    private void syncPageDetails(FacebookPage page) {
        try {
            FacebookGraphClient.FacebookPageDetails details = graph.fetchPageDetails(page.getPageAccessToken());
            if (details.providerName() != null) {
                page.setProviderName(details.providerName());
            }
            if (details.instagramId() != null) {
                page.setInstagramId(details.instagramId());
            }
            facebookPages.save(page);
        } catch (RuntimeException ex) {
            log.error("Error in sync_page_details: {}", ex.getMessage());
        }
    }

    private void subscribe(FacebookPage page) {
        try {
            graph.subscribePage(page.getPageId(), page.getPageAccessToken());
        } catch (RuntimeException ex) {
            log.debug("Rescued Facebook subscribe: {}", ex.getMessage());
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
