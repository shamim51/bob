package com.chatwoot.api.domain;

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
@Table(name = "contacts")
public class Contact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name = "";

    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "account_id", nullable = false)
    private Integer accountId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "additional_attributes")
    private Map<String, Object> additionalAttributes = new HashMap<>();

    private String identifier;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes")
    private Map<String, Object> customAttributes = new HashMap<>();

    @Column(name = "last_activity_at")
    private Instant lastActivityAt;

    @Column(name = "contact_type")
    private Integer contactType = 0;

    @Column(nullable = false)
    private boolean blocked = false;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Map<String, Object> getAdditionalAttributes() {
        return additionalAttributes == null ? Map.of() : additionalAttributes;
    }

    public String getIdentifier() {
        return identifier;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes == null ? Map.of() : customAttributes;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public boolean isBlocked() {
        return blocked;
    }

    public void setBlocked(boolean blocked) {
        this.blocked = blocked;
    }
}
