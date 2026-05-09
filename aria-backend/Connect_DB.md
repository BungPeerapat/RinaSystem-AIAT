# ARIA System — Database Connection

## Docker Container

```bash
# สร้าง container
docker run -d --name aria-postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=aria1234 -e POSTGRES_DB=aria_db -p 5433:5432 postgres:16

# Start / Stop / ลบ
docker start aria-postgres
docker stop aria-postgres
docker rm aria-postgres
```

## Connection Info

| Field | Value |
|---|---|
| Host | localhost |
| Port | 5433 |
| Database | aria_db |
| Username | postgres |
| Password | aria1234 |

## Connection URL

```
postgresql+asyncpg://postgres:aria1234@localhost:5433/aria_db
```

## DBeaver Setup

1. New Connection → PostgreSQL
2. Host: `localhost`, Port: `5433`
3. Database: `aria_db`
4. Username: `postgres`, Password: `aria1234`
5. Test Connection → OK
