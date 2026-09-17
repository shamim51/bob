package com.chatwoot.api.finder;

import com.chatwoot.api.domain.Conversation;
import com.chatwoot.api.domain.Message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Port of Chatwoot MessageFinder: latest 20 asc, before 20 reversed, after 100.
 */
@Service
public class MessageFinder {

    public static final int MESSAGE_ID_MAX = 2_147_483_647;

    private final EntityManager entityManager;

    public MessageFinder(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public List<Message> perform(Conversation conversation, Integer after, Integer before) {
        if (oversized(after)) {
            return List.of();
        }
        if (after != null && before != null) {
            return messagesBetween(conversation, normalized(after), before);
        }
        if (before != null) {
            return messagesBefore(conversation, before);
        }
        if (after != null) {
            return messagesAfter(conversation, normalized(after));
        }
        return messagesLatest(conversation);
    }

    private List<Message> messagesAfter(Conversation conversation, int afterId) {
        TypedQuery<Message> query = entityManager.createQuery(
                "select m from Message m where m.conversation.id = :cid and m.id > :after order by m.createdAt asc, m.id asc",
                Message.class);
        query.setParameter("cid", conversation.getId());
        query.setParameter("after", afterId);
        query.setMaxResults(100);
        return query.getResultList();
    }

    private List<Message> messagesBefore(Conversation conversation, Integer beforeId) {
        if (oversized(beforeId)) {
            return messagesLatest(conversation);
        }
        int id = normalized(beforeId);
        TypedQuery<Message> query = entityManager.createQuery(
                "select m from Message m where m.conversation.id = :cid and m.id < :before order by m.createdAt desc, m.id desc",
                Message.class);
        query.setParameter("cid", conversation.getId());
        query.setParameter("before", id);
        query.setMaxResults(20);
        List<Message> rows = new ArrayList<>(query.getResultList());
        Collections.reverse(rows);
        return rows;
    }

    private List<Message> messagesBetween(Conversation conversation, int afterId, Integer beforeId) {
        String jpql = "select m from Message m where m.conversation.id = :cid and m.id >= :after";
        if (!oversized(beforeId)) {
            jpql += " and m.id < :before";
        }
        jpql += " order by m.createdAt asc, m.id asc";
        TypedQuery<Message> query = entityManager.createQuery(jpql, Message.class);
        query.setParameter("cid", conversation.getId());
        query.setParameter("after", afterId);
        if (!oversized(beforeId)) {
            query.setParameter("before", normalized(beforeId));
        }
        query.setMaxResults(1000);
        return query.getResultList();
    }

    private List<Message> messagesLatest(Conversation conversation) {
        TypedQuery<Message> query = entityManager.createQuery(
                "select m from Message m where m.conversation.id = :cid order by m.createdAt desc, m.id desc",
                Message.class);
        query.setParameter("cid", conversation.getId());
        query.setMaxResults(20);
        List<Message> rows = new ArrayList<>(query.getResultList());
        Collections.reverse(rows);
        return rows;
    }

    private int normalized(Integer value) {
        return Math.clamp(value.longValue(), 0, MESSAGE_ID_MAX);
    }

    private boolean oversized(Integer value) {
        return value != null && value > MESSAGE_ID_MAX;
    }
}
