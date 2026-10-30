package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody AuthRequest request) {
        return service.register(request);
    }

    @PostMapping("/token")
    public AuthResponse token(@Valid @RequestBody AuthRequest request) {
        return service.authenticate(request);
    }
}
