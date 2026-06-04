# EventSphere

EventSphere is a full-stack event management application for student events. The backend is a Spring Boot REST API with JWT authentication, CAPTCHA-based login/register, optional email OTP two-factor authentication, admin management tools, event registration, and direct chat. The frontend is a Vite + React app that consumes the API through a local development proxy.

## Project Structure

```text
.
+-- eventsphere/              # Spring Boot backend
|   +-- src/main/java/com/eventsphere
|   |   +-- config/           # Security, cache, OpenAPI, CAPTCHA config
|   |   +-- controller/       # REST controllers
|   |   +-- dto/              # Request and response DTOs
|   |   +-- entity/           # JPA entities
|   |   +-- enums/            # Role and event status enums
|   |   +-- exception/        # Global API exception handling
|   |   +-- filter/           # JWT authentication filter
|   |   +-- repository/       # Spring Data JPA repositories
|   |   +-- security/         # JWT and user details services
|   |   +-- service/          # Business logic
|   +-- src/main/resources/application.yml
|   +-- src/test/             # Unit/controller tests and Bruno API collection
+-- eventsphere-frontend/     # Vite + React frontend
    +-- src/api/              # Axios client and API wrappers
    +-- src/components/       # Shared UI components
    +-- src/context/          # Auth context
    +-- src/pages/            # Login, register, events, admin dashboard, 2FA
```

## Tech Stack

Backend:

- Java 17
- Spring Boot 3.2.0
- Spring Web, Spring Security, Spring Data JPA, Validation, Mail, Cache
- PostgreSQL for local/runtime data
- H2 for tests
- JWT with `jjwt`
- CAPTCHA with `kaptcha`
- Caffeine cache
- Springdoc OpenAPI / Swagger UI
- Maven

Frontend:

- React 18
- Vite 5
- React Router
- Axios
- CSS modules

## Features

- Student registration with CAPTCHA verification
- Login with CAPTCHA verification
- Optional two-factor authentication using email OTP codes
- JWT access tokens and refresh tokens
- Password change and logout endpoints
- Public event browsing with search, status filters, pagination, upcoming events, and past events
- Student event registration and cancellation
- Admin dashboard statistics
- Admin CRUD for events and users
- Admin student profile management
- Direct user-to-user chat with unread counts
- Swagger UI API documentation
- Bruno API collection for manual API testing

## Requirements

- Java 17+
- Maven 3.8+
- Node.js 18+
- npm
- PostgreSQL 12+

## Backend Setup

1. Create a PostgreSQL database:

```sql
CREATE DATABASE eventsphere;
```

2. Configure backend environment variables as needed:

```powershell
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_postgres_password"
$env:JWT_SECRET="replace_with_a_long_random_secret"
$env:FRONTEND_URL="http://localhost:5460"
```

The backend defaults to:

- Server port: `8081`
- Database URL: `jdbc:postgresql://localhost:5432/eventsphere`
- Frontend URL: `http://localhost:5460`

Mail is configured through Spring Boot mail settings. For local development, set your own mail values in environment-specific config or override `spring.mail.username` and `spring.mail.password` before running the app. Do not commit real email credentials.

3. Start the backend:

```powershell
cd eventsphere
mvn spring-boot:run
```

The API will be available at:

```text
http://localhost:8081
```

Swagger UI:

```text
http://localhost:8081/swagger-ui.html
```

## Frontend Setup

Install dependencies and start Vite:

```powershell
cd eventsphere-frontend
npm install
npm run dev
```

The frontend runs on:

```text
http://localhost:5460
```

During development, Vite proxies `/api` requests to:

```text
http://localhost:8081
```

For production builds, set `VITE_API_BASE_URL` to the deployed backend URL.

```powershell
$env:VITE_API_BASE_URL="https://your-api.example.com"
npm run build
```

## Useful Commands

Backend:

```powershell
cd eventsphere
mvn test
mvn package
mvn spring-boot:run
```

Frontend:

```powershell
cd eventsphere-frontend
npm run dev
npm run build
npm run preview
```

## Authentication Flow

Normal login/register uses CAPTCHA:

1. Call `GET /api/auth/captcha`.
2. Read the `X-Captcha-Token` response header.
3. Send the token and CAPTCHA answer with `POST /api/auth/register` or `POST /api/auth/login`.
4. Store the returned JWT access token and send it as:

```text
Authorization: Bearer <accessToken>
```

If two-factor authentication is enabled for the user, login returns `requiresTwoFactor=true`. Complete the login with:

```text
POST /api/auth/verify-2fa
```

There is also a development API-testing login endpoint:

```text
POST /api/auth/login-dev
```

It accepts username/email and password without CAPTCHA.

## Seed Admin User

The backend includes a setup endpoint:

```text
POST /api/setup/seed-admin
```

It creates the default admin user if missing and resets user passwords to known development values. Use this only in local development or controlled test environments.

Default development credentials created by the endpoint:

```text
admin / Admin@1234
```

## Main API Endpoints

Auth:

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/auth/captcha` | Generate CAPTCHA image and token |
| POST | `/api/auth/register` | Register a student account |
| POST | `/api/auth/login` | Login with CAPTCHA |
| POST | `/api/auth/login-dev` | Login without CAPTCHA for API testing |
| POST | `/api/auth/send-otp` | Send 2FA OTP |
| POST | `/api/auth/verify-2fa` | Verify 2FA OTP |
| POST | `/api/auth/refresh` | Refresh JWT tokens |
| POST | `/api/auth/change-password` | Change current user's password |
| PATCH | `/api/auth/2fa/enable` | Enable 2FA |
| PATCH | `/api/auth/2fa/disable` | Disable 2FA |
| POST | `/api/auth/logout` | Logout |

Events:

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/events` | Paginated event list with `page`, `size`, `search`, `status` |
| GET | `/api/events/{id}` | Get event by ID |
| GET | `/api/events/upcoming` | Upcoming events |
| GET | `/api/events/past` | Past events |
| POST | `/api/events` | Create event, admin only |
| PUT | `/api/events/{id}` | Update event, admin only |
| DELETE | `/api/events/{id}` | Delete event, admin only |
| POST | `/api/events/{id}/register` | Register current student for event |
| DELETE | `/api/events/{id}/register` | Cancel current student's event registration |

Admin and management:

| Method | Endpoint | Description |
| --- | --- | --- |
| GET | `/api/admin/dashboard` | Dashboard statistics, admin only |
| GET | `/api/users` | List users, admin only |
| GET | `/api/users/me` | Get current user profile |
| GET | `/api/users/{id}` | Get user by ID, admin only |
| POST | `/api/users` | Create user, admin only |
| PUT | `/api/users/{id}` | Update user, admin only |
| DELETE | `/api/users/{id}` | Delete user, admin only |
| GET | `/api/students` | List students, admin only |
| GET | `/api/students/{id}` | Get student by ID, admin only |
| PATCH | `/api/students/{id}` | Update student profile, admin only |

Chat:

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/chat/send` | Send direct message |
| GET | `/api/chat/conversation/{userId}` | Get conversation with another user |
| GET | `/api/chat/unread-count` | Get unread message count |

Setup:

| Method | Endpoint | Description |
| --- | --- | --- |
| POST | `/api/setup/seed-admin` | Seed/reset local development users |

## Roles and Event Statuses

Roles:

- `ADMIN`
- `STUDENT`

Event statuses:

- `ACTIVE`
- `CANCELLED`
- `COMPLETED`

## Testing

Backend tests use H2 with `src/test/resources/application-test.yml`, so PostgreSQL is not required for the test suite.

Run:

```powershell
cd eventsphere
mvn test
```

Current test areas include:

- Auth service
- Event service
- Event controller
- JWT token provider
- Test security configuration

## API Testing Collection

Bruno collection files are included at:

```text
eventsphere/src/test/resources/bruno-apis/eventsphere
```

The collection includes requests for:

- CAPTCHA
- Events
- npm/frontend helper notes
- Open collection metadata

## Notes for Contributors

- Keep secrets out of `application.yml`; prefer environment variables or local-only config.
- The backend listens on `8081`.
- The frontend listens on `5460` and proxies `/api` to the backend during development.
- Use the Swagger UI for live endpoint documentation and request schemas.
- The repository currently contains generated folders such as `target`, `dist`, and `node_modules`; these should generally not be committed in normal project workflow.
