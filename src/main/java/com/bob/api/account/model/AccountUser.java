package com.bob.api.account.model;

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

import java.time.Instant;

@Entity
@Table(name = "account_users")
public class AccountUser {

    public static final int ROLE_AGENT = 0;
    public static final int ROLE_ADMINISTRATOR = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private Integer role = ROLE_AGENT;

    @Column(name = "inviter_id")
    private Long inviterId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "active_at")
    private Instant activeAt;

    @Column(nullable = false)
    private Integer availability = 0;

    @Column(name = "auto_offline", nullable = false)
    private boolean autoOffline = true;

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

    public Long getId() {
        return id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Integer getRole() {
        return role;
    }

    public void setRole(Integer role) {
        this.role = role;
    }

    public boolean administrator() {
        return role != null && role == ROLE_ADMINISTRATOR;
    }

    public String roleName() {
        return administrator() ? "administrator" : "agent";
    }

    public Instant getActiveAt() {
        return activeAt;
    }

    public void setActiveAt(Instant activeAt) {
        this.activeAt = activeAt;
    }

    public Integer getAvailability() {
        return availability;
    }

    public String availabilityName() {
        return availabilityName(availability);
    }

    public static String availabilityName(Integer availability) {
        if (availability == null) {
            return "online";
        }
        return switch (availability) {
            case 1 -> "offline";
            case 2 -> "busy";
            default -> "online";
        };
    }

    public boolean isAutoOffline() {
        return autoOffline;
    }
}
