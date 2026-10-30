package com.example.hotel.service;

import com.example.hotel.dto.AvailabilityRequest;

public interface RoomAvailabilityService {
    void confirm(Long roomId, AvailabilityRequest request);
    void release(Long roomId, String requestId);
}
