package com.chatwoot.api.integration.facebook.repository;

import com.chatwoot.api.integration.facebook.model.FacebookPage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacebookPageRepository extends JpaRepository<FacebookPage, Integer> {
    boolean existsByAccountIdAndPageId(Integer accountId, String pageId);

    Optional<FacebookPage> findByAccountIdAndPageId(Integer accountId, String pageId);

    List<FacebookPage> findByPageId(String pageId);

    @Query("select p.pageId from FacebookPage p where p.accountId = :accountId")
    List<String> findPageIdsByAccountId(@Param("accountId") Integer accountId);
}
