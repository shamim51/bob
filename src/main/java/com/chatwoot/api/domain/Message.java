package com.chatwoot.api.domain;

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
import jakarta.persistence.Transient;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "messages")
public class Message {

    public static final int TYPE_INCOMING = 0;
    public static final int TYPE_OUTGOING = 1;
    public static final int TYPE_ACTIVITY = 2;
    public static final int TYPE_TEMPLATE = 3;

    public static final int STATUS_SENT = 0;
    public static final int STATUS_DELIVERED = 1;
    public static final int STATUS_READ = 2;
    public static final int STATUS_FAILED = 3;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String content;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(name = "inbox_id", nullable = false)
    private Integer inboxId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(name = "message_type", nullable = false)
    private Integer messageType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "private", nullable = false)
    private boolean privateMessage = false;

    private Integer status = STATUS_SENT;

    @Column(name = "source_id")
    private String sourceId;

    @Column(name = "content_type", nullable = false)
    private Integer contentType = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content_attributes")
    private Map<String, Object> contentAttributes = new HashMap<>();

    @Column(name = "sender_type")
    private String senderType;

    @Column(name = "sender_id")
    private Long senderId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_attributes")
    private Map<String, Object> additionalAttributes = new HashMap<>();

    @Transient
    private String echoId;

    @Transient
    private User senderUser;

    @Transient
    private Contact senderContact;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        if (status == null) {
            status = STATUS_SENT;
        }
        if (contentType == null) {
            contentType = 0;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getInboxId() {
        return inboxId;
    }

    public void setInboxId(Integer inboxId) {
        this.inboxId = inboxId;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPrivateMessage() {
        return privateMessage;
    }

    public void setPrivateMessage(boolean privateMessage) {
        this.privateMessage = privateMessage;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String statusName() {
        return switch (status == null ? 0 : status) {
            case STATUS_DELIVERED -> "delivered";
            case STATUS_READ -> "read";
            case STATUS_FAILED -> "failed";
            default -> "sent";
        };
    }

    public String getSourceId() {
        return sourceId;
    }

    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
    }

    public Integer getContentType() {
        return contentType;
    }

    public void setContentType(Integer contentType) {
        this.contentType = contentType;
    }

    public String contentTypeName() {
        return switch (contentType == null ? 0 : contentType) {
            case 1 -> "input_text";
            case 2 -> "input_textarea";
            case 3 -> "input_email";
            case 4 -> "input_select";
            case 5 -> "cards";
            case 6 -> "form";
            case 7 -> "article";
            case 8 -> "incoming_email";
            case 9 -> "input_csat";
            case 10 -> "integrations";
            case 11 -> "sticker";
            case 12 -> "voice_call";
            default -> "text";
        };
    }

    public static Integer contentTypeFromName(String name) {
        if (name == null || name.isBlank()) {
            return 0;
        }
        return switch (name) {
            case "input_text" -> 1;
            case "input_textarea" -> 2;
            case "input_email" -> 3;
            case "input_select" -> 4;
            case "cards" -> 5;
            case "form" -> 6;
            case "article" -> 7;
            case "incoming_email" -> 8;
            case "input_csat" -> 9;
            case "integrations" -> 10;
            case "sticker" -> 11;
            case "voice_call" -> 12;
            default -> 0;
        };
    }

    public static Integer messageTypeFromName(String name) {
        if (name == null || name.isBlank()) {
            return TYPE_OUTGOING;
        }
        return switch (name) {
            case "incoming" -> TYPE_INCOMING;
            case "activity" -> TYPE_ACTIVITY;
            case "template" -> TYPE_TEMPLATE;
            default -> TYPE_OUTGOING;
        };
    }

    public Map<String, Object> getContentAttributes() {
        return contentAttributes == null ? Map.of() : contentAttributes;
    }

    public void setContentAttributes(Map<String, Object> contentAttributes) {
        this.contentAttributes = contentAttributes;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes == null ? Map.of() : additionalAttributes;
    }

    public String getEchoId() {
        return echoId;
    }

    public void setEchoId(String echoId) {
        this.echoId = echoId;
    }

    public User getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(User senderUser) {
        this.senderUser = senderUser;
    }

    public Contact getSenderContact() {
        return senderContact;
    }

    public void setSenderContact(Contact senderContact) {
        this.senderContact = senderContact;
    }

    public boolean incoming() {
        return messageType != null && messageType == TYPE_INCOMING;
    }

    public boolean activity() {
        return messageType != null && messageType == TYPE_ACTIVITY;
    }
}
