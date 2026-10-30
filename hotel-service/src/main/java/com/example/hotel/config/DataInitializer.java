package com.example.hotel.config;

import java.math.BigDecimal;
import java.util.List;
import com.example.hotel.entity.*;
import com.example.hotel.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedHotels(HotelRepository hotels, RoomRepository rooms) {
        return args -> {
            if (hotels.count() > 0) return;
            Hotel h1 = hotels.save(Hotel.builder().name("Aurora").city("Moscow").address("Tverskaya 1").build());
            Hotel h2 = hotels.save(Hotel.builder().name("Neva").city("Saint Petersburg").address("Nevsky 10").build());
            Hotel h3 = hotels.save(Hotel.builder().name("Volga").city("Kazan").address("Baumana 7").build());
            rooms.saveAll(List.of(
                    room(h1, "101", "STANDARD", "4500", 0), room(h1, "102", "STANDARD", "4700", 2),
                    room(h1, "201", "SUITE", "9000", 5), room(h1, "202", "SUITE", "9500", 1),
                    room(h2, "11", "STANDARD", "5200", 0), room(h2, "12", "DELUXE", "7600", 3),
                    room(h2, "21", "SUITE", "11000", 7), room(h3, "301", "STANDARD", "3900", 1),
                    room(h3, "302", "STANDARD", "4100", 0), room(h3, "401", "SUITE", "8200", 4)));
        };
    }

    private Room room(Hotel hotel, String number, String type, String price, long times) {
        return Room.builder().hotel(hotel).number(number).type(type)
                .price(new BigDecimal(price)).available(true).timesBooked(times).build();
    }
}
