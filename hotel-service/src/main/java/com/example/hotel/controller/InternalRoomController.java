package com.example.hotel.controller;

import com.example.hotel.dto.*;
import com.example.hotel.service.RoomAvailabilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/rooms")
@RequiredArgsConstructor
@PreAuthorize("hasRole('INTERNAL')")
public class InternalRoomController {
    private final RoomAvailabilityService service;

    @PostMapping("/{roomId}/confirm-availability")
    public void confirm(@PathVariable Long roomId, @Valid @RequestBody AvailabilityRequest request) {
        service.confirm(roomId, request);
    }

    @PostMapping("/{roomId}/release")
    public void release(@PathVariable Long roomId, @Valid @RequestBody ReleaseRequest request) {
        service.release(roomId, request.requestId());
    }
}
