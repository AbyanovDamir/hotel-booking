package com.example.hotel.repository;

import java.time.LocalDate;
import java.util.Optional;
import com.example.hotel.entity.RoomReservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RoomReservationRepository extends JpaRepository<RoomReservation, Long> {
    Optional<RoomReservation> findByRequestId(String requestId);

    @Query("select count(rr) > 0 from RoomReservation rr where rr.room.id = :roomId "
            + "and rr.startDate < :endDate and rr.endDate > :startDate")
    boolean existsOverlap(@Param("roomId") Long roomId,
                          @Param("startDate") LocalDate startDate,
                          @Param("endDate") LocalDate endDate);

    long countByRoomHotelId(Long hotelId);
}
