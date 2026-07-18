# HTD RollBook — Vercel project setup

Configure the **HTD-RollBook** frontend on Vercel with GitHub. **Production uses the Vercel hostname only** — no custom domain required.

| Item | Value |
|------|-------|
| Vercel project | [vercel.com/liyandah/htd-roll-book](https://vercel.com/liyandah/htd-roll-book) |
| **Production URL** | **https://htd-roll-book.vercel.app** |
| GitHub repo | [github.com/liyandah/HTD-RollBook](https://github.com/liyandah/HTD-RollBook) |
| Production branch | `main` |
| Framework | Vite (React) |
| Build output | `frontend/dist` |

Related docs: [DEPLOYMENT-SERVER-187.77.99.225.md](./DEPLOYMENT-SERVER-187.77.99.225.md) (backend VM).

---

## Architecture (Vercel-only)

```
Browser → https://htd-roll-book.vercel.app
              ├── /*           → SPA (static)
              └── /api/*       → Vercel rewrite → http://187.77.99.225:8599/api/*
```

- The frontend calls **same-origin** `/api/...` (empty `VITE_API_BASE_URL` at build time).
- **Do not** point `VITE_API_BASE_URL` at `https://api.htdrollbook.com` — that domain is not in use unless you register it and configure DNS later.
- **Do not** add `htd-roll-book.vercel.app` to Cloudflare. `*.vercel.app` is issued and served by Vercel; Cloudflare is only relevant if you add a **custom** domain later.

Root `vercel.json`:

```json
{
  "installCommand": "cd frontend && npm ci",
  "buildCommand": "cd frontend && npm run build:prod",
  "outputDirectory": "frontend/dist",
  "framework": "vite",
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "http://187.77.99.225:8599/api/:path*"
    },
    { "source": "/(.*)", "destination": "/index.html" }
  ]
}
```

**Alternative:** set **Root Directory** to `frontend` — Vercel uses `frontend/vercel.json` (same rewrites, paths relative to `frontend/`).

---

## Step 1 — Connect Git repository

Open: [https://vercel.com/liyandah/htd-roll-book](https://vercel.com/liyandah/htd-roll-book)

1. **Settings → Git** → connect **`liyandah/HTD-RollBook`**.
2. **Production Branch**: **`main`**.

---

## Step 2 — Build settings

| Setting | Value |
|---------|-------|
| **Root Directory** | *(empty — repo root)* recommended |
| **Build Command** | `cd frontend && npm run build:prod` *(from `vercel.json`)* |
| **Output Directory** | `frontend/dist` |
| **Install Command** | `cd frontend && npm ci` |

Do **not** override the build command to inject `VITE_API_BASE_URL=https://api.htdrollbook.com` — that breaks same-origin proxying.

---

## Step 3 — Environment variables

**Leave `VITE_API_BASE_URL` unset** in Vercel (Production and Preview).

| Name | Value | Environments |
|------|-------|--------------|
| `VITE_API_BASE_URL` | *(delete if present)* | — |

If this variable was set previously, remove it and **Redeploy** so the bundle uses relative `/api/...` paths.

Local dev: copy `frontend/env.example` → `frontend/.env` and leave `VITE_API_BASE_URL` empty.

---

## Step 4 — Deploy

Push to `main` or **Redeploy** from the Vercel dashboard.

Live URL: **https://htd-roll-book.vercel.app**

Login: **https://htd-roll-book.vercel.app/login**

---

## Step 5 — Verify

```bash
# Frontend
curl -sI https://htd-roll-book.vercel.app

# Login via Vercel /api proxy (replace password with value from server htf-backend.env)
curl -s -X POST https://htd-roll-book.vercel.app/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"YOUR_ADMIN_PASSWORD"}'
```

Expected: HTTP 200 with a JWT `token` in JSON.

Backend CORS on `187.77.99.225` must include `https://htd-roll-book.vercel.app` and `https://*.vercel.app` (for browser calls if you ever bypass the proxy). Same-origin proxy requests do not need CORS for typical SPA usage.

---

## Optional — custom domain later

If you register **`htdrollbook.com`** later:

1. Add the domain in **Vercel → Settings → Domains** (not Cloudflare for `*.vercel.app`).
2. Point DNS at Vercel per their instructions (Cloudflare can front the custom domain if you choose).
3. Add `https://htdrollbook.com` to `CORS_ALLOWED_ORIGINS` on the server.
4. Still prefer **empty** `VITE_API_BASE_URL` and the `/api` rewrite unless you deploy a separate public API host.

See [CLOUDFLARE-SETUP.md](./CLOUDFLARE-SETUP.md) only when you adopt a custom domain + `api.*` subdomain.

---

## Troubleshooting

| Symptom | Action |
|---------|--------|
| API calls go to `api.htdrollbook.com` | Remove `VITE_API_BASE_URL` from Vercel; redeploy |
| Login 404 `/api/api/...` | Env must be empty or origin only — never `.../api` |
| Build fails on `npm ci` | Commit `frontend/package-lock.json` |
| SPA routes 404 on refresh | Ensure root `vercel.json` rewrites are active |
| Used Cloudflare on `*.vercel.app` | Remove — Vercel manages that hostname |

---

## Quick reference

| What | Value |
|------|-------|
| Production URL | **https://htd-roll-book.vercel.app** |
| Login URL | **https://htd-roll-book.vercel.app/login** |
| Admin username | `admin` (see `ADMIN_USERNAME` in server `htf-backend.env`) |
| API proxy | `/api/*` → `http://187.77.99.225:8599/api/*` |
| `VITE_API_BASE_URL` | **unset** on Vercel |
| Cloudflare | **Not used** for `htd-roll-book.vercel.app` |
