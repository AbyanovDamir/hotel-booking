package com.example.hotel.mapper;

import com.example.hotel.dto.RoomDto;
import com.example.hotel.entity.Room;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface RoomMapper {
    @Mapping(target = "hotelId", source = "hotel.id")
    @Mapping(target = "hotelName", source = "hotel.name")
    RoomDto toDto(Room room);
}
