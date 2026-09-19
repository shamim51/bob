package com.bob.api.account.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String provider = "email";

    @Column(nullable = false)
    private String uid = "";

    @Column(name = "encrypted_password", nullable = false)
    private String encryptedPassword = "";

    @Column(nullable = false)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    private String email;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "pubsub_token")
    private String pubsubToken;

    private Integer availability = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "ui_settings")
    private Map<String, Object> uiSettings = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes")
    private Map<String, Object> customAttributes = new HashMap<>();

    private String type;

    @Column(name = "message_signature")
    private String messageSignature;

    @Transient
    private AccountUser currentAccountUser;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (uid == null || uid.isBlank()) {
            uid = email == null ? "" : email;
        }
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public String getUid() {
        return uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPubsubToken() {
        return pubsubToken;
    }

    public void setPubsubToken(String pubsubToken) {
        this.pubsubToken = pubsubToken;
    }

    public Map<String, Object> getUiSettings() {
        return uiSettings == null ? Map.of() : uiSettings;
    }

    public void setUiSettings(Map<String, Object> uiSettings) {
        this.uiSettings = uiSettings == null ? new HashMap<>() : uiSettings;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public String getType() {
        return type;
    }

    public String getMessageSignature() {
        return messageSignature;
    }

    public void setMessageSignature(String messageSignature) {
        this.messageSignature = messageSignature;
    }

    public String availableName() {
        return (displayName == null || displayName.isBlank()) ? name : displayName;
    }

    public AccountUser getCurrentAccountUser() {
        return currentAccountUser;
    }

    public void setCurrentAccountUser(AccountUser currentAccountUser) {
        this.currentAccountUser = currentAccountUser;
    }

    public String availabilityStatus() {
        AccountUser membership = currentAccountUser;
        if (membership == null) {
            return availabilityName(availability);
        }
        return membership.availabilityName();
    }

    public static String availabilityName(Integer availability) {
        return AccountUser.availabilityName(availability);
    }
}
