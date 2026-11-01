package com.example.booking.repository;

import java.util.Optional;
import com.example.booking.entity.Booking;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookingRepository extends JpaRepository<Booking, Long> {
    @EntityGraph(attributePaths = "user")
    Optional<Booking> findByRequestId(String requestId);
    Optional<Booking> findByIdAndUserUsername(Long id, String username);
    Page<Booking> findByUserUsername(String username, Pageable pageable);
}
