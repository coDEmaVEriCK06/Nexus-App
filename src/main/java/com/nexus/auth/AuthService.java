package com.nexus.auth;

import com.nexus.auth.dto.RegisterRequest;
import com.nexus.auth.dto.UserResponse;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse register(RegisterRequest request) {
        if (users.existsByUsername(request.username())) {
            throw new UsernameAlreadyTakenException(request.username());
        }
        String hash = passwordEncoder.encode(request.password());
        User saved = users.save(new User(request.username(), hash));
        return new UserResponse(saved.getId(), saved.getUsername(), saved.getCreatedAt());
    }
}
