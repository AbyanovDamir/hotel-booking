package com.example.hotel.config;

import java.io.IOException;
import java.util.List;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalKeyFilter extends OncePerRequestFilter {
    private final String expectedKey;

    public InternalKeyFilter(String expectedKey) {
        this.expectedKey = expectedKey;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String requestUri = request.getRequestURI();
        boolean serviceEndpoint = requestUri.startsWith("/internal/")
                || requestUri.equals("/api/rooms/recommend");
        if (serviceEndpoint && expectedKey.equals(request.getHeader("X-Internal-Key"))) {
            var authentication = new UsernamePasswordAuthenticationToken("booking-service", null,
                    List.of(new SimpleGrantedAuthority("ROLE_INTERNAL")));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
