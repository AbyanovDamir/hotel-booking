package com.example.hotel.entity;

import java.math.BigDecimal;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "rooms", indexes = {
        @Index(name = "idx_room_hotel", columnList = "hotel_id"),
        @Index(name = "idx_room_available", columnList = "available")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Room {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hotel_id", nullable = false)
    private Hotel hotel;
    @Column(nullable = false, length = 20)
    private String number;
    @Column(nullable = false, length = 40)
    private String type;
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;
    @Column(nullable = false)
    private boolean available;
    @Column(name = "times_booked", nullable = false)
    private long timesBooked;
    @Version
    private long version;
}
