package com.nexus.realtime;

import com.nexus.chat.MessagePostedEvent;
import com.nexus.chat.dto.MessageResponse;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageBroadcaster {

    private static final String USER_QUEUE = "/queue/messages";

    private final SimpMessagingTemplate messagingTemplate;

    public MessageBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagePosted(MessagePostedEvent event) {
        MessageResponse message = event.message();
        for (String username : event.recipientUsernames()) {
            messagingTemplate.convertAndSendToUser(username, USER_QUEUE, message);
        }
    }
}
