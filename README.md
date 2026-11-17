# Online Bookstore

A small online bookstore built as a code kata: browse books, add them to a cart, change quantities, remove items and check out. The repository contains a React frontend and a Spring Boot REST API in separate folders.

## Stack

- **Backend:** Java 17, Spring Boot 3.4, Spring Security, Spring Data JPA, H2, JJWT, Maven
- **Frontend:** React 18, TypeScript, Vite, React Query, React Router, Axios


```
backend/    REST API (port 8080)
frontend/   React SPA (port 5173, proxies /api to the backend)

The backend is organized in layers:

```
backend/src/main/java/com/bookstore/
  controller/   REST endpoints (HTTP in, JSON out)
  service/      business logic and transactions
  repository/   Spring Data JPA interfaces (the DAO layer)
  model/        JPA entities
  dto/          request and response records
  security/     JWT, rate limiting, idempotency
  common/       API errors and shared helpers
```

## Prerequisites

- JDK 17+
- Maven 3.9+
- Node.js 20.19+ and npm

## Running locally

### Backend

```bash
cd backend
mvn spring-boot:run
```

The API starts on `http://localhost:8080`. On first start it seeds 10 books and creates an H2 file database at `backend/data/bookstore.mv.db`. Delete the `backend/data` folder to reset all data.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Open `http://localhost:5173`. The Vite dev server proxies `/api` to `http://localhost:8080`, so no CORS setup is needed for local development.

### Demo flow

1. Create an account (or sign in).
2. Add books to the cart — adding only checks availability.
3. Open the cart, change quantities with `−` / `+`, or remove items.
4. Proceed to checkout and place the order to see the order confirmation.

## Tests

```bash
cd backend && mvn test     # 15 tests
cd frontend && npm test    # 6 tests
cd frontend && npm run build
```

Backend coverage includes registration/login, refresh-token rotation and reuse detection, book caching and DB-down fallback, cart add/update/remove, stock limits, idempotent replay, checkout, and a concurrency test where two users race for the last copy of a book. Frontend coverage includes currency formatting, book card behavior and cart quantity controls.

## API

| Method | Path | Auth | Description |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | public | Create an account, returns an access token and sets the refresh cookie |
| POST | `/api/auth/login` | public | Sign in |
| POST | `/api/auth/refresh` | refresh cookie | Rotate the refresh token and issue a new access token |
| POST | `/api/auth/logout` | refresh cookie | Revoke the refresh token |
| GET | `/api/auth/me` | bearer | Current user |
| GET | `/api/books` | public | Book catalog |
| GET | `/api/cart` | bearer | Current cart |
| POST | `/api/cart/items` | bearer + `Idempotency-Key` | Add a book to the cart |
| PATCH | `/api/cart/items/{id}` | bearer | Change item quantity |
| DELETE | `/api/cart/items/{id}` | bearer | Remove an item |
| POST | `/api/orders` | bearer + `Idempotency-Key` | Check out the cart |
| GET | `/api/orders` | bearer | List orders |
| GET | `/api/orders/{id}` | bearer | Single order |

Errors use a consistent body:

```json
{
  "timestamp": "2026-11-15T16:20:11Z",
  "status": 409,
  "code": "OUT_OF_STOCK",
  "message": "Only 1 copies left in stock",
  "path": "/api/cart/items"
}
```

Validation errors additionally contain a `fieldErrors` map. HTTP status codes used: 200, 201, 204, 400, 401, 403, 404, 409, 422, 429, 500, 503.

### Idempotency

`POST /api/cart/items` and `POST /api/orders` require an `Idempotency-Key` header (8–64 characters: letters, digits, `-`, `_`).

- Replaying the same key with the same payload returns the stored response with `Idempotency-Replayed: true`.
- Reusing a key with a different payload returns `422 IDEMPOTENCY_KEY_REUSED`.
- A concurrent request with an in-progress key returns `409 REQUEST_IN_PROGRESS`.

The frontend generates a UUID per user action. Stored responses are kept so that later replays return the same result.

## Notes and limitations

- H2 file storage keeps the kata self-contained. For production, switch to MySQL/PostgreSQL and manage schema changes with Flyway or Liquibase instead of `ddl-auto: update`.
- Rate limiting and the catalog cache are in-process; a multi-instance deployment would move both to a shared store (Redis).
- Payment is simulated: checkout creates a `CONFIRMED` order.
- Strict refresh rotation means a rare simultaneous refresh from two tabs can invalidate the older tab's session.
