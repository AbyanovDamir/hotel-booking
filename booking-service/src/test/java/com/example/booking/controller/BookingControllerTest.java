package com.example.booking.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.*;
import com.example.booking.config.SecurityConfig;
import com.example.booking.dto.BookingDto;
import com.example.booking.entity.BookingStatus;
import com.example.booking.exception.GlobalExceptionHandler;
import com.example.booking.service.BookingService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@WebMvcTest(BookingController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class})
@DisplayName("Booking HTTP API")
class BookingControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean BookingService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    @DisplayName("без JWT возвращается 401")
    void rejectsAnonymous() throws Exception {
        mvc.perform(post("/api/bookings").contentType(MediaType.APPLICATION_JSON)
                .content(validJson())).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("неподходящая роль возвращает 403")
    void rejectsWrongRole() throws Exception {
        mvc.perform(post("/api/bookings").with(jwt().jwt(jwt -> jwt.subject("guest"))
                        .authorities(() -> "ROLE_GUEST"))
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("USER создаёт бронь и получает 201")
    void createsBooking() throws Exception {
        LocalDate start = LocalDate.now().plusDays(2);
        when(service.create(eq("user"), eq("http-key"), any())).thenReturn(
                new BookingDto(1L, 1L, start, start.plusDays(2), BookingStatus.CONFIRMED,
                        "http-key", Instant.now(), Instant.now()));

        mvc.perform(post("/api/bookings").with(jwt().jwt(jwt -> jwt.subject("user"))
                        .authorities(() -> "ROLE_USER"))
                .header("Idempotency-Key", "http-key")
                .contentType(MediaType.APPLICATION_JSON).content(validJson()))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("неверный диапазон дат возвращает 400 и описание ошибки")
    void rejectsInvalidDateRange() throws Exception {
        LocalDate date = LocalDate.now().plusDays(2);
        String json = "{\"roomId\":1,\"autoSelect\":false,\"startDate\":\"" + date
                + "\",\"endDate\":\"" + date + "\"}";

        mvc.perform(post("/api/bookings").with(jwt().jwt(jwt -> jwt.subject("user"))
                        .authorities(() -> "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));

        verifyNoInteractions(service);
    }

    @Test
    @DisplayName("без roomId при отключённом автоподборе возвращается 400")
    void requiresRoomForManualSelection() throws Exception {
        LocalDate start = LocalDate.now().plusDays(2);
        String json = "{\"autoSelect\":false,\"startDate\":\"" + start
                + "\",\"endDate\":\"" + start.plusDays(2) + "\"}";

        mvc.perform(post("/api/bookings").with(jwt().jwt(jwt -> jwt.subject("user"))
                        .authorities(() -> "ROLE_USER"))
                .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors").isNotEmpty());

        verifyNoInteractions(service);
    }

    private String validJson() {
        LocalDate start = LocalDate.now().plusDays(2);
        return "{\"roomId\":1,\"autoSelect\":false,\"startDate\":\"" + start
                + "\",\"endDate\":\"" + start.plusDays(2) + "\"}";
    }
}
