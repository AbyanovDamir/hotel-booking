package com.example.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HotelDto(Long id,
                       @NotBlank @Size(max = 120) String name,
                       @NotBlank @Size(max = 120) String city,
                       @NotBlank @Size(max = 300) String address) {
}
