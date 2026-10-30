package com.example.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class TraceIdFilterTest {
    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    void preservesClientRequestId() {
        var exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/").header(TraceIdFilter.HEADER, "trace-42").build());
        AtomicReference<String> forwarded = new AtomicReference<>();

        filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(TraceIdFilter.HEADER));
            return current.getResponse().setComplete();
        }).block();

        assertThat(forwarded.get()).isEqualTo("trace-42");
        assertThat(exchange.getResponse().getHeaders().getFirst(TraceIdFilter.HEADER)).isEqualTo("trace-42");
    }
}
