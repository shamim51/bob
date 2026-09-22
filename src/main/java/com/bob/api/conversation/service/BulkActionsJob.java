package com.bob.api.conversation.service;

import com.bob.api.account.model.AccountUser;
import com.bob.api.account.model.User;
import com.bob.api.account.repository.UserRepository;
import com.bob.api.conversation.dto.BulkActionFields;
import com.bob.api.conversation.dto.BulkActionLabels;
import com.bob.api.conversation.dto.BulkActionRequest;
import com.bob.api.conversation.model.Conversation;
import com.bob.api.conversation.repository.ConversationRepository;
import com.bob.api.inbox.model.InboxMember;
import com.bob.api.inbox.repository.InboxMemberRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BulkActionsJob {

    private static final String MODEL_CONVERSATION = "Conversation";

    private final ConversationRepository conversations;
    private final InboxMemberRepository inboxMembers;
    private final UserRepository users;

    public BulkActionsJob(
            ConversationRepository conversations,
            InboxMemberRepository inboxMembers,
            UserRepository users
    ) {
        this.conversations = conversations;
        this.inboxMembers = inboxMembers;
        this.users = users;
    }

    @Transactional
    public void perform(Integer accountId, User user, AccountUser membership, BulkActionRequest params) {
        List<Conversation> records = recordsToUpdated(accountId, user, membership, params);
        bulkRemoveLabels(records, params.labels());
        bulkConversationUpdate(records, params);
    }

    private List<Conversation> recordsToUpdated(
            Integer accountId,
            User user,
            AccountUser membership,
            BulkActionRequest params
    ) {
        if (params.ids() == null || params.ids().isEmpty()) {
            return List.of();
        }
        if (!MODEL_CONVERSATION.equals(camelize(params.type()))) {
            return List.of();
        }
        List<Conversation> records = conversations.findByAccountIdAndDisplayIdIn(accountId, params.ids());
        if (membership.administrator()) {
            return records;
        }
        Set<Integer> inboxIds = inboxMembers.findByUserId(user.getId()).stream()
                .map(InboxMember::getInboxId)
                .collect(Collectors.toSet());
        return records.stream()
                .filter(conversation -> conversation.getInbox() != null && inboxIds.contains(conversation.getInbox().getId()))
                .toList();
    }

    private void bulkRemoveLabels(List<Conversation> records, BulkActionLabels labels) {
        if (labels == null || labels.remove() == null || labels.remove().isEmpty()) {
            return;
        }
        Set<String> toRemove = new LinkedHashSet<>(labels.remove());
        for (Conversation conversation : records) {
            List<String> remaining = conversation.labelList().stream()
                    .filter(label -> !toRemove.contains(label))
                    .toList();
            conversation.setCachedLabelList(joinLabels(remaining));
        }
    }

    private void bulkConversationUpdate(List<Conversation> records, BulkActionRequest params) {
        for (Conversation conversation : records) {
            bulkAddLabels(conversation, params.labels());
            bulkSnoozedUntil(conversation, params.snoozedUntil());
            applyFields(conversation, params.fields());
        }
        conversations.saveAll(records);
    }

    private void bulkAddLabels(Conversation conversation, BulkActionLabels labels) {
        if (labels == null || labels.add() == null || labels.add().isEmpty()) {
            return;
        }
        LinkedHashSet<String> combined = new LinkedHashSet<>(conversation.labelList());
        combined.addAll(labels.add());
        conversation.setCachedLabelList(joinLabels(combined.stream().toList()));
    }

    private void bulkSnoozedUntil(Conversation conversation, Object snoozedUntil) {
        Instant parsed = parseUnixSeconds(snoozedUntil);
        if (parsed != null) {
            conversation.setSnoozedUntil(parsed);
        }
    }

    private void applyFields(Conversation conversation, BulkActionFields fields) {
        if (fields == null) {
            return;
        }
        if (fields.getStatus() != null) {
            Integer status = Conversation.statusFromName(fields.getStatus());
            if (status != null) {
                conversation.setStatus(status);
            }
        }
        if (fields.assigneeIdPresent()) {
            if (fields.getAssigneeId() == null) {
                conversation.setAssignee(null);
            } else {
                users.findById(fields.getAssigneeId()).ifPresent(conversation::setAssignee);
            }
        }
        if (fields.teamIdPresent()) {
            Long teamId = fields.getTeamId();
            conversation.setTeamId(teamId == null || teamId == 0L ? null : teamId);
        }
    }

    public static String camelize(String type) {
        if (type == null || type.isBlank()) {
            return "";
        }
        String[] parts = type.split("_");
        StringBuilder camelized = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            camelized.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                camelized.append(part.substring(1));
            }
        }
        return camelized.toString();
    }

    private static Instant parseUnixSeconds(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return Instant.ofEpochSecond(number.longValue());
        }
        String text = raw.toString();
        if (text.isBlank()) {
            return null;
        }
        try {
            return Instant.ofEpochSecond(Long.parseLong(text));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static String joinLabels(List<String> labels) {
        if (labels == null || labels.isEmpty()) {
            return null;
        }
        List<String> trimmed = new ArrayList<>();
        for (String label : labels) {
            if (label != null && !label.isBlank()) {
                trimmed.add(label);
            }
        }
        return trimmed.isEmpty() ? null : String.join(",", trimmed);
    }
}
