package com.example.booking.mapper;

import com.example.booking.dto.BookingDto;
import com.example.booking.entity.Booking;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BookingMapper {
    BookingDto toDto(Booking booking);
}
