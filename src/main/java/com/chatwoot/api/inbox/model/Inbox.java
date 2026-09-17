package com.chatwoot.api.inbox.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "inboxes")
public class Inbox {

    public static final String CHANNEL_API = "Channel::Api";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "channel_id", nullable = false)
    private Integer channelId;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "channel_type")
    private String channelType = CHANNEL_API;

    @Column(name = "enable_auto_assignment")
    private Boolean enableAutoAssignment = true;

    @Column(name = "greeting_enabled")
    private Boolean greetingEnabled = false;

    @Column(name = "greeting_message")
    private String greetingMessage;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "working_hours_enabled")
    private Boolean workingHoursEnabled = false;

    @Column(name = "out_of_office_message")
    private String outOfOfficeMessage;

    private String timezone = "UTC";

    @Column(name = "enable_email_collect")
    private Boolean enableEmailCollect = true;

    @Column(name = "csat_survey_enabled")
    private Boolean csatSurveyEnabled = false;

    @Column(name = "allow_messages_after_resolved")
    private Boolean allowMessagesAfterResolved = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "auto_assignment_config")
    private Map<String, Object> autoAssignmentConfig = new HashMap<>();

    @Column(name = "lock_to_single_conversation", nullable = false)
    private boolean lockToSingleConversation = false;

    @Column(name = "sender_name_type", nullable = false)
    private Integer senderNameType = 0;

    @Column(name = "business_name")
    private String businessName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "csat_config")
    private Map<String, Object> csatConfig = new HashMap<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public Integer getChannelId() {
        return channelId;
    }

    public void setChannelId(Integer channelId) {
        this.channelId = channelId;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChannelType() {
        return channelType;
    }

    public void setChannelType(String channelType) {
        this.channelType = channelType;
    }

    public Boolean getEnableAutoAssignment() {
        return enableAutoAssignment;
    }

    public Boolean getGreetingEnabled() {
        return greetingEnabled;
    }

    public String getGreetingMessage() {
        return greetingMessage;
    }

    public Boolean getWorkingHoursEnabled() {
        return workingHoursEnabled;
    }

    public String getOutOfOfficeMessage() {
        return outOfOfficeMessage;
    }

    public String getTimezone() {
        return timezone;
    }

    public Boolean getEnableEmailCollect() {
        return enableEmailCollect;
    }

    public Boolean getCsatSurveyEnabled() {
        return csatSurveyEnabled;
    }

    public Boolean getAllowMessagesAfterResolved() {
        return allowMessagesAfterResolved;
    }

    public Map<String, Object> getAutoAssignmentConfig() {
        return autoAssignmentConfig == null ? Map.of() : autoAssignmentConfig;
    }

    public boolean isLockToSingleConversation() {
        return lockToSingleConversation;
    }

    public Integer getSenderNameType() {
        return senderNameType;
    }

    public String senderNameTypeName() {
        return senderNameType != null && senderNameType == 1 ? "friendly" : "friendly";
    }

    public String getBusinessName() {
        return businessName;
    }

    public Map<String, Object> getCsatConfig() {
        return csatConfig == null ? Map.of() : csatConfig;
    }

    public boolean apiChannel() {
        return CHANNEL_API.equals(channelType);
    }
}
