# ArchMain

**Design, review, and build backend systems together, live.**

ArchMain is a shared workspace for developers, architects, and AI teammates to
design software systems on one canvas. Sketch an architecture, collaborate in
real time, review it for design risks, explore proven patterns, simulate request
flows, track versions, and generate Java Spring Boot starter structures.

## What you can do

- **Design visually:** place and connect services, APIs, databases, caches,
  queues, gateways, load balancers, and external systems on an architecture
  canvas.
- **Collaborate live:** work on a shared project with teammates, presence,
  cursors, invitations, and project roles.
- **Review architecture:** request reviews that highlight design concerns and
  provide findings and recommendations.
- **Apply patterns:** explore patterns such as API Gateway, CQRS, Event-Driven,
  Saga, Cache-Aside, and Circuit Breaker, as well as SOLID principles.
- **Track changes:** save versions, branch to explore alternatives, compare and
  merge designs, and restore earlier versions.
- **Simulate and generate:** inspect simulated request paths and export a
  project structure with Java Spring Boot starter code.
- **Connect AI:** configure supported cloud providers with your own key or use
  an Ollama instance reachable by the backend.

## Repository layout

```text
.
├── backend/
│   └── ArchMain/       Spring Boot REST and WebSocket service
└── frontend/           React + TypeScript single-page application
```

The frontend and backend are separate applications. The frontend is built as
static assets and does not require a Node.js server in production. Spring Boot
continues to run as the API and WebSocket backend.

## Requirements

- Git
- Java 17 or newer (Java 21 is a good choice)
- Node.js 20.19+ or 22.12+ and npm
- PostgreSQL for a production database (optional for local development; the
  backend uses a persistent H2 database by default)

## Run locally

Clone the repository and start the backend first:

```sh
git clone https://github.com/ilogikashvili/ArchPilot.git
cd ArchPilot/backend/ArchMain
./gradlew bootRun
```

The backend listens on port `8081` by default. On Windows, run
`gradlew.bat bootRun` from the backend project directory.

In another terminal, install and start the frontend:

```sh
cd ArchPilot/frontend
npm ci
npm run dev
```

Open the Vite URL printed in the terminal (usually
`http://localhost:5173`). During development, Vite proxies `/api` and `/ws` to
`http://127.0.0.1:8081`. Set `API_TARGET` to change the development proxy
target, or `FRONTEND_PORT` to change Vite's port.

## Backend configuration

The backend defaults to port `8081` and a persistent H2 database under the
current user's home directory. Configure a production database and secrets
through environment variables; never put production credentials in frontend
code or commit them to the repository.

| Variable | Purpose |
| --- | --- |
| `PORT` | Backend HTTP port; defaults to `8081`. |
| `SPRING_DATASOURCE_URL` | JDBC URL. For PostgreSQL, for example: `jdbc:postgresql://127.0.0.1:5432/archmain`. |
| `SPRING_DATASOURCE_USERNAME` | Database username. |
| `SPRING_DATASOURCE_PASSWORD` | Database password. |
| `ARCHMAIN_JWT_SECRET` | JWT signing secret. Set a unique, random secret of at least 32 bytes in production. |
| `ARCHMAIN_ADMIN_EMAILS` | Comma-separated email addresses to bootstrap as administrators. |
| `JPA_DDL_AUTO` | Hibernate schema behavior; choose a production-appropriate value for your deployment. |

The backend supports H2 and PostgreSQL. For production, use PostgreSQL, back up
the database, and replace all development defaults—especially the JWT secret.

## API and WebSockets

- REST API endpoints are under `/api/v1`.
- Collaboration WebSocket endpoints are under `/ws`.
- Protected REST endpoints use JWT bearer authentication.
- Keep authorization, provider keys, and other privileged operations on the
  backend; browser-side route guards are not a substitute for server-side
  authorization.

By default, the production frontend calls `/api/v1` and connects to `/ws` on
the same host. If using separate public API hosts, set `VITE_API_BASE_URL` and
`VITE_WS_BASE_URL` at frontend build time and configure backend CORS
accordingly. These `VITE_*` values are public browser configuration, not
secrets.

See the [backend API reference](backend/ArchMain/README.md) for endpoint
details.

## Build and test

Build the static frontend:

```sh
cd frontend
npm ci
npm run build
```

The deployable site is written to `frontend/dist/`.

Run frontend tests and lint:

```sh
cd frontend
npm test
npm run lint
```

Run backend tests:

```sh
cd backend/ArchMain
./gradlew test
```

## Deploy

Deploy the contents of `frontend/dist/` to a static web root; no frontend
Node.js process is required. Your web server must:

1. Serve static assets normally.
2. Proxy `/api` to the Spring Boot service at `127.0.0.1:8081`.
3. Proxy `/ws` to Spring Boot with WebSocket upgrade headers.
4. Return `index.html` for non-file frontend routes such as
   `/projects/project-123`.
5. Handle `/api` and `/ws` before the SPA fallback so unknown backend requests
   are not rewritten to `index.html`.

For a complete Hestia/Nginx example and deployment commands, see the
[frontend deployment guide](frontend/README.md).

## More documentation

- [Frontend setup and deployment](frontend/README.md)
- [Backend API reference](backend/ArchMain/README.md)
- [Agent API guide](backend/ArchMain/AGENT_API.md)
- [API contract](backend/ArchMain/API_CONTRACT.md)
