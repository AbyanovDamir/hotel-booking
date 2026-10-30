# Модель данных

## Booking DB

- `users`: уникальный `username`, BCrypt-пароль, роль;
- `bookings`: пользователь, номер, полуинтервал `[start_date, end_date)`, статус,
  уникальный `request_id`, временные метки;
- индексы: `user_id`, `(room_id, start_date, end_date)`.

## Hotel DB

- `hotels`: название, город, адрес;
- `rooms`: отель, номер, тип, цена, административный флаг доступности,
  накопительный `times_booked`, optimistic `version`;
- `room_reservations`: занятые интервалы и уникальный `request_id`;
- индексы: `hotel_id`, `available`, `(room_id, start_date, end_date)`.

Дата выезда не считается занятой: бронь `10..12` совместима с `12..14`.

## Среды

- default/test: H2 создаётся заново и наполняется через `CommandLineRunner`;
- docker: отдельные PostgreSQL `booking` и `hotel`, схема создаётся Flyway;
- Hibernate в Docker только валидирует схему и не изменяет её;
- volumes `booking-db-data` и `hotel-db-data` сохраняют состояние.

Демонстрационные данные: 2 пользователя, 3 отеля, 10 номеров и 3 брони.
