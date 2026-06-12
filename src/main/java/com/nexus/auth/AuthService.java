package com.nexus.auth;

import com.nexus.auth.dto.LoginRequest;
import com.nexus.auth.dto.RegisterRequest;
import com.nexus.auth.dto.TokenResponse;
import com.nexus.auth.dto.UserResponse;
import com.nexus.security.JwtService;
import com.nexus.user.User;
import com.nexus.user.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(UserRepository users,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
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

    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        String token = jwtService.generateToken(authentication.getName());
        return new TokenResponse(token);
    }
}
