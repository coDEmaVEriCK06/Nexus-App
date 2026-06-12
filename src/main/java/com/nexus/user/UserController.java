package com.nexus.user;

import com.nexus.auth.dto.UserResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) {
        this.users = users;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserDetails principal) {
        User user = users.findByUsername(principal.getUsername()).orElseThrow();
        return new UserResponse(user.getId(), user.getUsername(), user.getCreatedAt());
    }
}
