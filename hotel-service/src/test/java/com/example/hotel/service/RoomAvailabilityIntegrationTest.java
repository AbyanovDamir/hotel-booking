package com.example.hotel.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalDate;
import com.example.hotel.dto.AvailabilityRequest;
import com.example.hotel.exception.ConflictException;
import com.example.hotel.repository.RoomRepository;
import com.example.hotel.repository.RoomReservationRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@DisplayName("Room availability transaction")
class RoomAvailabilityIntegrationTest {
    @Autowired RoomAvailabilityService availability;
    @Autowired HotelCatalogService catalog;
    @Autowired RoomReservationRepository reservations;
    @Autowired RoomRepository rooms;

    @Test
    @DisplayName("confirm идемпотентен, пересекающаяся бронь отклоняется")
    void idempotencyAndOverlap() {
        LocalDate start = LocalDate.of(2099, 1, 10);
        AvailabilityRequest first = new AvailabilityRequest(start, start.plusDays(3), "integration-1");

        availability.confirm(1L, first);
        availability.confirm(1L, first);

        assertThat(reservations.findByRequestId("integration-1")).isPresent();
        assertThatThrownBy(() -> availability.confirm(1L,
                new AvailabilityRequest(start.plusDays(1), start.plusDays(4), "integration-2")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    @DisplayName("recommend сортирует по timesBooked, затем id")
    void recommendationOrderIsStable() {
        var result = catalog.recommend(LocalDate.of(2099, 2, 1), LocalDate.of(2099, 2, 3), null);

        assertThat(result).isNotEmpty();
        assertThat(result).isSortedAccordingTo((left, right) -> {
            int byTimes = Long.compare(left.timesBooked(), right.timesBooked());
            return byTimes != 0 ? byTimes : Long.compare(left.id(), right.id());
        });
    }

    @Test
    @DisplayName("соседние интервалы не считаются пересекающимися")
    void adjacentReservationsAreAllowed() {
        LocalDate start = LocalDate.of(2099, 3, 10);

        availability.confirm(2L, new AvailabilityRequest(start, start.plusDays(2), "adjacent-1"));
        availability.confirm(2L,
                new AvailabilityRequest(start.plusDays(2), start.plusDays(4), "adjacent-2"));

        assertThat(reservations.findByRequestId("adjacent-1")).isPresent();
        assertThat(reservations.findByRequestId("adjacent-2")).isPresent();
    }

    @Test
    @DisplayName("повтор requestId с другими данными отклоняется")
    void duplicateRequestWithDifferentDataIsRejected() {
        LocalDate start = LocalDate.of(2099, 4, 10);
        availability.confirm(3L, new AvailabilityRequest(start, start.plusDays(2), "duplicate-1"));

        assertThatThrownBy(() -> availability.confirm(3L,
                new AvailabilityRequest(start, start.plusDays(3), "duplicate-1")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("different data");
    }

    @Test
    @DisplayName("release удаляет резерв и безопасен при повторном вызове")
    void releaseIsIdempotent() {
        LocalDate start = LocalDate.of(2099, 5, 10);
        availability.confirm(4L, new AvailabilityRequest(start, start.plusDays(2), "release-1"));
        assertThat(reservations.findByRequestId("release-1")).isPresent();

        availability.release(4L, "release-1");
        availability.release(4L, "release-1");

        assertThat(reservations.findByRequestId("release-1")).isEmpty();
    }

    @Test
    @DisplayName("release другого номера отклоняется и сохраняет резерв")
    void releaseWithWrongRoomIsRejected() {
        LocalDate start = LocalDate.of(2099, 6, 10);
        availability.confirm(5L, new AvailabilityRequest(start, start.plusDays(2), "release-wrong"));

        assertThatThrownBy(() -> availability.release(6L, "release-wrong"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("another room");
        assertThat(reservations.findByRequestId("release-wrong")).isPresent();
    }

    @Test
    @DisplayName("confirm увеличивает счётчик бронирований только один раз")
    void duplicateConfirmIncrementsCounterOnce() {
        LocalDate start = LocalDate.of(2099, 7, 10);
        long before = rooms.findById(7L).orElseThrow().getTimesBooked();
        AvailabilityRequest request = new AvailabilityRequest(start, start.plusDays(2), "counter-1");

        availability.confirm(7L, request);
        availability.confirm(7L, request);

        assertThat(rooms.findById(7L).orElseThrow().getTimesBooked()).isEqualTo(before + 1);
    }

    @Test
    @DisplayName("неверный диапазон дат отклоняется до обращения к номеру")
    void invalidDatesAreRejected() {
        LocalDate date = LocalDate.of(2099, 8, 10);

        assertThatThrownBy(() -> availability.confirm(1L,
                new AvailabilityRequest(date, date, "invalid-dates")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("before");
    }
}
