package com.example.booking.dto;

import java.time.*;
import com.example.booking.entity.BookingStatus;

public record BookingDto(Long id, Long roomId, LocalDate startDate, LocalDate endDate,
                         BookingStatus status, String requestId, Instant createdAt, Instant updatedAt) {
}
