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

Set `VITE_API_BASE_URL` at **build time** to your public API origin (no trailing `/api`), for example:

```bash
# Windows PowerShell
$env:VITE_API_BASE_URL="https://your-api.example.com"
npm run build
```

## Deployment to Vercel (Frontend)

1. Push this repo to GitHub (already: `https://github.com/liyandah/HTD-RollBook.git`).
2. In [Vercel](https://vercel.com): **Add New Project** → import `liyandah/HTD-RollBook`.
3. Configure:
   - **Root Directory**: `frontend`
   - **Framework Preset**: Vite
   - **Build Command**: `npm run build`
   - **Output Directory**: `dist`
4. Environment variables (Production):
   - `VITE_API_BASE_URL` = your HTTPS API base (no trailing `/api`)
5. Deploy. SPA routing is covered by `frontend/vercel.json` rewrites to `index.html`.

The Spring Boot API is **not** hosted on Vercel; deploy it separately (VM, container, PaaS) and point CORS / `VITE_API_BASE_URL` at that host.

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