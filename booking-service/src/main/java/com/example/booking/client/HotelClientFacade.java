package com.example.booking.client;

import java.time.LocalDate;
import java.util.List;
import com.example.booking.dto.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HotelClientFacade {
    private final HotelClient hotelClient;

    @Retry(name = "hotelClient")
    @CircuitBreaker(name = "hotelClient")
    public List<RoomDto> recommend(LocalDate start, LocalDate end, Long hotelId) {
        return hotelClient.recommend(start, end, hotelId);
    }

    @Retry(name = "hotelClient")
    @CircuitBreaker(name = "hotelClient")
    public void confirm(Long roomId, AvailabilityRequest request) {
        hotelClient.confirm(roomId, request);
    }

    @Retry(name = "hotelClient")
    @CircuitBreaker(name = "hotelClient")
    public void release(Long roomId, String requestId) {
        hotelClient.release(roomId, new ReleaseRequest(requestId));
    }
}
