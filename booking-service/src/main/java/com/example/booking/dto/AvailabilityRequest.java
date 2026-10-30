package com.example.booking.dto;

import java.time.LocalDate;

public record AvailabilityRequest(LocalDate startDate, LocalDate endDate, String requestId) {
}
