package com.nexus.presence;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class PresenceController {

    private final PresenceService presenceService;

    public PresenceController(PresenceService presenceService) {
        this.presenceService = presenceService;
    }

    /** Snapshot of which of the caller's contacts are online right now (for initial render). */
    @GetMapping("/api/presence")
    public List<String> onlineContacts(@AuthenticationPrincipal UserDetails principal) {
        return presenceService.onlineContactsOf(principal.getUsername());
    }
}
