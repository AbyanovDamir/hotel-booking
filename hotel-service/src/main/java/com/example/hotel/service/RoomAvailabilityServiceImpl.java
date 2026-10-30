package com.example.hotel.service;

import java.time.Instant;
import com.example.hotel.dto.AvailabilityRequest;
import com.example.hotel.entity.*;
import com.example.hotel.exception.*;
import com.example.hotel.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomAvailabilityServiceImpl implements RoomAvailabilityService {
    private final RoomRepository roomRepository;
    private final RoomReservationRepository reservationRepository;

    @Override @Transactional
    public void confirm(Long roomId, AvailabilityRequest request) {
        HotelCatalogServiceImpl.validateDates(request.startDate(), request.endDate());
        MDC.put("requestId", request.requestId());
        try {
            var previous = reservationRepository.findByRequestId(request.requestId());
            if (previous.isPresent()) {
                RoomReservation existing = previous.get();
                if (!existing.getRoom().getId().equals(roomId)
                        || !existing.getStartDate().equals(request.startDate())
                        || !existing.getEndDate().equals(request.endDate())) {
                    throw new ConflictException("requestId was already used with different data");
                }
                log.info("Duplicate confirm skipped");
                return;
            }

            Room room = roomRepository.findByIdForUpdate(roomId)
                    .orElseThrow(() -> new NotFoundException("Room not found: " + roomId));
            if (!room.isAvailable()) {
                throw new ConflictException("Room is disabled");
            }
            if (reservationRepository.existsOverlap(roomId, request.startDate(), request.endDate())) {
                throw new ConflictException("Room is already booked for the selected dates");
            }
            reservationRepository.save(RoomReservation.builder().room(room)
                    .startDate(request.startDate()).endDate(request.endDate())
                    .requestId(request.requestId()).createdAt(Instant.now()).build());
            room.setTimesBooked(room.getTimesBooked() + 1);
            log.info("Room {} confirmed for {}..{}", roomId, request.startDate(), request.endDate());
        } finally {
            MDC.remove("requestId");
        }
    }

    @Override @Transactional
    public void release(Long roomId, String requestId) {
        reservationRepository.findByRequestId(requestId).ifPresentOrElse(reservation -> {
            if (!reservation.getRoom().getId().equals(roomId)) {
                throw new ConflictException("requestId belongs to another room");
            }
            reservationRepository.delete(reservation);
            log.info("Room {} released for request {}", roomId, requestId);
        }, () -> log.info("Idempotent release skipped for request {}", requestId));
    }
}
