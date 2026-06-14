package com.nexus.chat;

import com.nexus.chat.dto.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    Optional<Message> findTopByConversationIdOrderByCreatedAtDesc(Long conversationId);

    @Query(value = """
            select new com.nexus.chat.dto.MessageResponse(
                m.id, m.conversation.id, m.sender.username, m.content, m.createdAt, m.type)
            from Message m
            where m.conversation.id = :conversationId
            order by m.createdAt desc
            """,
            countQuery = "select count(m) from Message m where m.conversation.id = :conversationId")
    Page<MessageResponse> findResponsesByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);
}
