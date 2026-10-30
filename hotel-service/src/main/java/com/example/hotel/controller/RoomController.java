package com.example.hotel.controller;

import java.time.LocalDate;
import java.util.List;
import com.example.hotel.dto.RoomDto;
import com.example.hotel.service.HotelCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final HotelCatalogService service;

    @GetMapping
    public List<RoomDto> all(@RequestParam(required = false) Long hotelId,
                             @RequestParam(required = false) Boolean available,
                             @RequestParam(required = false) Long minTimesBooked) {
        return service.rooms(hotelId, available, minTimesBooked);
    }

    @GetMapping("/{id}")
    public RoomDto one(@PathVariable Long id) { return service.room(id); }

    @GetMapping("/recommend")
    public List<RoomDto> recommend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long hotelId) {
        return service.recommend(startDate, endDate, hotelId);
    }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public RoomDto create(@Valid @RequestBody RoomDto dto) { return service.createRoom(dto); }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public RoomDto update(@PathVariable Long id, @Valid @RequestBody RoomDto dto) {
        return service.updateRoom(id, dto);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.deleteRoom(id); }
}
