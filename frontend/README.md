# HTD RollBook Frontend

Modern admin dashboard and chat interface for the Salvation Army HTD RollBook system — soldier enrollment, contributions, events, and WhatsApp-style messaging.

## Description

This is the React + Vite single-page application (SPA) for HTD RollBook. It connects to the Spring Boot backend API for authentication, records management, payments, contributions, and chatbot features. The UI is built with Tailwind CSS and supports both web deployment and optional Capacitor Android builds.

## Features

- JWT-authenticated admin dashboard with role-based routes
- Soldier records management (search, filter, export, verification)
- Contributions and payments tracking
- Events and projects management
- WhatsApp-style chat interface with chatbot integration
- Digital ID cards and QR codes
- Responsive layout with Salvation Army themed design
- Production-ready Vite build with environment-based API configuration

## Installation

```bash
git clone https://github.com/liyandah/HTD-RollBook.git
cd HTD-RollBook/frontend
npm install
```

## Development

Start the Vite dev server (default port `5173`):

```bash
npm start
# or
npm run dev
```

Copy environment variables before running locally:

```bash
cp env.example .env
```

Ensure the backend is running (default `http://localhost:8599`). The dev server proxies `/api` and `/uploads` to the backend.

## Production Build

```bash
npm run build:prod
```

Output is written to `dist/`. Preview the production build locally:

```bash
npm run preview
```

## Deployment to Vercel

1. Push this repository to GitHub.
2. Import the project in [Vercel](https://vercel.com).
3. Set **Root Directory** to `frontend`.
4. Configure:
   - **Build Command:** `npm run build:prod`
   - **Output Directory:** `dist`
   - **Install Command:** `npm install`
5. Add environment variables (see below).
6. Deploy.

The included `vercel.json` rewrites all routes to `index.html` so React Router client-side navigation works correctly.

## Folder Structure

```
frontend/
├── public/              # Static assets served as-is
├── src/
│   ├── api/             # Axios API clients
│   ├── assets/          # Images, fonts, static imports
│   ├── components/      # Reusable UI components
│   ├── pages/           # Route-level page components
│   ├── utils/           # Helpers and utilities
│   ├── App.jsx          # Root app component & routes
│   ├── main.jsx         # Application entry point
│   └── index.css        # Global styles
├── docs/                # Project documentation
├── scripts/             # Build and deployment scripts
├── env.example          # Environment variable template
├── vercel.json          # Vercel SPA routing config
├── vite.config.js       # Vite configuration
└── package.json
```

## Environment Configuration

| Variable | Description | Example |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Backend API origin (no trailing slash). Leave empty to use same-origin or dev proxy. | `https://api.example.com` |
| `VITE_DEV_PROXY_TARGET` | Backend URL for Vite dev proxy (development only). | `http://localhost:8599` |

Create a `.env` file from the template:

```bash
cp env.example .env
```

For production on Vercel, set `VITE_API_BASE_URL` to your deployed backend URL in the Vercel project settings.

## Scripts

| Script | Description |
|--------|-------------|
| `npm start` | Start development server |
| `npm run build` | Production build (default mode) |
| `npm run build:prod` | Production build (production mode) |
| `npm test` | Test placeholder |
| `npm run lint` | Lint placeholder |

## License

Private — Salvation Army HTD RollBook project.
