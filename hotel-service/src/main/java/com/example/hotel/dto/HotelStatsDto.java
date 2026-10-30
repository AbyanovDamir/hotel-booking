package com.example.hotel.dto;

import java.util.List;

public record HotelStatsDto(Long hotelId, long roomCount, long activeReservations,
                            double occupancyPercent, long totalTimesBooked,
                            List<RoomDto> rooms) {
}
