package com.example.booking.dto;

import jakarta.validation.constraints.*;

public record AuthRequest(@NotBlank @Size(min = 3, max = 80) String username,
                          @NotBlank @Size(min = 4, max = 100) String password) {
}
