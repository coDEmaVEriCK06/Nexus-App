package com.nexus.chat;

import com.nexus.chat.dto.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Query("select max(m.id) from Message m where m.conversation.id = :conversationId")
    Long findMaxMessageId(@Param("conversationId") Long conversationId);

    /**
     * Unread counts for a user across a set of conversations, in one query. Counts messages
     * from other people that the member is allowed to see (sent on or after they joined) whose
     * id is past the user's read watermark. Each row is [conversationId, count].
     */
    @Query("""
            select m.conversation.id, count(m)
            from Message m, ConversationMember cm
            where cm.conversation.id = m.conversation.id
              and cm.user.id = :userId
              and m.conversation.id in :conversationIds
              and m.sender.id <> :userId
              and m.createdAt >= cm.joinedAt
              and (cm.lastReadMessageId is null or m.id > cm.lastReadMessageId)
            group by m.conversation.id
            """)
    List<Object[]> countUnreadByConversation(@Param("userId") Long userId,
                                             @Param("conversationIds") List<Long> conversationIds);

    /**
     * Messages in a conversation visible to a member who joined at {@code since}: only those
     * sent on or after their join time, newest first. Prevents a newly added member from
     * reading history that predates their membership.
     */
    @Query(value = """
            select new com.nexus.chat.dto.MessageResponse(
                m.id, m.conversation.id, m.sender.username, m.content, m.createdAt, m.type)
            from Message m
            where m.conversation.id = :conversationId and m.createdAt >= :since
            order by m.createdAt desc
            """,
            countQuery = """
            select count(m) from Message m
            where m.conversation.id = :conversationId and m.createdAt >= :since
            """)
    Page<MessageResponse> findResponsesByConversationId(@Param("conversationId") Long conversationId,
                                                        @Param("since") OffsetDateTime since,
                                                        Pageable pageable);
}
