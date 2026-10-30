package com.example.hotel.entity;

import java.time.LocalDate;
import java.time.Instant;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "room_reservations", indexes = {
        @Index(name = "idx_reservation_room_dates", columnList = "room_id,start_date,end_date")
}, uniqueConstraints = @UniqueConstraint(name = "uk_reservation_request", columnNames = "request_id"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomReservation {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;
    @Column(name = "request_id", nullable = false, updatable = false, length = 100)
    private String requestId;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
