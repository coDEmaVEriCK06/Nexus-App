package com.nexus.presence;

import com.nexus.chat.ConversationMemberRepository;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;
import java.util.Set;

/**
 * Tracks WebSocket presence and notifies the people who share a conversation with a user
 * when that user comes online or goes offline. Presence transitions are driven by Spring's
 * STOMP session lifecycle events; only edge transitions (first connect / last disconnect)
 * are broadcast, so opening a second tab doesn't spam contacts.
 */
@Service
public class PresenceService {

    private static final String PRESENCE_QUEUE = "/queue/presence";

    private final PresenceRegistry registry;
    private final ConversationMemberRepository members;
    private final SimpMessagingTemplate messagingTemplate;

    public PresenceService(PresenceRegistry registry,
                           ConversationMemberRepository members,
                           SimpMessagingTemplate messagingTemplate) {
        this.registry = registry;
        this.members = members;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onConnected(SessionConnectedEvent event) {
        Principal user = event.getUser();
        if (user != null && registry.connected(user.getName())) {
            broadcast(user.getName(), true);
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        Principal user = event.getUser();
        if (user != null && registry.disconnected(user.getName())) {
            broadcast(user.getName(), false);
        }
    }

    /** The user's contacts (people they share a conversation with) who are currently online. */
    public List<String> onlineContactsOf(String username) {
        Set<String> online = registry.onlineUsers();
        return members.findContactUsernames(username).stream()
                .filter(online::contains)
                .toList();
    }

    private void broadcast(String username, boolean online) {
        PresenceEvent event = new PresenceEvent(username, online);
        for (String contact : members.findContactUsernames(username)) {
            messagingTemplate.convertAndSendToUser(contact, PRESENCE_QUEUE, event);
        }
    }
}
