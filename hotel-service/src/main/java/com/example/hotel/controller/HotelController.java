package com.example.hotel.controller;

import java.util.List;
import com.example.hotel.dto.*;
import com.example.hotel.service.HotelCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/hotels")
@RequiredArgsConstructor
public class HotelController {
    private final HotelCatalogService service;

    @GetMapping
    public List<HotelDto> all() { return service.hotels(); }

    @GetMapping("/{id}")
    public HotelDto one(@PathVariable Long id) { return service.hotel(id); }

    @GetMapping("/{id}/stats")
    public HotelStatsDto stats(@PathVariable Long id) { return service.stats(id); }

    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasRole('ADMIN')")
    public HotelDto create(@Valid @RequestBody HotelDto dto) { return service.createHotel(dto); }

    @PutMapping("/{id}") @PreAuthorize("hasRole('ADMIN')")
    public HotelDto update(@PathVariable Long id, @Valid @RequestBody HotelDto dto) {
        return service.updateHotel(id, dto);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) @PreAuthorize("hasRole('ADMIN')")
    public void delete(@PathVariable Long id) { service.deleteHotel(id); }
}
