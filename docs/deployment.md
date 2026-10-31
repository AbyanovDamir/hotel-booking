# Развёртывание на виртуальной машине

Требования: Linux VM, 2 vCPU, 4 ГБ RAM, 15 ГБ диска, Docker Compose и `curl`.

```bash
git clone https://github.com/AbyanovDamir/hotel-booking.git
cd hotel-booking
cp .env.example .env
# Замените все секреты и пароли.
chmod +x scripts/*.sh
./scripts/docker-start.sh
./scripts/container-test.sh smoke
```

Или: `make up`, `make status`, `make smoke`, `make logs`.

Полный прогон тестов внутри контейнеров: `make test-e2e`.

Production-подобный запуск, где опубликован только Gateway:

```bash
docker compose up -d --build
docker compose ps
```

Dev-overlay дополнительно открывает `8761`, `8081`, `8082`, `5433` и `5434`.
Данные хранятся в volumes `booking-db-data` и `hotel-db-data`; обычный `down` их не
удаляет. Перед production нужны TLS reverse proxy, firewall, резервное копирование,
уникальные секреты и централизованные логи.
