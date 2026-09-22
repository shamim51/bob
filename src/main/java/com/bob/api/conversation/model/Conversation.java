package com.bob.api.conversation.model;

import com.bob.api.contact.model.Contact;
import com.bob.api.contact.model.ContactInbox;
import com.bob.api.inbox.model.Inbox;
import com.bob.api.account.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "conversations")
public class Conversation {

    public static final int STATUS_OPEN = 0;
    public static final int STATUS_RESOLVED = 1;
    public static final int STATUS_PENDING = 2;
    public static final int STATUS_SNOOZED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inbox_id", nullable = false)
    private Inbox inbox;

    @Column(nullable = false)
    private Integer status = STATUS_OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Contact contact;

    @Column(name = "display_id", nullable = false)
    private Integer displayId;

    @Column(name = "contact_last_seen_at")
    private Instant contactLastSeenAt;

    @Column(name = "agent_last_seen_at")
    private Instant agentLastSeenAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_attributes")
    private Map<String, Object> additionalAttributes = new HashMap<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_inbox_id")
    private ContactInbox contactInbox;

    @Column(nullable = false)
    private UUID uuid;

    private String identifier;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "team_id")
    private Long teamId;

    @Column(name = "campaign_id")
    private Long campaignId;

    @Column(name = "snoozed_until")
    private Instant snoozedUntil;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes")
    private Map<String, Object> customAttributes = new HashMap<>();

    @Column(name = "assignee_last_seen_at")
    private Instant assigneeLastSeenAt;

    @Column(name = "first_reply_created_at")
    private Instant firstReplyCreatedAt;

    private Integer priority;

    @Column(name = "sla_policy_id")
    private Long slaPolicyId;

    @Column(name = "waiting_since")
    private Instant waitingSince;

    @Column(name = "cached_label_list")
    private String cachedLabelList;

    @Column(name = "assignee_agent_bot_id")
    private Long assigneeAgentBotId;

    @Column(name = "ai_assignee_type")
    private String aiAssigneeType;

    @Column(name = "status_changed_at")
    private Instant statusChangedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (lastActivityAt == null) {
            lastActivityAt = createdAt;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Inbox getInbox() {
        return inbox;
    }

    public void setInbox(Inbox inbox) {
        this.inbox = inbox;
    }

    public Integer getInboxId() {
        return inbox == null ? null : inbox.getId();
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String statusName() {
        return switch (status == null ? 0 : status) {
            case STATUS_RESOLVED -> "resolved";
            case STATUS_PENDING -> "pending";
            case STATUS_SNOOZED -> "snoozed";
            default -> "open";
        };
    }

    public static Integer statusFromName(String name) {
        if (name == null) {
            return STATUS_OPEN;
        }
        return switch (name) {
            case "resolved" -> STATUS_RESOLVED;
            case "pending" -> STATUS_PENDING;
            case "snoozed" -> STATUS_SNOOZED;
            case "all" -> null;
            default -> STATUS_OPEN;
        };
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Contact getContact() {
        return contact;
    }

    public void setContact(Contact contact) {
        this.contact = contact;
    }

    public Integer getDisplayId() {
        return displayId;
    }

    public void setDisplayId(Integer displayId) {
        this.displayId = displayId;
    }

    public Instant getContactLastSeenAt() {
        return contactLastSeenAt;
    }

    public Instant getAgentLastSeenAt() {
        return agentLastSeenAt;
    }

    public void setAgentLastSeenAt(Instant agentLastSeenAt) {
        this.agentLastSeenAt = agentLastSeenAt;
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes == null ? Map.of() : additionalAttributes;
    }

    public ContactInbox getContactInbox() {
        return contactInbox;
    }

    public void setContactInbox(ContactInbox contactInbox) {
        this.contactInbox = contactInbox;
    }

    public UUID getUuid() {
        return uuid;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt == null ? createdAt : lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Instant getSnoozedUntil() {
        return snoozedUntil;
    }

    public void setSnoozedUntil(Instant snoozedUntil) {
        this.snoozedUntil = snoozedUntil;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes == null ? Map.of() : customAttributes;
    }

    public Instant getAssigneeLastSeenAt() {
        return assigneeLastSeenAt;
    }

    public Instant getFirstReplyCreatedAt() {
        return firstReplyCreatedAt;
    }

    public void setFirstReplyCreatedAt(Instant firstReplyCreatedAt) {
        this.firstReplyCreatedAt = firstReplyCreatedAt;
    }

    public Integer getPriority() {
        return priority;
    }

    public String priorityName() {
        if (priority == null) {
            return null;
        }
        return switch (priority) {
            case 0 -> "low";
            case 1 -> "medium";
            case 2 -> "high";
            case 3 -> "urgent";
            default -> null;
        };
    }

    public Instant getWaitingSince() {
        return waitingSince;
    }

    public String getCachedLabelList() {
        return cachedLabelList;
    }

    public void setCachedLabelList(String cachedLabelList) {
        this.cachedLabelList = cachedLabelList;
    }

    public List<String> labelList() {
        if (cachedLabelList == null || cachedLabelList.isBlank()) {
            return List.of();
        }
        return List.of(cachedLabelList.split("\\s*,\\s*"));
    }

    public Long getAssigneeAgentBotId() {
        return assigneeAgentBotId;
    }

    public boolean muted() {
        return contact != null && contact.isBlocked();
    }
}
