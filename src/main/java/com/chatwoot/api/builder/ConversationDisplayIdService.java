package com.chatwoot.api.builder;

import com.chatwoot.api.domain.ConversationDisplayIdCounter;
import com.chatwoot.api.repo.ConversationDisplayIdCounterRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConversationDisplayIdService {

    private final ConversationDisplayIdCounterRepository counters;

    public ConversationDisplayIdService(ConversationDisplayIdCounterRepository counters) {
        this.counters = counters;
    }

    @Transactional
    public int next(Integer accountId) {
        ConversationDisplayIdCounter counter = counters.findByAccountId(accountId).orElseGet(() -> {
            ConversationDisplayIdCounter created = new ConversationDisplayIdCounter();
            created.setAccountId(accountId);
            created.setLastValue(0);
            return created;
        });
        int next = counter.getLastValue() + 1;
        counter.setLastValue(next);
        counters.save(counter);
        return next;
    }
}
