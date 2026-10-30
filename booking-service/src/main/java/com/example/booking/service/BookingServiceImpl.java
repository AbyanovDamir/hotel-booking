package com.example.booking.service;

import java.time.Instant;
import java.util.UUID;
import com.example.booking.client.HotelClientFacade;
import com.example.booking.dto.*;
import com.example.booking.entity.*;
import com.example.booking.exception.*;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingServiceImpl implements BookingService {
    private final BookingRepository bookings;
    private final UserRepository users;
    private final HotelClientFacade hotelClient;
    private final BookingMapper mapper;

    @Override
    public BookingDto create(String username, String idempotencyKey, CreateBookingRequest request) {
        String requestId = idempotencyKey == null || idempotencyKey.isBlank()
                ? UUID.randomUUID().toString() : idempotencyKey;
        var existing = bookings.findByRequestId(requestId);
        if (existing.isPresent()) {
            if (!existing.get().getUser().getUsername().equals(username)) {
                throw new ConflictException("Idempotency-Key belongs to another user");
            }
            log.info("Duplicate booking request {}, returning existing result", requestId);
            return mapper.toDto(existing.get());
        }

        UserAccount user = users.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found: " + username));
        Long roomId = request.roomId();
        if (request.autoSelect()) {
            roomId = hotelClient.recommend(request.startDate(), request.endDate(), request.hotelId()).stream()
                    .findFirst().map(RoomDto::id)
                    .orElseThrow(() -> new ConflictException("No rooms available for the selected dates"));
        }

        Instant now = Instant.now();
        Booking booking = bookings.save(Booking.builder().user(user).roomId(roomId)
                .startDate(request.startDate()).endDate(request.endDate()).status(BookingStatus.PENDING)
                .requestId(requestId).createdAt(now).updatedAt(now).build());
        MDC.put("bookingId", String.valueOf(booking.getId()));
        try {
            log.info("PENDING created");
            hotelClient.confirm(roomId, new AvailabilityRequest(request.startDate(), request.endDate(), requestId));
            transition(booking, BookingStatus.CONFIRMED);
            log.info("PENDING -> CONFIRMED");
            return mapper.toDto(booking);
        } catch (Exception failure) {
            compensate(roomId, requestId);
            transition(booking, BookingStatus.CANCELLED);
            log.warn("PENDING -> CANCELLED after confirm failure: {}", failure.toString());
            if (failure instanceof HotelConflictException hotelConflict) throw hotelConflict;
            throw new ConflictException("Booking failed; compensation completed", failure);
        } finally {
            MDC.remove("bookingId");
        }
    }

    @Override @Transactional(readOnly = true)
    public BookingDto get(String username, Long id) {
        return bookings.findByIdAndUserUsername(id, username).map(mapper::toDto)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
    }

    @Override @Transactional(readOnly = true)
    public Page<BookingDto> history(String username, Pageable pageable) {
        return bookings.findByUserUsername(username, pageable).map(mapper::toDto);
    }

    @Override
    public void cancel(String username, Long id) {
        Booking booking = bookings.findByIdAndUserUsername(id, username)
                .orElseThrow(() -> new NotFoundException("Booking not found: " + id));
        if (booking.getStatus() == BookingStatus.CANCELLED) return;
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            compensate(booking.getRoomId(), booking.getRequestId());
        }
        transition(booking, BookingStatus.CANCELLED);
        log.info("Booking {} -> CANCELLED by user", id);
    }

    private void transition(Booking booking, BookingStatus target) {
        booking.setStatus(target);
        booking.setUpdatedAt(Instant.now());
        bookings.save(booking);
    }

    private void compensate(Long roomId, String requestId) {
        try {
            hotelClient.release(roomId, requestId);
        } catch (Exception compensationFailure) {
            log.error("Compensation failed for request {}: {}", requestId, compensationFailure.toString());
        }
    }
}
