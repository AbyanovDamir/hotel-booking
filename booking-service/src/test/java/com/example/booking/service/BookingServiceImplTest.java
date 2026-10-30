package com.example.booking.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.example.booking.client.HotelClientFacade;
import com.example.booking.dto.*;
import com.example.booking.entity.*;
import com.example.booking.exception.*;
import com.example.booking.mapper.BookingMapper;
import com.example.booking.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Booking saga")
class BookingServiceImplTest {
    @Mock BookingRepository bookings;
    @Mock UserRepository users;
    @Mock HotelClientFacade hotelClient;
    @Mock BookingMapper mapper;
    @InjectMocks BookingServiceImpl service;

    private UserAccount user;
    private CreateBookingRequest request;

    @BeforeEach
    void setUp() {
        user = UserAccount.builder().id(10L).username("user").role(Role.USER).build();
        request = new CreateBookingRequest(7L, false, null,
                LocalDate.now().plusDays(2), LocalDate.now().plusDays(4));
        lenient().when(bookings.findByRequestId("request-1")).thenReturn(Optional.empty());
        lenient().when(users.findByUsername("user")).thenReturn(Optional.of(user));
        lenient().when(bookings.save(any(Booking.class))).thenAnswer(invocation -> {
            Booking booking = invocation.getArgument(0);
            if (booking.getId() == null) booking.setId(42L);
            return booking;
        });
        lenient().when(mapper.toDto(any(Booking.class))).thenAnswer(invocation -> {
            Booking b = invocation.getArgument(0);
            return new BookingDto(b.getId(), b.getRoomId(), b.getStartDate(), b.getEndDate(),
                    b.getStatus(), b.getRequestId(), b.getCreatedAt(), b.getUpdatedAt());
        });
    }

    @Test
    @DisplayName("confirm переводит PENDING в CONFIRMED")
    void confirmsBooking() {
        BookingDto result = service.create("user", "request-1", request);

        assertThat(result.status()).isEqualTo(BookingStatus.CONFIRMED);
        verify(hotelClient).confirm(eq(7L), any(AvailabilityRequest.class));
        verify(hotelClient, never()).release(anyLong(), anyString());
    }

    @Test
    @DisplayName("ошибка Hotel Service вызывает release и CANCELLED")
    void compensatesWhenConfirmationFails() {
        doThrow(new HotelConflictException("busy")).when(hotelClient)
                .confirm(eq(7L), any(AvailabilityRequest.class));

        assertThatThrownBy(() -> service.create("user", "request-1", request))
                .isInstanceOf(HotelConflictException.class);

        verify(hotelClient).release(7L, "request-1");
        ArgumentCaptor<Booking> captor = ArgumentCaptor.forClass(Booking.class);
        verify(bookings, atLeast(2)).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    @DisplayName("повторный Idempotency-Key возвращает существующую бронь")
    void duplicateRequestReturnsExistingBooking() {
        Booking existing = Booking.builder().id(99L).user(user).roomId(7L)
                .startDate(request.startDate()).endDate(request.endDate())
                .status(BookingStatus.CONFIRMED).requestId("request-1")
                .createdAt(Instant.now()).updatedAt(Instant.now()).build();
        when(bookings.findByRequestId("request-1")).thenReturn(Optional.of(existing));

        BookingDto result = service.create("user", "request-1", request);

        assertThat(result.id()).isEqualTo(99L);
        verifyNoInteractions(hotelClient);
    }

    @Test
    @DisplayName("повторный Idempotency-Key другого пользователя отклоняется")
    void rejectsIdempotencyKeyOwnedByAnotherUser() {
        UserAccount owner = UserAccount.builder().id(11L).username("other").role(Role.USER).build();
        Booking existing = Booking.builder().id(99L).user(owner).roomId(7L)
                .startDate(request.startDate()).endDate(request.endDate())
                .status(BookingStatus.CONFIRMED).requestId("request-1").build();
        when(bookings.findByRequestId("request-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.create("user", "request-1", request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("another user");

        verifyNoInteractions(hotelClient);
    }

    @Test
    @DisplayName("автоподбор использует первый рекомендованный номер")
    void autoSelectUsesFirstRecommendation() {
        CreateBookingRequest autoRequest = new CreateBookingRequest(null, true, 3L,
                request.startDate(), request.endDate());
        RoomDto recommendation = new RoomDto(25L, 3L, "Hotel", "301", "DOUBLE",
                BigDecimal.valueOf(150), true, 0);
        when(hotelClient.recommend(autoRequest.startDate(), autoRequest.endDate(), 3L))
                .thenReturn(List.of(recommendation));

        BookingDto result = service.create("user", "request-1", autoRequest);

        assertThat(result.roomId()).isEqualTo(25L);
        verify(hotelClient).confirm(eq(25L), any(AvailabilityRequest.class));
    }

    @Test
    @DisplayName("автоподбор без свободных номеров не создаёт бронь")
    void autoSelectFailsWhenNoRoomsAreAvailable() {
        CreateBookingRequest autoRequest = new CreateBookingRequest(null, true, null,
                request.startDate(), request.endDate());
        when(hotelClient.recommend(autoRequest.startDate(), autoRequest.endDate(), null))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.create("user", "request-1", autoRequest))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("No rooms available");

        verify(bookings, never()).save(any());
        verify(hotelClient, never()).confirm(anyLong(), any());
    }

    @Test
    @DisplayName("отмена подтверждённой брони освобождает номер")
    void cancelConfirmedBookingReleasesRoom() {
        Booking confirmed = Booking.builder().id(42L).user(user).roomId(7L)
                .startDate(request.startDate()).endDate(request.endDate())
                .status(BookingStatus.CONFIRMED).requestId("request-1").build();
        when(bookings.findByIdAndUserUsername(42L, "user")).thenReturn(Optional.of(confirmed));

        service.cancel("user", 42L);

        verify(hotelClient).release(7L, "request-1");
        assertThat(confirmed.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        verify(bookings).save(confirmed);
    }

    @Test
    @DisplayName("повторная отмена идемпотентна")
    void repeatedCancellationDoesNothing() {
        Booking cancelled = Booking.builder().id(42L).user(user).roomId(7L)
                .status(BookingStatus.CANCELLED).requestId("request-1").build();
        when(bookings.findByIdAndUserUsername(42L, "user")).thenReturn(Optional.of(cancelled));

        service.cancel("user", 42L);

        verifyNoInteractions(hotelClient);
        verify(bookings, never()).save(any());
    }

    @Test
    @DisplayName("чужая или отсутствующая бронь недоступна")
    void getMissingBookingThrowsNotFound() {
        when(bookings.findByIdAndUserUsername(404L, "user")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get("user", 404L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("404");
    }
}
