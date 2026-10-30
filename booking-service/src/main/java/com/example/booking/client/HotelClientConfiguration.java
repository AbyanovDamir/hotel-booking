package com.example.booking.client;

import com.example.booking.exception.HotelConflictException;
import feign.*;
import feign.codec.ErrorDecoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class HotelClientConfiguration {
    @Bean
    RequestInterceptor internalKeyInterceptor(@Value("${security.internal-key}") String key) {
        return template -> template.header("X-Internal-Key", key);
    }

    @Bean
    ErrorDecoder hotelErrorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 409) {
                return new HotelConflictException("Room is not available for the selected dates");
            }
            return FeignException.errorStatus(methodKey, response);
        };
    }
}
