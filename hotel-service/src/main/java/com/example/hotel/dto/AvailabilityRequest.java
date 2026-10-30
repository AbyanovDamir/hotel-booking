package com.example.hotel.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record AvailabilityRequest(@NotNull LocalDate startDate,
                                  @NotNull LocalDate endDate,
                                  @NotBlank @Size(max = 100) String requestId) {
}
