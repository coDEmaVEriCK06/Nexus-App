package com.nexus.chat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(name = "conversations")
@Getter
@Setter
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConversationType type;

    @Column(length = 100)
    private String name;

    @Column(name = "direct_key", length = 64, unique = true)
    private String directKey;

    // Denormalized snapshot of the most recent message, maintained on every message write.
    // Lets the conversation list be served without a per-conversation "latest message" query.
    @Column(name = "last_message_at")
    private OffsetDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 8000)
    private String lastMessagePreview;

    @Column(name = "last_message_sender", length = 50)
    private String lastMessageSender;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_message_type", length = 20)
    private MessageType lastMessageType;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Conversation(ConversationType type, String name) {
        this.type = type;
        this.name = name;
    }

    /** Refreshes the denormalized last-message snapshot. Called whenever a message is saved. */
    public void applyLastMessage(Message message) {
        this.lastMessageAt = message.getCreatedAt();
        this.lastMessagePreview = message.getContent();
        this.lastMessageSender = message.getSender().getUsername();
        this.lastMessageType = message.getType();
    }
}
