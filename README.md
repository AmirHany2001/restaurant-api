# Restaurant API

The backend of the restaurant system: a public menu, customer accounts and orders, and admin
management. One Spring Boot application (a monolith) with one MySQL database and a Redis cache.

The web client is the sibling **`restaurant-frontend`** project.

## Stack

Java 21 · Spring Boot 4.1.1 (Web, Data JPA, Security, Validation) · MySQL · Redis (cache) ·
JWT (jjwt) · MapStruct · Lombok

## Requirements

- JDK 21
- MySQL with an **empty database named `restaurant`** (the tables are created on first start)
- Redis (see "Redis" below: the API still works without it, only slower)

## Configuration

The app reads **environment variables**; there is no `.env` loader in the code. Copy
`keys.env.example` to `keys.env` and fill it in. `keys.env` is git-ignored: **never commit it**.

| Variable | Required | Meaning |
|---|---|---|
| `DB-DRIVER` | yes | `com.mysql.cj.jdbc.Driver` |
| `DB-URL` | yes | e.g. `jdbc:mysql://localhost:3306/restaurant` |
| `DB-USERNAME`, `DB-PASSWORD` | yes | database login |
| `CACHE-HOST`, `CACHE-PORT` | yes | Redis, e.g. `localhost` / `6379` |
| `JWT-SECRET-KEY` | yes | Base64 of at least 32 random bytes: `openssl rand -base64 48`. The app refuses to start with a shorter or invalid key |
| `JWT-EXPIRATION` | yes | token lifetime in milliseconds (`3600000` = 1 hour) |
| `ADMIN-EMAIL`, `ADMIN-PASSWORD` | no | creates (or promotes) an admin at startup when both are set; password at least 8 characters |
| `CORS-ALLOWED-ORIGINS` | prod | comma-separated browser origins allowed to call the API |
| `DDL-AUTO` | no | dev only. `create` wipes and rebuilds all tables once; default `update` |

### Profiles

| Profile | When | Database schema | SQL logging |
|---|---|---|---|
| `dev` (default) | local work | `ddl-auto: update` keeps your data between restarts | on |
| `prod` | `SPRING_PROFILES_ACTIVE=prod` | `validate`: only checks the schema, never changes it | off |

`prod` expects the tables to exist already (there are no migrations yet), and CORS is empty until
you set `CORS-ALLOWED-ORIGINS`. In `dev` the browser origins `localhost:3000` and `localhost:5173`
are allowed.

## Run it

**IntelliJ:** create a run configuration for `restaurant.site.SiteApplication` and load `keys.env`
as its environment variables.

**Command line** (PowerShell, loads `keys.env` into the current session):

```powershell
Get-Content keys.env | Where-Object { $_ -match '^\s*[^#=\s][^=]*=' } | ForEach-Object { $k,$v = $_ -split '=',2; Set-Item -Path "env:$($k.Trim())" -Value $v.Trim() }
./mvnw spring-boot:run
```

The API listens on `http://localhost:8080`.

### Redis

Everything the API caches (menu, orders, users) is also in MySQL, so the cache is only an
optimisation. If Redis is down, the app **keeps working**: cache errors are logged as warnings
and the request is served from the database. Cached entries expire after 10 minutes and every
write evicts the entries it affects. A quick local Redis: `docker run -d --name redis -p 6379:6379 redis:7`.

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
**no database and no Redis**. `SiteApplicationTests` (starts the whole app) only runs when `DB-URL`
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

- No refresh tokens, no schema migrations (Flyway), no Docker files and no payments yet.
- Deleting a user also deletes their orders.
- The login limiter is in memory: it is per instance and resets on restart.
- Behind a reverse proxy the limiter sees the proxy's address unless forwarded headers are configured.
