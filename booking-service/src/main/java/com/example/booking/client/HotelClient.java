package com.example.booking.client;

import java.time.LocalDate;
import java.util.List;
import com.example.booking.dto.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "hotel-service", configuration = HotelClientConfiguration.class)
public interface HotelClient {
    @GetMapping("/api/rooms/recommend")
    List<RoomDto> recommend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) Long hotelId);

    @PostMapping("/internal/rooms/{roomId}/confirm-availability")
    void confirm(@PathVariable Long roomId, @RequestBody AvailabilityRequest request);

    @PostMapping("/internal/rooms/{roomId}/release")
    void release(@PathVariable Long roomId, @RequestBody ReleaseRequest request);
}
