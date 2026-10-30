package com.example.booking.config;

import java.io.IOException;
import java.util.*;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

public class TraceLoggingFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String traceId = Optional.ofNullable(request.getHeader("X-Request-Id"))
                .filter(value -> !value.isBlank()).orElseGet(() -> UUID.randomUUID().toString());
        MDC.put("traceId", traceId);
        response.setHeader("X-Request-Id", traceId);
        try { chain.doFilter(request, response); } finally { MDC.remove("traceId"); }
    }
}
