package com.example.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReleaseRequest(@NotBlank @Size(max = 100) String requestId) {
}
