package com.example.booking.config;

import java.time.*;
import java.util.List;
import com.example.booking.entity.*;
import com.example.booking.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {
    @Bean
    CommandLineRunner seedUsers(UserRepository users, BookingRepository bookings, PasswordEncoder encoder) {
        return args -> {
            if (users.count() > 0) return;
            UserAccount admin = users.save(UserAccount.builder().username("admin")
                    .password(encoder.encode("admin")).role(Role.ADMIN).build());
            UserAccount user = users.save(UserAccount.builder().username("user")
                    .password(encoder.encode("user")).role(Role.USER).build());
            Instant now = Instant.now();
            LocalDate base = LocalDate.now().plusDays(30);
            bookings.saveAll(List.of(
                    sample(user, 1L, base, base.plusDays(2), BookingStatus.CONFIRMED, "sample-confirmed", now),
                    sample(user, 2L, base.plusDays(5), base.plusDays(7), BookingStatus.CANCELLED, "sample-cancelled", now),
                    sample(admin, 3L, base.plusDays(10), base.plusDays(12), BookingStatus.PENDING, "sample-pending", now)));
        };
    }

    private Booking sample(UserAccount user, Long roomId, LocalDate start, LocalDate end,
                           BookingStatus status, String requestId, Instant now) {
        return Booking.builder().user(user).roomId(roomId).startDate(start).endDate(end)
                .status(status).requestId(requestId).createdAt(now).updatedAt(now).build();
    }
}
