# Архитектура

## Цели

- независимое владение данными каждым бизнес-сервисом;
- единая внешняя точка входа;
- идемпотентное бронирование без распределённой транзакции;
- воспроизводимый локальный и контейнерный запуск;
- контролируемая деградация при сбоях зависимостей.

## Компоненты

```mermaid
flowchart LR
  C[Клиент] -->|JWT + Idempotency-Key| G[API Gateway :8080]
  G -->|lb://booking-service| B[Booking Service :8081]
  G -->|lb://hotel-service| H[Hotel Service :8082]
  B -->|Feign + X-Internal-Key| H
  G & B & H -. discovery .-> E[Eureka :8761]
  B --> BDB[(Booking PostgreSQL)]
  H --> HDB[(Hotel PostgreSQL)]
```

| Компонент | Ответственность | Данные |
|---|---|---|
| API Gateway | маршрутизация, JWT, correlation ID | нет |
| Eureka | обнаружение сервисов | временный registry |
| Booking Service | пользователи, брони, оркестрация Saga | `users`, `bookings` |
| Hotel Service | каталог, доступность, блокировки | `hotels`, `rooms`, `room_reservations` |

Сервисы не читают таблицы друг друга. Межсервисное взаимодействие проходит только
через внутренний HTTP-контракт.

## Слои бизнес-сервисов

```mermaid
flowchart LR
  HTTP[controller / DTO / validation] --> APP[service / use cases]
  APP --> DOMAIN[entity / business rules]
  APP --> PORTS[repository + client interfaces]
  PORTS --> DB[(JPA / PostgreSQL)]
  PORTS --> REMOTE[Feign / Hotel API]
```

Контроллеры отвечают за HTTP-контракт, сервисы — за сценарии и транзакции,
репозитории — за хранение, Feign-фасад — за отказоустойчивые удалённые вызовы.

## Последовательность бронирования

```mermaid
sequenceDiagram
  actor User
  participant Gateway
  participant Booking
  participant Hotel
  participant BookingDB
  participant HotelDB
  User->>Gateway: POST /api/bookings + JWT + Idempotency-Key
  Gateway->>Booking: authenticated request + X-Request-Id
  Booking->>BookingDB: findByRequestId
  alt повтор запроса
    BookingDB-->>Booking: существующая бронь
    Booking-->>User: прежний результат
  else новый запрос
    Booking->>BookingDB: INSERT PENDING
    opt autoSelect
      Booking->>Hotel: recommendations
      Hotel->>HotelDB: свободные номера
      Hotel-->>Booking: ordered rooms
    end
    Booking->>Hotel: confirm-availability(requestId)
    Hotel->>HotelDB: lock Room + overlap check + INSERT
    alt успех
      Booking->>BookingDB: PENDING -> CONFIRMED
      Booking-->>User: 201
    else конфликт/сбой
      Booking->>Hotel: release(requestId)
      Booking->>BookingDB: PENDING -> CANCELLED
      Booking-->>User: 409
    end
  end
```

Слот — полуинтервал `[startDate, endDate)`. Confirm/release идемпотентны благодаря
уникальному `requestId`. Retry не применяется к бизнес-конфликтам `409`.

## Развёртывание и профили

Базовый `docker-compose.yml` публикует только Gateway. Dev-overlay открывает Eureka,
сервисы и БД для Swagger и отладки. Приложения работают непривилегированным
пользователем, имеют health checks и стартуют после готовности зависимостей.

| Профиль | База | Схема | Назначение |
|---|---|---|---|
| default | H2 | Hibernate `create-drop` | локальная разработка |
| test | отдельная H2 | Hibernate `create-drop` | автотесты |
| docker | PostgreSQL 17 | Flyway + Hibernate `validate` | VM/контейнеры |

## Отказоустойчивость

- Feign timeout 2 секунды, retry с backoff и circuit breaker;
- `X-Request-Id`, `bookingId` и `requestId` попадают в MDC;
- Actuator предоставляет liveness/readiness и метрики;
- PostgreSQL volumes сохраняют состояние при пересоздании контейнеров.

Следующий production-шаг — persistent outbox/inbox и worker повторной компенсации.
