package com.example.hotel.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

@DisplayName("Internal service authentication filter")
class InternalKeyFilterTest {
    private final InternalKeyFilter filter = new InternalKeyFilter("expected-key");

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("верный внутренний ключ назначает роль INTERNAL")
    void authenticatesValidInternalKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/rooms/1/confirm");
        request.addHeader("X-Internal-Key", "expected-key");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> { };

        filter.doFilter(request, response, chain);

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getName()).isEqualTo("booking-service");
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_INTERNAL");
    }

    @Test
    @DisplayName("внутренний ключ разрешает Booking Service получить рекомендации")
    void authenticatesRecommendationRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/rooms/recommend");
        request.addHeader("X-Internal-Key", "expected-key");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        var authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities()).extracting("authority")
                .containsExactly("ROLE_INTERNAL");
    }

    @Test
    @DisplayName("внутренний ключ не даёт доступ к административным API")
    void doesNotAuthenticateUnlistedApi() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/hotels");
        request.addHeader("X-Internal-Key", "expected-key");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("неверный внутренний ключ не создаёт аутентификацию")
    void ignoresInvalidInternalKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/rooms/1/confirm");
        request.addHeader("X-Internal-Key", "wrong-key");

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> { });

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }
}
