# Restaurant API

The backend of the restaurant system: a public menu, customer accounts and orders, and admin
management. One Spring Boot application (a monolith) with one MySQL database and a Redis cache.

The web client is the sibling **`restaurant-frontend`** project.

## Stack

Java 21 · Spring Boot 4.1.1 (Web, Data JPA, Security, Validation) · MySQL · Redis (cache) ·
JWT (jjwt) · MapStruct · Lombok

## Requirements

- JDK 21
- Docker (Docker Desktop on Windows): `docker-compose.yml` runs MySQL 8.4 and Redis 7 for development.
  The database `restaurant` is created by Docker and its tables by the API on its first start.

## Configuration

All settings live in **one file, `.env`**. Copy `.env.example` to `.env` and fill it in. It is git-ignored:
**never commit it** (it holds passwords and the JWT key). Docker Compose reads it automatically. The API
reads **environment variables** (there is no `.env` loader in the code), so load the file as shown in
"Run it".

| Variable | Required | Meaning |
|---|---|---|
| `SPRING_DATASOURCE_URL` | yes | `jdbc:mysql://localhost:3307/restaurant` (3307 = the port Docker publishes MySQL on) |
| `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD` | yes | database login. Docker creates the MySQL user from these same two values, so they are written once |
| `SPRING_DATA_REDIS_HOST`, `SPRING_DATA_REDIS_PORT` | no | only if Redis is not on `localhost:6379` (the default, and what Docker publishes) |
| `JWT-SECRET-KEY` | yes | Base64 of at least 32 random bytes: `openssl rand -base64 48`. The app refuses to start with a shorter or invalid key |
| `JWT-EXPIRATION` | yes | token lifetime in milliseconds (`3600000` = 1 hour) |
| `ADMIN-EMAIL`, `ADMIN-PASSWORD` | no | creates (or promotes) an admin at startup when both are set; password at least 8 characters |
| `CORS-ALLOWED-ORIGINS` | prod | comma-separated browser origins allowed to call the API |
| `DDL-AUTO` | no | dev only. `create` wipes and rebuilds all tables once; default `update` |
| `MYSQL_ROOT_PASSWORD`, `MYSQL_DATABASE`, `MYSQL_PORT`, `REDIS_PORT` | Docker | used only by `docker-compose.yml`: MySQL root password, database name, ports on your computer |

### Docker stack (MySQL and Redis)

With `.env` in place (see above), start the stack:

```bash
docker compose up -d       # start MySQL and Redis (the first time it downloads the images)
docker compose ps          # both must say "healthy"
docker compose logs -f     # follow the logs
docker compose down        # stop; the database is KEPT in a Docker volume
docker compose down -v     # stop AND delete the data (an empty database next time)
```

- Docker Desktop must be running whenever you work on the API.
- MySQL is published on port **3307** and Redis on **6379**, on `127.0.0.1` only (not reachable from the network).
  Port 3307 keeps it away from a MySQL installed on Windows, which uses 3306.
- The database login is in `.env` once (`SPRING_DATASOURCE_USERNAME` / `_PASSWORD`): Docker creates the MySQL
  user from it **the first time** the database is created (changing it later does not change the existing
  MySQL user), and the API logs in with it.
- Names with a hyphen (`JWT-SECRET-KEY`, `ADMIN-EMAIL`, ...) are read by the API only. Docker Compose ignores
  them, and cannot reference such names in `docker-compose.yml` (it would misread `${JWT-SECRET-KEY}`).

### Profiles

| Profile | When | Database schema | SQL logging |
|---|---|---|---|
| `dev` (default) | local work | `ddl-auto: update` keeps your data between restarts | on |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | `validate`: only checks the schema, never changes it | off |

`prod` expects the tables to exist already (there are no migrations yet), and CORS is empty until
you set `CORS-ALLOWED-ORIGINS`. In `dev` the browser origins `localhost:3000` and `localhost:5173`
are allowed.

## Run it

Start the stack first (`docker compose up -d`).

**IntelliJ:** create a run configuration for `restaurant.site.SiteApplication` and load `.env`
as its environment variables.

**Command line** (PowerShell, loads `.env` into the current session):

```powershell
Get-Content .env | Where-Object { $_ -match '^\s*[^#=\s][^=]*=' } | ForEach-Object { $k,$v = $_ -split '=',2; Set-Item -Path "env:$($k.Trim())" -Value $v.Trim() }
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

### Redis

Everything the API caches (menu, orders, users) is also in MySQL, so the cache is only an
optimisation. If Redis is down, the app **keeps working** but **slowly**: cache errors are logged as warnings and
the request is served from the database, but every cached request first waits for the Redis timeout
(currently 5 seconds) before it gives up. Lowering that timeout is the next configuration step. Cached entries expire after 10 minutes and every
write evicts the entries it affects. In Docker, Redis keeps nothing on disk, uses at most 128 MB and drops the least recently used entries when full.

### First admin

Public registration always creates a customer. Set `ADMIN-EMAIL` and `ADMIN-PASSWORD` to get the
first admin at startup. Afterwards admins promote or demote others with `PUT /users/{id}/role`
(the last admin can never be removed).

## API

Send `Accept-Language: ar` or `en` to get messages in that language. Protected endpoints need
`Authorization: Bearer <token>` (from `/auth/login` or `/auth/register`).

**Auth**

| Method and path | Who |
|---|---|
| `POST /auth/register` | everybody (creates a customer) |
| `POST /auth/login` | everybody (5 failed logins per IP and e-mail in 15 minutes are blocked with `429`) |

**Menu**

| Method and path | Who |
|---|---|
| `GET /categories`, `GET /categories/{name}` | public |
| `POST /categories`, `DELETE /categories/{name}` | admin |
| `GET /items?status=&page=&size=`, `GET /items/{id}`, `GET /items/{id}/price/{size}` | public |
| `POST /items`, `PUT /items/{id}/price`, `PUT /items/{id}/status/{status}`, `DELETE /items/{id}` | admin |

**Orders**

| Method and path | Who |
|---|---|
| `POST /orders` | any logged-in user (the customer is the token's user; prices come from the database) |
| `GET /orders/user/{userId}` | that user or an admin |
| `POST /orders/{id}/cancel` | the owner or an admin, only while `pending` |
| `GET /orders?status=&page=&size=`, `GET /orders/{id}`, `PUT /orders/{id}/status/{status}` | admin |

Order statuses: `pending → approved → completed`, and `pending → cancelled`. `completed` and
`cancelled` are final.

**Users**

| Method and path | Who |
|---|---|
| `GET /users/{id}`, `PUT /users/{id}` (only the fields you send change), `DELETE /users/{id}` | that user or an admin |
| `GET /users`, `PUT /users/{id}/role` | admin |

**Shapes.** Lists use `page` (starts at 0) and `size` (default 20, maximum 100) and return
`{ content, page, size, totalElements, totalPages }`. Errors are
`{ "status": 409, "message": "...", "errors": { "field": "..." } }` (`errors` only for validation).
Actions without data answer `{ "message": "..." }`. Money is `BigDecimal` with 2 decimals.

## Tests

```bash
./mvnw test
```

Unit tests for the services and security tests that send real HTTP requests through the real
security chain (JWT filter, roles, ownership, error format) with the services mocked. They need
**no database and no Redis**. `SiteApplicationTests` (starts the whole app) only runs when `SPRING_DATASOURCE_URL`
is set in the environment and is skipped otherwise.

## Project layout

```
src/main/java/restaurant/site/
  config/        security (JWT filter, rules, 401/403 JSON), CORS, cache, i18n, exception handler, admin bootstrap
  controller/    REST endpoints and who may call them (@PreAuthorize)
  services/      business rules (application/), JWT and login limiter (security/), messages (language/)
  repo/          Spring Data repositories
  entity/ dto/ mapper/   JPA entities, request/response objects, MapStruct mappers
  exception/     typed errors mapped to HTTP statuses
  util/          paging limits
src/main/resources/i18n/   messages.properties (default = English), messages_en, messages_ar
```

## Security notes

- Passwords are hashed with BCrypt (max 72 characters). Login answers the same for an unknown
  e-mail and a wrong password.
- The token is stateless. **Logging out forgets the token in the browser; it stays valid until it
  expires**, and a changed role takes effect when the user logs in again.
- Customers only reach their own account and orders; admin endpoints answer `403` to everybody else.

## Known limitations

- No refresh tokens, no schema migrations (Flyway), no Docker image of the API itself (only MySQL and Redis run in Docker) and no payments yet.
- Deleting a user also deletes their orders.
- The login limiter is in memory: it is per instance and resets on restart.
- Behind a reverse proxy the limiter sees the proxy's address unless forwarded headers are configured.
