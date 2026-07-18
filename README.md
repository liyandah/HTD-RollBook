# HTD RollBook (Salvation Army Highfield Temple)

Full-stack **HTD RollBook** for The Salvation Army Highfield Temple: soldier enrollment, corps administration, contributions, and related workflows. The web UI is a **React + Vite** SPA; the API is **Spring Boot**.

## Description

HTD RollBook helps corps staff manage Highfield Temple Division (HTD) roll and related records through a modern dashboard. The frontend talks to a Spring Boot backend (JWT auth, PostgreSQL, Flyway migrations). Optional WhatsApp / Dialogflow integrations support assisted data collection.

## Features

- JWT-secured admin dashboard
- Soldier / enrollment records with search, filters, and status workflows
- Corps and contributions-related views (see `frontend/src`)
- CSV / Excel export where enabled
- Salvation Army–themed UI (React, Tailwind CSS, React Router)
- Capacitor Android packaging scripts (optional)
- Spring Boot API with OpenAPI/Swagger and Flyway

## Tech Stack

| Layer | Stack |
|--------|--------|
| Frontend | React 18, Vite 5, Tailwind CSS, Axios, React Router v6 |
| Backend | Spring Boot 3.x, Java 17, Maven, PostgreSQL, Flyway, JWT |
| Deploy (UI) | Vercel (static SPA from `frontend/dist`) |

## Folder Structure

```
SA!/
├── frontend/                 # React + Vite SPA
│   ├── public/               # Static assets (e.g. shield.jpg)
│   ├── src/                  # App source
│   ├── package.json
│   ├── vite.config.js
│   └── vercel.json           # SPA rewrites for Vercel
├── src/                      # Spring Boot Java sources
├── docs/                     # Extra documentation
├── scripts/                  # Safe automation helpers (no secrets)
├── pom.xml                   # Maven backend
├── docker-compose.yml
├── env.example               # Backend env template
├── vercel.json               # Optional monorepo Vercel root config
└── README.md
```

## Prerequisites

- **Node.js** 18+ and npm
- **Java** 17+ and **Maven** 3.6+ (backend)
- **PostgreSQL** 15+ (backend)

## Installation

### 1. Clone

```bash
git clone https://github.com/liyandah/HTD-RollBook.git
cd HTD-RollBook
```

### 2. Backend

```bash
cp env.example .env
# Edit .env with your database, JWT, and admin credentials

# Start PostgreSQL (example via Docker)
docker compose up -d

# Run Spring Boot (from repo root)
./mvnw spring-boot:run
# Windows: mvnw.cmd spring-boot:run
```

Default API port is typically **8599** (confirm in `application.properties` / your env).

### 3. Frontend

```bash
cd frontend
cp .env.example .env
# Leave VITE_API_BASE_URL empty for local Vite proxy to the backend
npm install
npm start
# or: npm run dev
```

Open http://localhost:5173

## Development

| Command | Where | Purpose |
|---------|--------|---------|
| `npm start` / `npm run dev` | `frontend/` | Vite dev server with API proxy |
| `npm run build` | `frontend/` | Production SPA build → `frontend/dist` |
| `npm run build:prod` | `frontend/` | Explicit production mode build |
| `npm run preview` | `frontend/` | Preview production build locally |
| `./mvnw spring-boot:run` | root | Backend API |

Vite proxies `/api` and `/uploads` to `http://localhost:8599` (override with `VITE_DEV_PROXY_TARGET`).

## Production Build (Frontend)

```bash
cd frontend
npm install
npm run build
```

Output: `frontend/dist/` (static files for Vercel, nginx, etc.).

For **Vercel production** (`https://htd-roll-book.vercel.app`), leave `VITE_API_BASE_URL` empty — root `vercel.json` rewrites `/api/*` to the backend VM. See `docs/VERCEL-SETUP.md`.

Only set `VITE_API_BASE_URL` at build time if the API is on a **different public origin** (no trailing `/api`):

```bash
# Windows PowerShell — optional, non-Vercel deployments only
$env:VITE_API_BASE_URL="https://your-api.example.com"
npm run build
```

## Deployment to Vercel (Frontend)

Production URL: **https://htd-roll-book.vercel.app**

1. Push this repo to GitHub (`https://github.com/liyandah/HTD-RollBook.git`).
2. Vercel project: [vercel.com/liyandah/htd-roll-book](https://vercel.com/liyandah/htd-roll-book) — use repo root `vercel.json` (empty Root Directory recommended).
3. **Do not** set `VITE_API_BASE_URL` in Vercel — same-origin `/api` proxy handles the backend.
4. **Do not** use Cloudflare for `*.vercel.app` — Vercel manages that hostname.

Full steps: `docs/VERCEL-SETUP.md`.

The Spring Boot API runs on the VM (`187.77.99.225:8599`); Vercel proxies `/api` to it at deploy time via rewrites.

## Environment Configuration

### Frontend (`frontend/.env`)

| Variable | Description |
|----------|-------------|
| `VITE_API_BASE_URL` | API origin only. Empty = same-origin / Vite proxy. |

See `frontend/.env.example`.

### Backend (`env.example` → `.env`)

Typical keys: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `CORS_ALLOWED_ORIGINS`, WhatsApp/Meta tokens if used.

**Never commit `.env` or scripts with hardcoded passwords.**

## Security Notes

- `.env` and `.env.*` are gitignored.
- SSH/deploy helper scripts that contained hardcoded passwords are **excluded** via `.gitignore`.
- Rotate any credentials that were previously embedded in local deploy scripts.

## License

Internal / Salvation Army Highfield Temple use unless otherwise stated.