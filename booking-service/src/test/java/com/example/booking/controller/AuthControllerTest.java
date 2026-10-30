package com.example.booking.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.booking.config.SecurityConfig;
import com.example.booking.dto.AuthResponse;
import com.example.booking.exception.GlobalExceptionHandler;
import com.example.booking.service.AuthService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("Authentication HTTP API")
class AuthControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean AuthService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    @DisplayName("регистрация доступна без JWT и возвращает 201")
    void registersWithoutAuthentication() throws Exception {
        when(service.register(any())).thenReturn(
                new AuthResponse("token", "Bearer", 3600, "alice", "USER"));

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"secret\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("token"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @DisplayName("короткие логин и пароль возвращают 400")
    void validatesRegistrationPayload() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors.username").exists())
                .andExpect(jsonPath("$.validationErrors.password").exists());

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("неверные учётные данные возвращают 401 без утечки деталей")
    void rejectsInvalidCredentials() throws Exception {
        when(service.authenticate(any())).thenThrow(new BadCredentialsException("password mismatch"));

        mvc.perform(post("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"alice\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}
