package com.nexus.chat;

import com.nexus.chat.dto.AddMemberRequest;
import com.nexus.chat.dto.ChangeRoleRequest;
import com.nexus.chat.dto.CreateGroupRequest;
import com.nexus.chat.dto.GroupResponse;
import com.nexus.chat.dto.MessageResponse;
import com.nexus.chat.dto.SendGroupMessageRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<GroupResponse> create(
            @AuthenticationPrincipal UserDetails principal,
            @Valid @RequestBody CreateGroupRequest request) {
        GroupResponse created = groupService.createGroup(principal.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/{conversationId}/members")
    public ResponseEntity<Void> addMember(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long conversationId,
            @Valid @RequestBody AddMemberRequest request) {
        groupService.addMember(principal.getUsername(), conversationId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{conversationId}/members/{username}")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long conversationId,
            @PathVariable String username) {
        groupService.removeMember(principal.getUsername(), conversationId, username);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{conversationId}/members/{username}/role")
    public ResponseEntity<Void> changeRole(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long conversationId,
            @PathVariable String username,
            @Valid @RequestBody ChangeRoleRequest request) {
        groupService.changeRole(principal.getUsername(), conversationId, username, request.role());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/leave")
    public ResponseEntity<Void> leave(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long conversationId) {
        groupService.leaveGroup(principal.getUsername(), conversationId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{conversationId}/messages")
    public ResponseEntity<MessageResponse> postMessage(
            @AuthenticationPrincipal UserDetails principal,
            @PathVariable Long conversationId,
            @Valid @RequestBody SendGroupMessageRequest request) {
        MessageResponse sent = groupService.postGroupMessage(principal.getUsername(), conversationId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sent);
    }
}
