package com.example.hotel.mapper;

import com.example.hotel.dto.HotelDto;
import com.example.hotel.entity.Hotel;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface HotelMapper {
    HotelDto toDto(Hotel hotel);
    @Mapping(target = "id", ignore = true)
    Hotel toEntity(HotelDto dto);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    void update(HotelDto dto, @MappingTarget Hotel hotel);
}
