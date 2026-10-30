package com.example.booking.entity;

import java.time.*;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_user", columnList = "user_id"),
        @Index(name = "idx_booking_room_dates", columnList = "room_id,start_date,end_date")
}, uniqueConstraints = @UniqueConstraint(name = "uk_booking_request", columnNames = "request_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;
    @Column(name = "room_id", nullable = false)
    private Long roomId;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20)
    private BookingStatus status;
    @Column(name = "request_id", nullable = false, updatable = false, length = 100)
    private String requestId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
