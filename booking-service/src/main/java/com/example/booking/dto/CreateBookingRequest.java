package com.example.booking.dto;

import java.time.LocalDate;
import jakarta.validation.constraints.*;

public record CreateBookingRequest(Long roomId, boolean autoSelect, Long hotelId,
                                   @NotNull @FutureOrPresent LocalDate startDate,
                                   @NotNull @Future LocalDate endDate) {
    @AssertTrue(message = "startDate must be before endDate")
    public boolean isDateRangeValid() {
        return startDate == null || endDate == null || startDate.isBefore(endDate);
    }

    @AssertTrue(message = "roomId is required when autoSelect=false")
    public boolean isRoomSelectionValid() {
        return autoSelect || roomId != null;
    }
}
