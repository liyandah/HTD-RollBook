# HTD RollBook — Vercel project setup

Configure the **HTD-RollBook** frontend on Vercel with GitHub, environment variables, and custom domains.

| Item | Value |
|------|-------|
| Vercel project | [vercel.com/liyandah/htd-roll-book](https://vercel.com/liyandah/htd-roll-book) |
| GitHub repo | [github.com/liyandah/HTD-RollBook](https://github.com/liyandah/HTD-RollBook) |
| Production branch | `main` |
| Framework | Vite (React) |
| Build output | `frontend/dist` |

Related docs: [CLOUDFLARE-SETUP.md](./CLOUDFLARE-SETUP.md) (DNS, API, CORS).

---

## Before you start

1. You need access to the **liyandah** Vercel team/account and the **HTD-RollBook** GitHub repo.
2. Cloudflare DNS for `htdrollbook.com` should already include frontend records (see [DNS verification](#dns-records-cloudflare) below).
3. The repo root contains `vercel.json`, which tells Vercel how to build `frontend/`:

```json
{
  "installCommand": "cd frontend && npm ci",
  "buildCommand": "cd frontend && npm run build:prod",
  "outputDirectory": "frontend/dist",
  "framework": "vite"
}
```

**Recommended:** leave **Root Directory** empty in Vercel so this root `vercel.json` is used.

**Alternative:** set **Root Directory** to `frontend` — Vercel will use `frontend/vercel.json` instead (same build commands, paths relative to `frontend/`).

---

## Step 1 — Connect Git repository

Open: [https://vercel.com/liyandah/htd-roll-book](https://vercel.com/liyandah/htd-roll-book)

1. On the project overview, complete **Connect Git Repository** (or go to **Settings → Git**).
2. Choose **GitHub** and authorize Vercel if prompted.
3. Select repository: **`liyandah/HTD-RollBook`**.
4. Confirm **Production Branch**: **`main`**.

If the repo is not listed, install the Vercel GitHub app for your account/org and grant access to **HTD-RollBook**.

---

## Step 2 — Build & output settings

Go to **Settings → General** (or review settings during the first import).

| Setting | Recommended value |
|---------|-------------------|
| **Framework Preset** | Vite |
| **Root Directory** | *(empty — repo root)* |
| **Build Command** | *(from `vercel.json`)* `cd frontend && npm run build:prod` |
| **Output Directory** | *(from `vercel.json`)* `frontend/dist` |
| **Install Command** | *(from `vercel.json`)* `cd frontend && npm ci` |

If you set **Root Directory** to `frontend`, Vercel auto-detects Vite and uses `frontend/vercel.json`:

| Setting | Value (Root Directory = `frontend`) |
|---------|-------------------------------------|
| Build Command | `npm run build:prod` |
| Output Directory | `dist` |
| Install Command | `npm ci` |

Do **not** override these unless you know you need a custom pipeline.

---

## Step 3 — Environment variables

Go to **Settings → Environment Variables**.

Add:

| Name | Value | Environments |
|------|-------|--------------|
| `VITE_API_BASE_URL` | `https://api.htdrollbook.com` | **Production**, **Preview** (recommended) |

Notes:

- No trailing slash; do **not** append `/api` (routes already use `/api/...`).
- Vite embeds this at **build time** — change the variable, then **Redeploy**.
- For local dev, copy `frontend/env.example` → `frontend/.env` (see [CLOUDFLARE-SETUP.md](./CLOUDFLARE-SETUP.md)).

Optional: leave **Development** empty if you only deploy from Git; local `.env` handles dev.

---

## Step 4 — Trigger first production deployment

1. Go to **Deployments**.
2. After Git is connected, push to `main` or click **Redeploy** / **Deploy**.
3. Wait until the build succeeds. You should see a **Production** deployment (fixes “No Production Deployment” on the checklist).

Vercel assigns a default URL such as:

- `https://htd-roll-book.vercel.app`
- or `https://htd-roll-book-<team>.vercel.app`

That URL works immediately and is useful for smoke tests before custom DNS is live.

---

## Step 5 — Add custom domains

Go to **Settings → Domains → Add**.

Add both:

1. `htdrollbook.com` (apex)
2. `www.htdrollbook.com`

For each domain, Vercel shows required DNS records and verification status.

### DNS records (Cloudflare)

In **Cloudflare → htdrollbook.com → DNS → Records**, you should already have:

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| **A** | `@` | `76.76.21.21` | **DNS only** (grey cloud) recommended for first setup |
| **CNAME** | `www` | `cname.vercel-dns.com` | **DNS only** recommended |

**Is `76.76.21.21` correct?** Yes — this is Vercel’s standard apex A record IP. After you add domains in the Vercel dashboard, open **Settings → Domains**, click each domain, and compare the shown A/CNAME values. If Vercel displays a **project-specific** IP or CNAME (e.g. `cname.vercel-dns-0.com`), **use Vercel’s values** and update Cloudflare to match.

Tips:

- Use **DNS only** (grey cloud) on `@` and `www` until Vercel shows **Valid Configuration** and SSL is issued.
- In Vercel, use **Refresh** (⋯ menu on the domain) if verification stays pending after DNS changes.
- Optionally redirect `www` → apex: in **Domains**, set `htdrollbook.com` as primary and enable redirect from `www.htdrollbook.com`.

The **`api`** record (`A` → `187.77.99.225`, proxied) is for the backend only — do not point it at Vercel.

---

## Default `*.vercel.app` vs custom domain

| URL | Purpose |
|-----|---------|
| `https://htd-roll-book.vercel.app` (or similar) | Default Vercel hostname; always available after first deploy; good for previews and testing |
| `https://htdrollbook.com` | Production custom domain (what users should use) |
| `https://www.htdrollbook.com` | Alias; redirect to apex if configured |

**Which should you use?**

- **Production users:** `https://htdrollbook.com` (and optionally `www` → apex redirect).
- **Testing before DNS:** the `*.vercel.app` URL.
- **Preview deployments:** each PR gets its own `*.vercel.app` URL; backend CORS already allows `https://*.vercel.app` (see [CLOUDFLARE-SETUP.md](./CLOUDFLARE-SETUP.md)).

API calls from the frontend always go to `https://api.htdrollbook.com` when `VITE_API_BASE_URL` is set — regardless of whether the site is opened via `vercel.app` or `htdrollbook.com`.

---

## Step 6 — Verify

### Vercel checklist

On the project overview, confirm:

- [x] Connect Git Repository
- [x] Add Custom Domain
- [x] Production deployment on `main`

### Commands

```bash
# Default Vercel URL (replace with your actual *.vercel.app host)
curl -sI https://htd-roll-book.vercel.app

# Custom domain (after DNS + SSL)
curl -sI https://htdrollbook.com
curl -sI https://www.htdrollbook.com
```

### Browser

1. Open `https://htdrollbook.com`.
2. DevTools → **Network**: API requests should target `https://api.htdrollbook.com/api/...`.
3. No CORS errors (backend allows `https://htdrollbook.com` and `https://*.vercel.app`).

---

## Troubleshooting

| Symptom | Action |
|---------|--------|
| “No Production Deployment” | Connect Git; push to `main`; check **Deployments** for failed builds |
| Build fails on `npm ci` | Ensure `frontend/package-lock.json` is committed |
| API calls hit wrong host | Set `VITE_API_BASE_URL` in Vercel; **Redeploy** |
| Domain “Invalid Configuration” | Match Cloudflare records to **Settings → Domains** in Vercel; grey-cloud proxy during setup |
| SSL stuck on “Pending” | Use DNS only on `@`/`www`; **Refresh** domain in Vercel; wait for propagation |
| SPA routes 404 on refresh | Root `vercel.json` includes SPA rewrites — ensure Root Directory is empty or use `frontend/vercel.json` |

---

## Optional — Vercel CLI (local machine)

CLI was not available in the initial setup environment. To link locally later:

```bash
npm i -g vercel
vercel login
cd /path/to/HTD-RollBook
vercel link   # select team liyandah, project htd-roll-book
vercel env pull frontend/.env.local   # optional: pull env for local testing
```

Production deploys should still flow from **Git push → `main` → Vercel**.

---

## Quick reference

| What | Value |
|------|-------|
| Vercel project URL | https://vercel.com/liyandah/htd-roll-book |
| Repo | `liyandah/HTD-RollBook`, branch `main` |
| Root Directory | *(empty)* recommended |
| Env var | `VITE_API_BASE_URL=https://api.htdrollbook.com` |
| Domains | `htdrollbook.com`, `www.htdrollbook.com` |
| Cloudflare apex A | `76.76.21.21` (confirm in Vercel Domains tab) |
| Cloudflare www CNAME | `cname.vercel-dns.com` |
