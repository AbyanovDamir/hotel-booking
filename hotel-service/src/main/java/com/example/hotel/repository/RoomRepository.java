package com.example.hotel.repository;

import java.util.List;
import java.util.Optional;
import com.example.hotel.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r join fetch r.hotel where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Query("select r from Room r join fetch r.hotel where (:hotelId is null or r.hotel.id = :hotelId) "
            + "and (:available is null or r.available = :available) "
            + "and (:minTimes is null or r.timesBooked >= :minTimes) order by r.timesBooked asc, r.id asc")
    List<Room> findFiltered(@Param("hotelId") Long hotelId,
                            @Param("available") Boolean available,
                            @Param("minTimes") Long minTimes);

    List<Room> findByHotelIdOrderById(Long hotelId);
}
