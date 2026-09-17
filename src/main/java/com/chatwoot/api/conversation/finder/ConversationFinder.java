package com.chatwoot.api.conversation.finder;

import com.chatwoot.api.account.model.AccountUser;
import com.chatwoot.api.conversation.model.Conversation;
import com.chatwoot.api.inbox.model.InboxMember;
import com.chatwoot.api.account.model.User;
import com.chatwoot.api.inbox.repository.InboxMemberRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Port of Chatwoot ConversationFinder + PermissionFilterService + SortService.
 */
@Service
public class ConversationFinder {

    private final EntityManager entityManager;
    private final InboxMemberRepository inboxMembers;
    private final int pageSize;

    public ConversationFinder(
            EntityManager entityManager,
            InboxMemberRepository inboxMembers,
            @Value("${chatwoot.conversation-results-per-page:25}") int pageSize
    ) {
        this.entityManager = entityManager;
        this.inboxMembers = inboxMembers;
        this.pageSize = pageSize;
    }

    public ConversationFinderResult perform(User user, AccountUser membership, ConversationListParams params) {
        long mine = count(user, membership, params, "me");
        long unassigned = count(user, membership, params, "unassigned");
        long all = count(user, membership, params, "all");
        long assigned = all - unassigned;

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Conversation> cq = cb.createQuery(Conversation.class);
        Root<Conversation> root = cq.from(Conversation.class);
        root.fetch("inbox", JoinType.LEFT);
        root.fetch("contact", JoinType.LEFT);
        root.fetch("assignee", JoinType.LEFT);
        root.fetch("contactInbox", JoinType.LEFT);
        cq.distinct(true);
        List<Predicate> predicates = predicatesFor(user, membership, params, params.assigneeType(), cb, root);
        cq.where(predicates.toArray(Predicate[]::new));
        applySort(cb, cq, root, params.sortBy());
        TypedQuery<Conversation> query = entityManager.createQuery(cq);
        int page = params.page() == null || params.page() < 1 ? 1 : params.page();
        query.setFirstResult((page - 1) * pageSize);
        query.setMaxResults(pageSize);
        return new ConversationFinderResult(query.getResultList(), mine, assigned, unassigned, all);
    }

    public ConversationFinderResult performMetaOnly(User user, AccountUser membership, ConversationListParams params) {
        long mine = count(user, membership, params, "me");
        long unassigned = count(user, membership, params, "unassigned");
        long all = count(user, membership, params, "all");
        return new ConversationFinderResult(List.of(), mine, all - unassigned, unassigned, all);
    }

    private long count(User user, AccountUser membership, ConversationListParams params, String assigneeType) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);
        Root<Conversation> root = cq.from(Conversation.class);
        cq.select(cb.count(root));
        cq.where(predicatesFor(user, membership, params, assigneeType, cb, root).toArray(Predicate[]::new));
        return entityManager.createQuery(cq).getSingleResult();
    }

    private List<Predicate> predicatesFor(
            User user,
            AccountUser membership,
            ConversationListParams params,
            String assigneeType,
            CriteriaBuilder cb,
            Root<Conversation> root
    ) {
        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("accountId"), membership.getAccount().getId()));

        List<Integer> inboxIds = assignedInboxIds(user, membership, params.inboxId());
        if (inboxIds.isEmpty()) {
            predicates.add(cb.disjunction());
        } else {
            predicates.add(root.get("inbox").get("id").in(inboxIds));
        }

        if (params.q() == null) {
            String status = params.status() == null ? "open" : params.status();
            if (!"all".equals(status)) {
                Integer statusCode = Conversation.statusFromName(status);
                if (statusCode != null) {
                    predicates.add(cb.equal(root.get("status"), statusCode));
                }
            }
        }

        String type = assigneeType == null ? params.assigneeType() : assigneeType;
        if ("me".equals(type)) {
            predicates.add(cb.equal(root.get("assignee").get("id"), user.getId()));
        } else if ("unassigned".equals(type)) {
            predicates.add(cb.isNull(root.get("assignee")));
            predicates.add(cb.isNull(root.get("assigneeAgentBotId")));
        } else if ("assigned".equals(type)) {
            predicates.add(cb.or(cb.isNotNull(root.get("assignee")), cb.isNotNull(root.get("assigneeAgentBotId"))));
        }

        return predicates;
    }

    private List<Integer> assignedInboxIds(User user, AccountUser membership, Integer inboxId) {
        List<Integer> ids;
        if (membership.administrator()) {
            ids = entityManager.createQuery(
                            "select i.id from Inbox i where i.accountId = :accountId", Integer.class)
                    .setParameter("accountId", membership.getAccount().getId())
                    .getResultList();
        } else {
            ids = inboxMembers.findByUserId(user.getId()).stream()
                    .map(InboxMember::getInboxId)
                    .toList();
        }
        if (inboxId != null) {
            ids = ids.stream().filter(inboxId::equals).toList();
        }
        return ids;
    }

    private void applySort(CriteriaBuilder cb, CriteriaQuery<Conversation> cq, Root<Conversation> root, String sortBy) {
        String key = sortBy == null || sortBy.isBlank() ? "last_activity_at_desc" : sortBy.toLowerCase(Locale.ROOT);
        switch (key) {
            case "last_activity_at_asc", "sort_on_created_at" -> cq.orderBy(cb.asc(root.get("lastActivityAt")));
            case "created_at_asc" -> cq.orderBy(cb.asc(root.get("createdAt")), cb.asc(root.get("id")));
            case "created_at_desc" -> cq.orderBy(cb.desc(root.get("createdAt")), cb.desc(root.get("id")));
            default -> cq.orderBy(cb.desc(root.get("lastActivityAt")));
        }
    }
}
