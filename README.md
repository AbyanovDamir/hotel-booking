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

## Ручное тестирование API

Команды ниже выполняются в Linux/VM после запуска стенда из шага 4. Для прямого
доступа к Eureka, Swagger и PostgreSQL обязательно используйте также
`docker-compose.dev.yml`, который публикует служебные порты на `localhost`.

### 1. Подготовка переменных

Переменные достаточно задать один раз в текущем терминале:

```bash
export GATEWAY="http://localhost:8080"
export EUREKA="http://localhost:8761"
export BOOKING_SWAGGER="http://localhost:8081/swagger-ui/index.html"
export HOTEL_SWAGGER="http://localhost:8082/swagger-ui/index.html"

export USER_NAME="user"
export USER_PASS="user"
export ADMIN_NAME="admin"
export ADMIN_PASS="admin"
```

Универсальная функция получения JWT:

```bash
get_token() {
  local username=$1
  local password=$2
  curl -sf "$GATEWAY/api/auth/token" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$username\",\"password\":\"$password\"}" \
    | grep -o '"token":"[^"]*"' | cut -d'"' -f4
}

export USER_TOKEN=$(get_token "$USER_NAME" "$USER_PASS")
export ADMIN_TOKEN=$(get_token "$ADMIN_NAME" "$ADMIN_PASS")

test -n "$USER_TOKEN" && echo "USER token: OK"
test -n "$ADMIN_TOKEN" && echo "ADMIN token: OK"
```

### 2. Проверка инфраструктуры

```bash
# Состояние контейнеров: все прикладные сервисы должны быть healthy
docker compose -f docker-compose.yml -f docker-compose.dev.yml ps

# Gateway health: ожидается HTTP 200 и status=UP
curl -i "$GATEWAY/actuator/health"

# Зарегистрированные в Eureka приложения
curl -s "$EUREKA/eureka/apps" -H 'Accept: application/json' \
  | grep -o '"name":"[^"]*"'
```

Ожидаются приложения `EUREKA-SERVER`, `HOTEL-SERVICE`, `BOOKING-SERVICE` и
`API-GATEWAY`.

### 3. Регистрация, авторизация и защита маршрутов

```bash
NEW_USER="testuser_$(date +%s)"

# Регистрация: ожидается HTTP 201
curl -i -X POST "$GATEWAY/api/auth/register" \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$NEW_USER\",\"password\":\"pass123\"}"

NEW_TOKEN=$(get_token "$NEW_USER" 'pass123')
test -n "$NEW_TOKEN" && echo "New user token: OK"

# История нового пользователя: ожидается HTTP 200 и пустой content
curl -i "$GATEWAY/api/bookings?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer $NEW_TOKEN"

# Без JWT: ожидается HTTP 401
curl -s -o /dev/null -w 'Unauthorized: HTTP %{http_code}\n' \
  "$GATEWAY/api/bookings"
```

### 4. Каталог отелей и номеров

```bash
curl -i "$GATEWAY/api/hotels" \
  -H "Authorization: Bearer $USER_TOKEN"

curl -i "$GATEWAY/api/rooms?available=true" \
  -H "Authorization: Bearer $USER_TOKEN"

curl -i "$GATEWAY/api/rooms/recommend?startDate=2099-01-10&endDate=2099-01-12" \
  -H "Authorization: Bearer $USER_TOKEN"

# Начальные данные содержат отель id=1
curl -i "$GATEWAY/api/hotels/1/stats" \
  -H "Authorization: Bearer $USER_TOKEN"
```

Все четыре запроса должны вернуть HTTP 200.

### 5. Создание брони и проверка идемпотентности

```bash
IDEM_KEY="manual-$(date +%s)"
BOOKING_BODY='{"autoSelect":true,"startDate":"2099-01-10","endDate":"2099-01-12"}'

FIRST_BOOKING=$(curl -sf -X POST "$GATEWAY/api/bookings" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $IDEM_KEY" \
  -d "$BOOKING_BODY")
echo "$FIRST_BOOKING"

SECOND_BOOKING=$(curl -sf -X POST "$GATEWAY/api/bookings" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -H "Idempotency-Key: $IDEM_KEY" \
  -d "$BOOKING_BODY")

test "$FIRST_BOOKING" = "$SECOND_BOOKING" \
  && echo 'Idempotency: OK' \
  || echo 'Idempotency: FAIL'
```

Оба запроса должны вернуть одну и ту же бронь; повтор не должен увеличивать
`timesBooked`.

### 6. История и отмена брони

```bash
BOOKINGS=$(curl -sf \
  "$GATEWAY/api/bookings?page=0&size=10&sort=createdAt,desc" \
  -H "Authorization: Bearer $USER_TOKEN")
echo "$BOOKINGS"

BOOKING_ID=$(echo "$BOOKINGS" | grep -o '"id":[0-9]*' | head -1 | cut -d: -f2)
echo "Booking ID: $BOOKING_ID"

if [ -n "$BOOKING_ID" ]; then
  curl -i -X DELETE "$GATEWAY/api/bookings/$BOOKING_ID" \
    -H "Authorization: Bearer $USER_TOKEN"
fi
```

Удаление должно вернуть HTTP 204, а статус брони при следующем чтении —
`CANCELLED`.

### 7. Проверка роли ADMIN

```bash
# ADMIN: ожидается HTTP 201
curl -i -X POST "$GATEWAY/api/hotels" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Manual Test Hotel","city":"Moscow","address":"Test Street 1"}'

# USER: ожидается HTTP 403
curl -s -o /dev/null -w 'USER create hotel: HTTP %{http_code}\n' \
  -X POST "$GATEWAY/api/hotels" \
  -H "Authorization: Bearer $USER_TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"name":"Forbidden Hotel","city":"Moscow","address":"X"}'
```

### 8. Защита внутренних эндпоинтов

```bash
# Прямой вызов без X-Internal-Key: ожидается HTTP 401 или 403
curl -s -o /dev/null -w 'Direct internal: HTTP %{http_code}\n' -X POST \
  'http://localhost:8082/internal/rooms/1/confirm-availability' \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"manual","startDate":"2099-01-10","endDate":"2099-01-12"}'

# Через Gateway внутренний маршрут не опубликован: ожидается HTTP 404
curl -s -o /dev/null -w 'Internal via Gateway: HTTP %{http_code}\n' -X POST \
  "$GATEWAY/internal/rooms/1/confirm-availability" \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"manual","startDate":"2099-01-10","endDate":"2099-01-12"}'
```

### 9. Swagger/OpenAPI

Интерактивная документация открывается в браузере:

- Booking: `http://localhost:8081/swagger-ui/index.html`;
- Hotel: `http://localhost:8082/swagger-ui/index.html`.

Проверка спецификаций из терминала:

```bash
curl -s -o /dev/null -w 'Booking OpenAPI: HTTP %{http_code}\n' \
  http://localhost:8081/v3/api-docs
curl -s -o /dev/null -w 'Hotel OpenAPI: HTTP %{http_code}\n' \
  http://localhost:8082/v3/api-docs
```

Оба запроса должны вернуть HTTP 200.

### 10. Проверка PostgreSQL

```bash
COMPOSE='docker compose -f docker-compose.yml -f docker-compose.dev.yml'

$COMPOSE exec -T booking-db psql -U booking -d booking -c '\dt'
$COMPOSE exec -T booking-db psql -U booking -d booking \
  -c 'SELECT id, status, start_date, end_date, created_at FROM bookings ORDER BY created_at DESC LIMIT 5;'

$COMPOSE exec -T hotel-db psql -U hotel -d hotel -c '\dt'
$COMPOSE exec -T hotel-db psql -U hotel -d hotel \
  -c 'SELECT * FROM room_reservations ORDER BY id DESC LIMIT 5;'
```

### 11. Автоматическая сверка ручного сценария

Тот же основной пользовательский путь можно проверить одной командой в
изолированном curl-контейнере:

```bash
./scripts/container-test.sh smoke
```

Успешное завершение содержит `All smoke tests passed`. После проверки остановите
стенд командой из шага 7 раздела запуска.

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
