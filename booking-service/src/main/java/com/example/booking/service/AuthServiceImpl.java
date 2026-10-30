package com.example.booking.service;

import com.example.booking.dto.*;
import com.example.booking.entity.*;
import com.example.booking.exception.ConflictException;
import com.example.booking.repository.UserRepository;
import com.example.booking.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override @Transactional
    public AuthResponse register(AuthRequest request) {
        if (users.existsByUsername(request.username())) throw new ConflictException("Username already exists");
        UserAccount user = users.save(UserAccount.builder().username(request.username())
                .password(passwordEncoder.encode(request.password())).role(Role.USER).build());
        return response(user);
    }

    @Override
    public AuthResponse authenticate(AuthRequest request) {
        UserAccount user = users.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }
        return response(user);
    }

    private AuthResponse response(UserAccount user) {
        return new AuthResponse(jwtService.issue(user), "Bearer", jwtService.expiresInSeconds(),
                user.getUsername(), user.getRole().name());
    }
}
