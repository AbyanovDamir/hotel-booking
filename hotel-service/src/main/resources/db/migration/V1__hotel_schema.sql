CREATE TABLE hotels (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    city VARCHAR(120) NOT NULL,
    address VARCHAR(300) NOT NULL
);

CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    hotel_id BIGINT NOT NULL REFERENCES hotels(id),
    number VARCHAR(20) NOT NULL,
    type VARCHAR(40) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    available BOOLEAN NOT NULL,
    times_booked BIGINT NOT NULL DEFAULT 0,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_room_price CHECK (price >= 0),
    CONSTRAINT chk_room_times_booked CHECK (times_booked >= 0),
    CONSTRAINT uk_room_hotel_number UNIQUE (hotel_id, number)
);

CREATE TABLE room_reservations (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    request_id VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_reservation_request UNIQUE (request_id),
    CONSTRAINT chk_reservation_dates CHECK (start_date < end_date)
);

CREATE INDEX idx_room_hotel ON rooms(hotel_id);
CREATE INDEX idx_room_available ON rooms(available);
CREATE INDEX idx_reservation_room_dates ON room_reservations(room_id, start_date, end_date);
