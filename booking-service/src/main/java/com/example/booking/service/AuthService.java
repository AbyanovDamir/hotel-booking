package com.example.booking.service;

import com.example.booking.dto.*;

public interface AuthService {
    AuthResponse register(AuthRequest request);
    AuthResponse authenticate(AuthRequest request);
}
