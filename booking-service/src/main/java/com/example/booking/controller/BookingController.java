package com.example.booking.controller;

import com.example.booking.dto.*;
import com.example.booking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class BookingController {
    private final BookingService service;

    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public BookingDto create(Authentication authentication,
                             @RequestHeader(value = "Idempotency-Key", required = false) String key,
                             @Valid @RequestBody CreateBookingRequest request) {
        return service.create(authentication.getName(), key, request);
    }

    @GetMapping
    public Page<BookingDto> history(Authentication authentication,
                                    @PageableDefault(size = 20, sort = "createdAt",
                                            direction = Sort.Direction.DESC) Pageable pageable) {
        return service.history(authentication.getName(), pageable);
    }

    @GetMapping("/{id}")
    public BookingDto get(Authentication authentication, @PathVariable Long id) {
        return service.get(authentication.getName(), id);
    }

    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(Authentication authentication, @PathVariable Long id) {
        service.cancel(authentication.getName(), id);
    }
}
