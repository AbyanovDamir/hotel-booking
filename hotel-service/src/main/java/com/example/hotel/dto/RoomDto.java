package com.example.hotel.dto;

import java.math.BigDecimal;
import jakarta.validation.constraints.*;

public record RoomDto(Long id, @NotNull Long hotelId, String hotelName,
                      @NotBlank String number, @NotBlank String type,
                      @NotNull @DecimalMin("0.0") BigDecimal price,
                      boolean available, long timesBooked) {
}
