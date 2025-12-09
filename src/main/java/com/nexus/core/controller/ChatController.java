package com.nexus.core.controller;

import com.nexus.core.model.Message;
import com.nexus.core.service.ChatService;
import com.nexus.core.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final UserService userService;

    public ChatController(ChatService chatService, UserService userService) {
        this.chatService = chatService;
        this.userService = userService;
    }

    private String getCurrentTime() {
        return DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.now());
    }

    // Direct Messaging

    @PostMapping("/send")
    public ResponseEntity<String> sendMessage(@RequestBody Message msg) throws IOException {
        if (userService.findUser(msg.getSender()).isEmpty())
            return ResponseEntity.badRequest().body("Sender not found");
        if (userService.findUser(msg.getRecipient()).isEmpty())
            return ResponseEntity.badRequest().body("Recipient not found");
        if (userService.isLoggedIn(msg.getSender())){
            msg.setTimestamp(getCurrentTime());
            msg.setGroupMessage(false);
            chatService.saveMessage(msg);
            return ResponseEntity.ok("Message sent");
        }
        else return ResponseEntity.badRequest().body("Sender is not logged in");
    }

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(@RequestParam String requester, @RequestParam String otherUser, @RequestParam String password) throws IOException {
        // Authorize Requester
        if (!userService.checkCredentials(requester, password))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Password. Access Denied.");

        // Fetch History
        if(userService.isLoggedIn(requester)){
            List<Message> history = chatService.getHistory(requester, otherUser, false);
            return ResponseEntity.ok(history);
        }
        else return ResponseEntity.badRequest().body("Requester is not logged in");
    }

    // Group Messaging

    @PostMapping("/group/create")
    public ResponseEntity<String> createGroup(@RequestParam String groupName, @RequestParam String creator) throws IOException {
        if (userService.findUser(creator).isEmpty()) return ResponseEntity.badRequest().body("Creator not found");

        if(userService.isLoggedIn(creator)){
            boolean created = chatService.createGroup(groupName, creator);
            if (created) return ResponseEntity.ok("Group " + groupName + " created.");
            else return ResponseEntity.badRequest().body("Group already exists.");
        }
        else return ResponseEntity.badRequest().body("Creator is not logged in");
    }

    @PostMapping("/group/join")
    public ResponseEntity<String> joinGroup(@RequestParam String groupName, @RequestParam String username) throws IOException {
        if (userService.findUser(username).isEmpty()) return ResponseEntity.badRequest().body("User not found");

        if(userService.isLoggedIn(username)){
            boolean added = chatService.addMemberToGroup(groupName, username);
            if (added) return ResponseEntity.ok("Joined group " + groupName);
            else return ResponseEntity.badRequest().body("Failed to join. Group might not exist or user already in.");
        }
        else return ResponseEntity.badRequest().body("User is not logged in");
    }

    @PostMapping("/group/send")
    public ResponseEntity<String> sendGroupMessage(@RequestBody Message msg) throws IOException {
        if (!chatService.isGroupMember(msg.getRecipient(), msg.getSender())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not a member of this group");
        }
        if(userService.isLoggedIn(msg.getSender())){
            msg.setTimestamp(getCurrentTime());
            msg.setGroupMessage(true);
            chatService.saveMessage(msg);
            return ResponseEntity.ok("Group message sent");
        }
        else return ResponseEntity.badRequest().body("Sender is not logged in");
    }

    @GetMapping("/group/history")
    public ResponseEntity<?> getGroupHistory(@RequestParam String groupName, @RequestParam String requester, @RequestParam String password) throws IOException {
        if (!userService.checkCredentials(requester, password))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Password.");
        if (!chatService.isGroupMember(groupName, requester))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not a member of this group.");

        List<Message> history = chatService.getHistory(requester, groupName, true);
        return ResponseEntity.ok(history);
    }

    // Deletions

    @DeleteMapping("/clear-chat")
    public ResponseEntity<String> clearChat(@RequestParam String requester, @RequestParam String otherUser, @RequestParam String password, @RequestParam(defaultValue = "false") boolean isGroup) throws IOException {
        if (!userService.checkCredentials(requester, password))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Password.");

        if(userService.isLoggedIn(requester)){
            // if clearing group chat then check whether user is part of group
            if (isGroup && !chatService.isGroupMember(otherUser, requester))
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Not a member of this group.");

            boolean done = chatService.clearChat(requester, otherUser, isGroup);
            if (done) return ResponseEntity.ok("Chat cleared for " + requester);
            else return ResponseEntity.badRequest().body("Chat not found");
        }
        else return ResponseEntity.badRequest().body("Requester is not logged in");
    }

    @DeleteMapping("/delete-message")
    public ResponseEntity<String> deleteMessageForEveryone(@RequestParam String sender, @RequestParam String recipient, @RequestParam String timestamp, @RequestParam String password, @RequestParam(defaultValue = "false") boolean isGroup) throws IOException {
        if (!userService.checkCredentials(sender, password))
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid Password.");

        if(userService.isLoggedIn(sender)){
            boolean deleted = chatService.deleteMessageForEveryone(sender, recipient, timestamp, isGroup);
            if (deleted) return ResponseEntity.ok("Message deleted for everyone");
            else return ResponseEntity.badRequest().body("Message not found or you are not the sender");
        }
        else return ResponseEntity.badRequest().body("Sender is not logged in");
    }
}
