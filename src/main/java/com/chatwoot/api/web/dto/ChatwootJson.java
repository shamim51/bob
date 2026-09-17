package com.chatwoot.api.web.dto;

import com.chatwoot.api.domain.Account;
import com.chatwoot.api.domain.AccountUser;
import com.chatwoot.api.domain.Contact;
import com.chatwoot.api.domain.Conversation;
import com.chatwoot.api.domain.Inbox;
import com.chatwoot.api.domain.Message;
import com.chatwoot.api.domain.User;
import com.chatwoot.api.repo.MessageRepository;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChatwootJson {

    private final MessageRepository messages;

    public ChatwootJson(MessageRepository messages) {
        this.messages = messages;
    }

    public Map<String, Object> conversation(Conversation conversation) {
        Map<String, Object> json = new LinkedHashMap<>();
        Map<String, Object> meta = new LinkedHashMap<>();
        if (conversation.getContact() != null) {
            meta.put("sender", contact(conversation.getContact(), false));
        }
        meta.put("channel", conversation.getInbox() == null ? null : conversation.getInbox().getChannelType());
        if (conversation.getAssignee() != null) {
            meta.put("assignee", agent(conversation.getAssignee(), conversation.getAccountId()));
            meta.put("assignee_type", "User");
        }
        meta.put("hmac_verified", conversation.getContactInbox() != null && conversation.getContactInbox().isHmacVerified());
        json.put("meta", meta);
        json.put("id", conversation.getDisplayId());

        List<Message> latest = messages.findLatestFirst(conversation.getId());
        if (latest.isEmpty()) {
            json.put("messages", List.of());
        } else {
            json.put("messages", List.of(message(latest.getFirst())));
        }

        json.put("account_id", conversation.getAccountId());
        json.put("uuid", conversation.getUuid() == null ? null : conversation.getUuid().toString());
        json.put("additional_attributes", conversation.getAdditionalAttributes());
        json.put("agent_last_seen_at", unix(conversation.getAgentLastSeenAt()));
        json.put("assignee_last_seen_at", unix(conversation.getAssigneeLastSeenAt()));
        json.put("can_reply", canReply(conversation));
        Map<String, Object> contactInfoRequest = new LinkedHashMap<>();
        contactInfoRequest.put("available", false);
        contactInfoRequest.put("reason", null);
        contactInfoRequest.put("delivery_mode", null);
        json.put("contact_info_request", contactInfoRequest);
        json.put("contact_last_seen_at", unix(conversation.getContactLastSeenAt()));
        json.put("custom_attributes", conversation.getCustomAttributes());
        json.put("inbox_id", conversation.getInboxId());
        json.put("labels", conversation.labelList());
        json.put("muted", conversation.muted());
        json.put("snoozed_until", conversation.getSnoozedUntil());
        json.put("status", conversation.statusName());
        json.put("created_at", unix(conversation.getCreatedAt()));
        json.put("updated_at", unixFloat(conversation.getUpdatedAt()));
        json.put("timestamp", unix(conversation.getLastActivityAt()));
        json.put("first_reply_created_at", unix(conversation.getFirstReplyCreatedAt()));
        json.put("unread_count", unreadCount(conversation));
        List<Message> nonActivity = messages.findNonActivityDesc(conversation.getId(), conversation.getAccountId());
        json.put("last_non_activity_message", nonActivity.isEmpty() ? null : message(nonActivity.getFirst()));
        json.put("last_activity_at", unix(conversation.getLastActivityAt()));
        json.put("priority", conversation.priorityName());
        json.put("waiting_since", unix(conversation.getWaitingSince()));
        json.put("sla_policy_id", (Object) null);
        return json;
    }

    public Map<String, Object> message(Message message) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", message.getId());
        json.put("content", message.getContent());
        json.put("inbox_id", message.getInboxId());
        if (message.getEchoId() != null) {
            json.put("echo_id", message.getEchoId());
        }
        Conversation conversation = message.getConversation();
        json.put("conversation_id", conversation == null ? null : conversation.getDisplayId());
        json.put("message_type", message.getMessageType());
        json.put("content_type", message.contentTypeName());
        json.put("status", message.statusName());
        json.put("content_attributes", message.getContentAttributes());
        json.put("created_at", unix(message.getCreatedAt()));
        json.put("private", message.isPrivateMessage());
        json.put("source_id", message.getSourceId());
        Object sender = sender(message);
        if (sender != null) {
            json.put("sender", sender);
        }
        return json;
    }

    public Map<String, Object> contact(Contact contact, boolean includeTimestamps) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("additional_attributes", contact.getAdditionalAttributes());
        json.put("availability_status", "offline");
        json.put("email", contact.getEmail());
        json.put("id", contact.getId());
        json.put("name", contact.getName());
        json.put("phone_number", contact.getPhoneNumber());
        json.put("blocked", contact.isBlocked());
        json.put("identifier", contact.getIdentifier());
        json.put("thumbnail", "");
        json.put("custom_attributes", contact.getCustomAttributes());
        json.put("type", "contact");
        if (includeTimestamps) {
            json.put("last_activity_at", unix(contact.getLastActivityAt()));
            json.put("created_at", unix(contact.getCreatedAt()));
        }
        return json;
    }

    public Map<String, Object> agent(User user, Integer accountId) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", user.getId());
        json.put("account_id", accountId);
        json.put("availability_status", user.availabilityStatus());
        json.put("auto_offline", user.getCurrentAccountUser() == null || user.getCurrentAccountUser().isAutoOffline());
        json.put("confirmed", true);
        json.put("email", user.getEmail());
        json.put("provider", user.getProvider());
        json.put("available_name", user.availableName());
        json.put("name", user.getName());
        json.put("role", user.getCurrentAccountUser() == null ? "agent" : user.getCurrentAccountUser().roleName());
        json.put("thumbnail", "");
        json.put("type", "user");
        return json;
    }

    public Map<String, Object> inbox(Inbox inbox) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", inbox.getId());
        json.put("avatar_url", "");
        json.put("channel_id", inbox.getChannelId());
        json.put("name", inbox.getName());
        json.put("channel_type", inbox.getChannelType());
        json.put("greeting_enabled", inbox.getGreetingEnabled());
        json.put("greeting_message", inbox.getGreetingMessage());
        json.put("working_hours_enabled", inbox.getWorkingHoursEnabled());
        json.put("enable_email_collect", inbox.getEnableEmailCollect());
        json.put("csat_survey_enabled", inbox.getCsatSurveyEnabled());
        json.put("csat_config", inbox.getCsatConfig());
        json.put("enable_auto_assignment", inbox.getEnableAutoAssignment());
        json.put("auto_assignment_config", inbox.getAutoAssignmentConfig());
        json.put("out_of_office_message", inbox.getOutOfOfficeMessage());
        json.put("working_hours", List.of());
        json.put("timezone", inbox.getTimezone());
        json.put("allow_messages_after_resolved", inbox.getAllowMessagesAfterResolved());
        json.put("lock_to_single_conversation", inbox.isLockToSingleConversation());
        json.put("sender_name_type", inbox.senderNameTypeName());
        json.put("business_name", inbox.getBusinessName());
        return json;
    }

    public Map<String, Object> account(Account account) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("settings", account.getSettings());
        json.put("created_at", account.getCreatedAt());
        if (!account.getCustomAttributes().isEmpty()) {
            json.put("custom_attributes", account.getCustomAttributes());
        }
        json.put("domain", account.getDomain());
        json.put("features", defaultFeatures());
        json.put("id", account.getId());
        json.put("locale", account.localeCode());
        json.put("name", account.getName());
        json.put("support_email", account.getSupportEmail());
        json.put("status", account.statusName());
        json.put("cache_keys", Map.of("label", "0", "inbox", "0", "team", "0", "canned_response", "0"));
        return json;
    }

    public Map<String, Object> userProfile(User user, List<AccountUser> memberships) {
        AccountUser active = memberships.stream()
                .max((a, b) -> {
                    Instant left = a.getActiveAt() == null ? Instant.EPOCH : a.getActiveAt();
                    Instant right = b.getActiveAt() == null ? Instant.EPOCH : b.getActiveAt();
                    return left.compareTo(right);
                })
                .orElse(null);
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("access_token", "");
        json.put("account_id", active == null ? null : active.getAccount().getId());
        json.put("available_name", user.availableName());
        json.put("avatar_url", "");
        json.put("confirmed", true);
        json.put("display_name", user.getDisplayName());
        json.put("message_signature", user.getMessageSignature());
        json.put("email", user.getEmail());
        json.put("id", user.getId());
        json.put("inviter_id", (Object) null);
        json.put("name", user.getName());
        json.put("provider", user.getProvider());
        json.put("pubsub_token", user.getPubsubToken());
        json.put("role", active == null ? "agent" : active.roleName());
        json.put("ui_settings", user.getUiSettings());
        json.put("uid", user.getUid());
        json.put("type", user.getType());
        List<Map<String, Object>> accounts = new ArrayList<>();
        for (AccountUser membership : memberships) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", membership.getAccount().getId());
            row.put("name", membership.getAccount().getName());
            row.put("status", membership.getAccount().statusName());
            row.put("active_at", membership.getActiveAt());
            row.put("role", membership.roleName());
            row.put("permissions", membership.administrator() ? List.of("*") : List.of());
            row.put("availability", membership.availabilityName());
            row.put("availability_status", membership.availabilityName());
            row.put("auto_offline", membership.isAutoOffline());
            row.put("api_and_webhooks", true);
            accounts.add(row);
        }
        json.put("accounts", accounts);
        return json;
    }

    public Map<String, Object> defaultFeatures() {
        Map<String, Object> features = new LinkedHashMap<>();
        features.put("inbox_management", true);
        features.put("agent_management", true);
        features.put("team_management", true);
        features.put("labels", true);
        features.put("channel_facebook", false);
        features.put("captain_integration", false);
        features.put("captain_tasks", false);
        features.put("conversation_unread_counts", false);
        return features;
    }

    private Object sender(Message message) {
        if ("User".equals(message.getSenderType()) && message.getSenderUser() != null) {
            return agent(message.getSenderUser(), message.getAccountId());
        }
        if ("Contact".equals(message.getSenderType()) && message.getSenderContact() != null) {
            return contact(message.getSenderContact(), false);
        }
        return null;
    }

    private boolean canReply(Conversation conversation) {
        Inbox inbox = conversation.getInbox();
        if (inbox == null || inbox.apiChannel()) {
            return true;
        }
        List<Message> incoming = messages.findIncomingAsc(conversation.getId(), conversation.getAccountId());
        if (incoming.isEmpty()) {
            return false;
        }
        Instant last = incoming.getLast().getCreatedAt();
        return Instant.now().isBefore(last.plus(Duration.ofHours(24)));
    }

    private long unreadCount(Conversation conversation) {
        return messages.countUnreadIncoming(conversation.getId(), conversation.getAgentLastSeenAt());
    }

    public static long unix(Instant instant) {
        return instant == null ? 0L : instant.getEpochSecond();
    }

    public static double unixFloat(Instant instant) {
        if (instant == null) {
            return 0d;
        }
        return instant.getEpochSecond() + instant.getNano() / 1_000_000_000d;
    }
}
