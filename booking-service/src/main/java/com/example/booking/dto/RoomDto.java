package com.example.booking.dto;

import java.math.BigDecimal;

public record RoomDto(Long id, Long hotelId, String hotelName, String number, String type,
                      BigDecimal price, boolean available, long timesBooked) {
}
