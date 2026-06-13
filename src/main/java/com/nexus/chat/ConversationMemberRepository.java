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

    @Query("""
            select m.user.username
            from ConversationMember m
            where m.conversation.id = :conversationId and m.user.id <> :excludeUserId
            """)
    List<String> findOtherMemberUsernames(@Param("conversationId") Long conversationId,
                                          @Param("excludeUserId") Long excludeUserId);
}
