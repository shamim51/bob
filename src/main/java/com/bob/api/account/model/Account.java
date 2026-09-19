package com.bob.api.account.model;

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
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private Integer locale = 0;

    private String domain;

    @Column(name = "support_email")
    private String supportEmail;

    @Column(name = "feature_flags", nullable = false)
    private Long featureFlags = 0L;

    @Column(name = "auto_resolve_duration")
    private Integer autoResolveDuration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> limits = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_attributes")
    private Map<String, Object> customAttributes = new HashMap<>();

    private Integer status = 0;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "internal_attributes")
    private Map<String, Object> internalAttributes = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column
    private Map<String, Object> settings = new HashMap<>();

    @Column(name = "feature_flags_ext_1", nullable = false)
    private Long featureFlagsExt1 = 0L;

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

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Integer getLocale() {
        return locale;
    }

    public String getDomain() {
        return domain;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes == null ? Map.of() : customAttributes;
    }

    public Map<String, Object> getSettings() {
        return settings == null ? Map.of() : settings;
    }

    public Integer getStatus() {
        return status;
    }

    public String statusName() {
        return status != null && status == 1 ? "suspended" : "active";
    }

    public String localeCode() {
        return "en";
    }
}
