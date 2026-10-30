package com.example.hotel.service;

import java.time.LocalDate;
import java.util.List;
import com.example.hotel.dto.*;
import com.example.hotel.entity.*;
import com.example.hotel.exception.*;
import com.example.hotel.mapper.*;
import com.example.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HotelCatalogServiceImpl implements HotelCatalogService {
    private final HotelRepository hotelRepository;
    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;
    private final HotelMapper hotelMapper;
    private final RoomMapper roomMapper;

    @Override
    public List<HotelDto> hotels() {
        return hotelRepository.findAll().stream().map(hotelMapper::toDto).toList();
    }

    @Override
    public HotelDto hotel(Long id) {
        return hotelMapper.toDto(requireHotel(id));
    }

    @Override @Transactional
    public HotelDto createHotel(HotelDto dto) {
        return hotelMapper.toDto(hotelRepository.save(hotelMapper.toEntity(dto)));
    }

    @Override @Transactional
    public HotelDto updateHotel(Long id, HotelDto dto) {
        Hotel hotel = requireHotel(id);
        hotelMapper.update(dto, hotel);
        return hotelMapper.toDto(hotel);
    }

    @Override @Transactional
    public void deleteHotel(Long id) {
        hotelRepository.delete(requireHotel(id));
    }

    @Override
    public List<RoomDto> rooms(Long hotelId, Boolean available, Long minTimesBooked) {
        return roomRepository.findFiltered(hotelId, available, minTimesBooked).stream()
                .map(roomMapper::toDto).toList();
    }

    @Override
    public RoomDto room(Long id) {
        return roomMapper.toDto(requireRoom(id));
    }

    @Override @Transactional
    public RoomDto createRoom(RoomDto dto) {
        Room room = Room.builder().hotel(requireHotel(dto.hotelId())).number(dto.number())
                .type(dto.type()).price(dto.price()).available(dto.available())
                .timesBooked(Math.max(0, dto.timesBooked())).build();
        return roomMapper.toDto(roomRepository.save(room));
    }

    @Override @Transactional
    public RoomDto updateRoom(Long id, RoomDto dto) {
        Room room = requireRoom(id);
        room.setHotel(requireHotel(dto.hotelId()));
        room.setNumber(dto.number());
        room.setType(dto.type());
        room.setPrice(dto.price());
        room.setAvailable(dto.available());
        return roomMapper.toDto(room);
    }

    @Override @Transactional
    public void deleteRoom(Long id) {
        roomRepository.delete(requireRoom(id));
    }

    @Override
    public List<RoomDto> recommend(LocalDate startDate, LocalDate endDate, Long hotelId) {
        validateDates(startDate, endDate);
        return roomRepository.findFiltered(hotelId, true, null).stream()
                .filter(room -> !reservationRepository.existsOverlap(room.getId(), startDate, endDate))
                .map(roomMapper::toDto).toList();
    }

    @Override
    public HotelStatsDto stats(Long hotelId) {
        requireHotel(hotelId);
        List<RoomDto> rooms = roomRepository.findByHotelIdOrderById(hotelId).stream()
                .map(roomMapper::toDto).toList();
        long reservations = reservationRepository.countByRoomHotelId(hotelId);
        long lifetime = rooms.stream().mapToLong(RoomDto::timesBooked).sum();
        double occupancy = rooms.isEmpty() ? 0.0 : Math.min(100.0, reservations * 100.0 / rooms.size());
        return new HotelStatsDto(hotelId, rooms.size(), reservations, occupancy, lifetime, rooms);
    }

    private Hotel requireHotel(Long id) {
        return hotelRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hotel not found: " + id));
    }

    private Room requireRoom(Long id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Room not found: " + id));
    }

    static void validateDates(LocalDate start, LocalDate end) {
        if (start == null || end == null || !start.isBefore(end)) {
            throw new ConflictException("startDate must be before endDate");
        }
    }
}
