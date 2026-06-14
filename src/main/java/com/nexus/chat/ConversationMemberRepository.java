package com.nexus.chat;

import com.nexus.chat.dto.GroupMemberResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    boolean existsByConversationIdAndUserId(Long conversationId, Long userId);

    boolean existsByConversationIdAndUserIdAndRole(Long conversationId, Long userId, MemberRole role);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    long countByConversationId(Long conversationId);

    long countByConversationIdAndRole(Long conversationId, MemberRole role);

    @Query("""
            select new com.nexus.chat.dto.GroupMemberResponse(m.user.username, m.role)
            from ConversationMember m
            where m.conversation.id = :conversationId
            order by m.joinedAt asc
            """)
    List<GroupMemberResponse> findMembersByConversationId(@Param("conversationId") Long conversationId);

    @Query("""
            select m from ConversationMember m
            join fetch m.conversation
            where m.user.id = :userId
            """)
    List<ConversationMember> findMembershipsWithConversationByUserId(@Param("userId") Long userId);

    /**
     * Batched lookup of the "other" participant for a set of conversations (used to resolve
     * the display name of direct conversations without a per-conversation query). For a direct
     * conversation this returns a single row; group rows are returned too but ignored by callers.
     * Each row is [conversationId (Long), username (String)].
     */
    @Query("""
            select m.conversation.id, m.user.username
            from ConversationMember m
            where m.conversation.id in :conversationIds and m.user.id <> :userId
            """)
    List<Object[]> findPeerUsernames(@Param("conversationIds") List<Long> conversationIds,
                                     @Param("userId") Long userId);
}
