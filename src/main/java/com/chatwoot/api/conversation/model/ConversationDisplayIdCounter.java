package com.chatwoot.api.conversation.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_display_id_counters")
public class ConversationDisplayIdCounter {

    @Id
    @Column(name = "account_id")
    private Integer accountId;

    @Column(name = "last_value", nullable = false)
    private Integer lastValue = 0;

    public Integer getAccountId() {
        return accountId;
    }

    public void setAccountId(Integer accountId) {
        this.accountId = accountId;
    }

    public Integer getLastValue() {
        return lastValue;
    }

    public void setLastValue(Integer lastValue) {
        this.lastValue = lastValue;
    }
}
