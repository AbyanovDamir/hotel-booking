package com.example.hotel.service;

import java.time.LocalDate;
import java.util.List;
import com.example.hotel.dto.*;

public interface HotelCatalogService {
    List<HotelDto> hotels();
    HotelDto hotel(Long id);
    HotelDto createHotel(HotelDto dto);
    HotelDto updateHotel(Long id, HotelDto dto);
    void deleteHotel(Long id);
    List<RoomDto> rooms(Long hotelId, Boolean available, Long minTimesBooked);
    RoomDto room(Long id);
    RoomDto createRoom(RoomDto dto);
    RoomDto updateRoom(Long id, RoomDto dto);
    void deleteRoom(Long id);
    List<RoomDto> recommend(LocalDate startDate, LocalDate endDate, Long hotelId);
    HotelStatsDto stats(Long hotelId);
}
