package com.nexus.chat;

import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendDirectMessageRequest;
import com.nexus.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class MessageController {

    private final ChatService chatService;

    public MessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages/direct")
    public ResponseEntity<MessageResponse> sendDirect(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody SendDirectMessageRequest request) {
        MessageResponse sent = chatService.sendDirectMessage(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sent);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public PageResponse<MessageResponse> getMessages(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return chatService.getConversationMessages(principal.getUsername(), conversationId, page, size);
    }
}
