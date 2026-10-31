# Hotel Booking Platform

Учебная распределённая REST API-система бронирования отелей. Проект реализован как
Maven-мультимодуль на Java 17, Spring Boot 3.5.15 и Spring Cloud 2025.0.3.

## Что реализовано

- Eureka Server (`8761`) — реестр сервисов;
- API Gateway (`8080`) — маршрутизация, проверка JWT и `X-Request-Id`;
- Booking Service (`8081`) — регистрация/вход, JWT, свои брони, пагинация,
  идемпотентность и оркестрация саги;
- Hotel Service (`8082`) — CRUD отелей/номеров, фильтры, статистика, рекомендации,
  транзакционная доступность и компенсация;
- H2 для локальной разработки/тестов и две независимые PostgreSQL 17 в Docker;
- Flyway-миграции, persistent volumes, liveness/readiness health checks;
- Resilience4j retry/circuit breaker и Feign timeout;
- MapStruct, Bean Validation, единый JSON ошибок, Swagger/OpenAPI;
- 34 автоматических теста: unit, MockMvc/security и интеграционные H2/JPA.

Архитектурные схемы и sequence diagram: [docs/architecture.md](docs/architecture.md).
Решения и компромиссы: [docs/adr](docs/adr). Схема данных: [docs/database.md](docs/database.md).
Развёртывание на VM: [docs/deployment.md](docs/deployment.md).

## Технологический стек

| Компонент | Технология | Назначение |
|---|---|---|
| Язык | Java 17 | реализация сервисов |
| Сборка | Maven 3.9+ | мультимодульная сборка и тесты |
| Приложение | Spring Boot 3.5.15 | REST API, конфигурация, Actuator |
| Микросервисы | Spring Cloud 2025.0.3 | Gateway, Eureka, OpenFeign, LoadBalancer |
| Основная БД | PostgreSQL 17 | долговременное хранение в Docker/VM |
| Тестовая БД | H2 | локальная разработка и интеграционные тесты |
| Миграции | Flyway | версионирование схем PostgreSQL |
| ORM | Spring Data JPA / Hibernate | доступ к данным и транзакции |
| Безопасность | Spring Security, JWT HS256, BCrypt | аутентификация и роли |
| Отказоустойчивость | Resilience4j | retry и circuit breaker |
| API-документация | SpringDoc OpenAPI / Swagger UI | описание и проверка endpoint'ов |
| Маппинг | MapStruct | преобразование entity и DTO |
| Тестирование | JUnit 5, Mockito, MockMvc, AssertJ | unit, web и integration tests |
| Контейнеризация | Docker, Docker Compose | Linux-стенд и изолированные тесты |

## Архитектура системы

```mermaid
flowchart LR
    Client[Клиент] -->|HTTP + JWT| Gateway[API Gateway :8080]
    Gateway -->|lb://booking-service| Booking[Booking Service :8081]
    Gateway -->|lb://hotel-service| Hotel[Hotel Service :8082]
    Booking -->|Feign + X-Internal-Key| Hotel
    Gateway & Booking & Hotel -. регистрация и discovery .-> Eureka[Eureka Server :8761]
    Booking --> BookingDB[(Booking PostgreSQL)]
    Hotel --> HotelDB[(Hotel PostgreSQL)]
```

### Ответственность компонентов

| Компонент | Ответственность | Публичный доступ |
|---|---|---|
| API Gateway | единая точка входа, JWT, маршрутизация, `X-Request-Id` | да, `8080` |
| Eureka Server | регистрация и обнаружение экземпляров | только dev/internal |
| Booking Service | пользователи, бронирования, история, Saga | через Gateway |
| Hotel Service | каталог, номера, доступность, статистика | через Gateway |
| Booking PostgreSQL | пользователи и состояния бронирований | нет |
| Hotel PostgreSQL | отели, номера и занятые интервалы | нет |

Каждый бизнес-сервис владеет собственной БД. Прямые SQL-запросы между сервисами
запрещены. Booking Service оркестрирует Saga: создаёт `PENDING`, запрашивает
подтверждение Hotel Service и переводит бронь в `CONFIRMED`; при ошибке выполняет
идемпотентный `release` и устанавливает `CANCELLED`.

В Hotel Service проверка пересечения дат и создание резерва выполняются в одной
транзакции с `PESSIMISTIC_WRITE`. `Idempotency-Key`/`requestId` защищает операции от
повторного выполнения. Подробная схема и sequence diagram находятся в
[docs/architecture.md](docs/architecture.md).

## Структура проекта

```text
hotel-booking/
├── api-gateway/                    # внешняя точка входа
│   ├── src/main/java/.../gateway/  # маршрутизация, JWT, trace filter
│   ├── src/main/resources/         # Gateway routes и Actuator
│   ├── src/test/                   # тест correlation ID
│   └── Dockerfile                  # multi-stage Linux image
├── eureka-server/                  # service discovery
│   ├── src/main/                   # Eureka application и настройки
│   ├── src/test/                   # context test
│   └── Dockerfile
├── booking-service/                # пользователи и процесс бронирования
│   ├── src/main/java/.../booking/
│   │   ├── client/                 # Feign-клиент Hotel Service
│   │   ├── config/                 # security, seed data, tracing
│   │   ├── controller/             # auth и booking REST API
│   │   ├── dto/                    # входные и выходные контракты
│   │   ├── entity/                 # UserAccount, Booking, статусы
│   │   ├── exception/              # единая обработка ошибок
│   │   ├── mapper/                 # MapStruct
│   │   ├── repository/             # Spring Data JPA
│   │   ├── security/               # выпуск JWT
│   │   └── service/                # аутентификация и Saga
│   ├── src/main/resources/
│   │   ├── application.yml         # H2/local профиль
│   │   ├── application-docker.yml  # PostgreSQL/docker профиль
│   │   └── db/migration/           # Flyway schema
│   ├── src/test/                   # unit, security и MockMvc tests
│   └── Dockerfile
├── hotel-service/                  # каталог и доступность номеров
│   ├── src/main/java/.../hotel/
│   │   ├── config/                 # JWT, internal key, seed data
│   │   ├── controller/             # hotels, rooms, internal API
│   │   ├── dto/                    # API-контракты
│   │   ├── entity/                 # Hotel, Room, RoomReservation
│   │   ├── mapper/                 # MapStruct
│   │   ├── repository/             # запросы, overlap и lock
│   │   └── service/                # каталог и транзакционная доступность
│   ├── src/main/resources/         # profiles и Flyway migrations
│   ├── src/test/                   # H2/JPA integration tests
│   └── Dockerfile
├── scripts/
│   ├── docker-start.sh             # сборка и запуск Linux-стенда
│   ├── docker-stop.sh              # остановка без удаления БД
│   ├── docker-restart.sh           # пересоздание контейнеров
│   ├── docker-logs.sh              # просмотр логов
│   ├── container-test.sh           # Maven + E2E тесты в Docker
│   └── smoke-test.sh               # сквозная проверка REST API
├── docs/
│   ├── architecture.md             # схемы компонентов и Saga
│   ├── database.md                 # модель данных
│   ├── deployment.md               # развёртывание на Linux VM
│   ├── testing.md                  # тестовые сценарии
│   └── adr/                        # принятые архитектурные решения
├── docker-compose.yml              # закрытый production-подобный контур
├── docker-compose.dev.yml          # публикация диагностических портов
├── docker-compose.test.yml         # контейнерные тестовые сервисы
├── Dockerfile.test                 # образ с JDK/Maven и всеми тестами
├── Makefile                        # единые команды управления
├── .env.example                    # шаблон переменных окружения
├── pom.xml                         # родительский Maven-модуль
└── README.md
```

## Потоки взаимодействия

1. Клиент получает JWT через `/api/auth/token`.
2. Все внешние запросы поступают в API Gateway.
3. Gateway проверяет JWT и выбирает сервис через Eureka/LoadBalancer.
4. Booking Service хранит состояние процесса в собственной БД.
5. Hotel Service единолично управляет доступностью номера.
6. Временные ошибки обрабатываются timeout/retry/circuit breaker.
7. `X-Request-Id` связывает логи Gateway и обоих сервисов.

## Требования

- JDK 17+;
- Maven 3.9+;
- Docker/Compose — только для контейнерного варианта.

## Архитектурные профили

| Профиль | Хранилище | Управление схемой | Назначение |
|---|---|---|---|
| default | H2 in-memory | Hibernate | быстрый локальный запуск |
| test | изолированная H2 | Hibernate | автоматические тесты |
| docker | PostgreSQL на сервис | Flyway + validate | Docker/VM |

В контейнерной схеме наружу по умолчанию открыт только API Gateway. Booking Service
и Hotel Service владеют разными БД и не обращаются к таблицам друг друга.

## Пошаговый запуск на Linux в Docker

Ниже приведён полный сценарий с чистой Linux VM. JDK и Maven на хосте не нужны:
сборка и тесты выполняются внутри контейнеров.

### Шаг 1. Клонирование репозитория

```bash
git clone https://github.com/AbyanovDamir/hotel-booking.git
cd hotel-booking
```

### Шаг 2. Настройка окружения

```bash
cp .env.example .env
chmod +x scripts/*.sh
```

Откройте `.env` и замените `JWT_SECRET`, `INTERNAL_API_KEY`,
`BOOKING_DB_PASSWORD` и `HOTEL_DB_PASSWORD`. Значения из примера нельзя использовать
на публичном сервере.

### Шаг 3. Сборка Docker-образов

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml build
```

Multi-stage Dockerfile самостоятельно собирают executable JAR. Результат сборки
должен завершиться без ошибок для Eureka, Gateway, Booking и Hotel Service.

### Шаг 4. Запуск контейнеров

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d
```

Проверка состояния:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml ps
curl http://localhost:8080/actuator/health
```

Дождитесь состояния `healthy`. Порядок старта задаётся health checks:
PostgreSQL → Eureka → Hotel → Booking → Gateway.

После запуска доступны:

- Gateway: `http://localhost:8080`;
- Eureka: `http://localhost:8761`;
- Swagger Booking: `http://localhost:8081/swagger-ui/index.html`;
- Swagger Hotel: `http://localhost:8082/swagger-ui/index.html`;
- Booking PostgreSQL: `localhost:5433`;
- Hotel PostgreSQL: `localhost:5434`.

Демонстрационные учётные записи: `user/user` и `admin/admin`.

### Шаг 5. Тестирование

Все 34 Maven-теста внутри отдельного контейнера:

```bash
./scripts/container-test.sh unit
```

Smoke-тест работающего API внутри curl-контейнера:

```bash
./scripts/container-test.sh smoke
```

Полный сценарий — сборка тестового образа, 34 теста, запуск стенда и проверка API:

```bash
./scripts/container-test.sh all
# или
make test-e2e
```

Успешный результат содержит `BUILD SUCCESS` и `All smoke tests passed`. Подробное
описание сценариев: [docs/testing.md](docs/testing.md).

### Шаг 6. Просмотр логов

```bash
./scripts/docker-logs.sh
# только один сервис:
./scripts/docker-logs.sh booking-service
```

### Шаг 7. Остановка

Остановка контейнеров с сохранением PostgreSQL volumes:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml \
  -f docker-compose.test.yml down --remove-orphans
```

### Шаг 8. Полная очистка проекта

Следующая команда удаляет только контейнеры, локально собранные образы, сеть и
volumes этого Compose-проекта. Данные обеих PostgreSQL будут удалены:

```bash
docker compose -f docker-compose.yml -f docker-compose.dev.yml \
  -f docker-compose.test.yml down --volumes --rmi local --remove-orphans
```

Для повторной сборки с нуля выполните шаги 3–5. Не используйте
`docker system prune -a`, если на машине работают другие Docker-проекты.

## Локальный запуск без Docker

```powershell
# Windows
./start-all.ps1
```

```bash
# Linux/macOS с установленными JDK и Maven
chmod +x start-all.sh
./start-all.sh
mvn test
```

## Быстрая проверка API

Получить токен:

```bash
curl -s http://localhost:8080/api/auth/token \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"user"}'
```

Рекомендации на даты (токен подставить вместо `$TOKEN`):

```bash
curl 'http://localhost:8080/api/rooms/recommend?startDate=2099-01-10&endDate=2099-01-12' \
  -H "Authorization: Bearer $TOKEN"
```

Автоматический выбор и идемпотентное бронирование:

```bash
curl -i http://localhost:8080/api/bookings \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: demo-001' \
  -d '{"autoSelect":true,"startDate":"2099-01-10","endDate":"2099-01-12"}'
```

Повтор той же команды возвращает исходную бронь и не увеличивает `timesBooked`.

```bash
curl 'http://localhost:8080/api/bookings?page=0&size=20&sort=createdAt,desc' \
  -H "Authorization: Bearer $TOKEN"
```

## Публичные маршруты Gateway

| Метод | Путь | Доступ | Назначение |
|---|---|---|---|
| POST | `/api/auth/register` | public | регистрация USER и JWT |
| POST | `/api/auth/token` | public | аутентификация |
| POST | `/api/bookings` | USER/ADMIN | создание брони |
| GET | `/api/bookings` | USER/ADMIN | пагинированная история только текущего пользователя |
| GET | `/api/bookings/{id}` | USER/ADMIN | своя бронь |
| DELETE | `/api/bookings/{id}` | USER/ADMIN | отмена с release |
| GET | `/api/hotels`, `/api/hotels/{id}` | authenticated | каталог |
| GET | `/api/hotels/{id}/stats` | authenticated | загрузка и статистика |
| POST/PUT/DELETE | `/api/hotels/**` | ADMIN | управление отелями |
| GET | `/api/rooms` | authenticated | фильтры `hotelId`, `available`, `minTimesBooked` |
| GET | `/api/rooms/recommend` | authenticated | свободные номера по датам |
| POST/PUT/DELETE | `/api/rooms/**` | ADMIN | управление номерами |

`/internal/rooms/{id}/confirm-availability` и `/release` отсутствуют в маршрутах
Gateway и требуют `X-Internal-Key` при прямом межсервисном вызове.

## Согласованность и конкуренция

1. Booking Service сохраняет `PENDING` до удалённого вызова.
2. Hotel Service блокирует строку Room (`PESSIMISTIC_WRITE`), проверяет интервал
   `existing.start < requested.end && existing.end > requested.start`, сохраняет слот и
   увеличивает `timesBooked` в одной транзакции.
3. Успех переводит бронь в `CONFIRMED`.
4. Конфликт, таймаут или недоступность вызывают идемпотентный `release`, после чего
   бронь становится `CANCELLED`.

`requestId`/`Idempotency-Key` имеет уникальное ограничение в обеих БД. Рекомендации
сортируются по `timesBooked ASC, id ASC`.

## Ошибки и наблюдаемость

Ошибки имеют поля `timestamp`, `status`, `error`, `message`, `path`, `traceId` и
`validationErrors`. Gateway создаёт или сохраняет `X-Request-Id`; сервисы помещают его
в MDC. Переходы саги логируются с `bookingId`.

## Production hardening

Контейнерный профиль уже использует PostgreSQL и Flyway. Для production замените
HS256 на внешний OIDC issuer/RS256, внутренний ключ на mTLS/service identity, добавьте
persistent outbox и worker повторной компенсации, TLS, backup и централизованные логи.
