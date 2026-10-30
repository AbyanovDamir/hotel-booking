package com.example.booking.service;

import com.example.booking.dto.*;
import org.springframework.data.domain.*;

public interface BookingService {
    BookingDto create(String username, String idempotencyKey, CreateBookingRequest request);
    BookingDto get(String username, Long id);
    Page<BookingDto> history(String username, Pageable pageable);
    void cancel(String username, Long id);
}
