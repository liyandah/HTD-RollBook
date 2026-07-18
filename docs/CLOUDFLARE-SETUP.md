# HTD RollBook — Cloudflare, Vercel & API setup

Connect the **HTD-RollBook** frontend (Vercel) and backend (187.77.99.225) using custom domains on Cloudflare.

| Role | Hostname | Points to |
|------|----------|-----------|
| Frontend | `htdrollbook.com`, `www.htdrollbook.com` | Vercel |
| Backend API | `api.htdrollbook.com` | `187.77.99.225` (nginx → `:8599`) |

---

## Prerequisites

### 1. Register the domain (if not already owned)

If **`htdrollbook.com`** is not registered:

1. Sign in to [Cloudflare](https://dash.cloudflare.com/)
2. **Domain Registration** → register `htdrollbook.com`, **or** transfer an existing domain to Cloudflare
3. Wait until the domain shows **Active** in Cloudflare DNS

You cannot create public DNS records until the domain exists in your Cloudflare account.

### 2. GitHub & Vercel

- Repo: [github.com/liyandah/HTD-RollBook](https://github.com/liyandah/HTD-RollBook)
- Vercel project should deploy from this repo (root `vercel.json` builds `frontend/`)

---

## Frontend environment

### Local / example files

Copy `frontend/env.example` to `frontend/.env` for local development.

```env
# Production API origin (no trailing slash, no /api suffix)
VITE_API_BASE_URL=https://api.htdrollbook.com
```

Leave empty for local dev so Vite proxies API calls to the backend.

### How the frontend uses it

`frontend/src/api/apiClient.js` reads `VITE_API_BASE_URL` at **build time**:

- Strips a trailing slash
- Uses it as axios `baseURL`
- App routes already use paths like `/api/auth/login` — do **not** append `/api` to the env value

### Vercel environment variable

In **Vercel → Project → Settings → Environment Variables**:

| Name | Value | Environments |
|------|-------|--------------|
| `VITE_API_BASE_URL` | `https://api.htdrollbook.com` | Production, Preview (recommended) |

Redeploy after changing this variable (Vite bakes env vars into the build).

---

## Cloudflare DNS records

Add these in **Cloudflare → htdrollbook.com → DNS → Records**.

### Frontend (Vercel)

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| **A** | `@` | `76.76.21.21` | DNS only (grey cloud) *or* proxied per Vercel docs |
| **CNAME** | `www` | `cname.vercel-dns.com` | DNS only (grey cloud) recommended |

Vercel may show different targets when you add the domain in the Vercel dashboard — **use the values Vercel provides** if they differ from the table above.

### Backend API

| Type | Name | Content | Proxy |
|------|------|---------|-------|
| **A** | `api` | `187.77.99.225` | **Proxied** (orange cloud) |

Cloudflare terminates HTTPS for visitors; nginx on the server listens on port **80** and proxies to Spring Boot on **127.0.0.1:8599**.

### SSL/TLS mode (Cloudflare)

**SSL/TLS → Overview → Encryption mode:**

- **Full** — recommended with nginx on port 80 (HTTP origin, HTTPS to clients)
- **Full (strict)** — only if you install a Cloudflare Origin Certificate on nginx (port 443)

Current server setup uses nginx on **80** only for `api.htdrollbook.com`, so use **Full**.

---

## Vercel custom domain

1. Open [Vercel Dashboard](https://vercel.com/dashboard) → your **HTD-RollBook** project
2. **Settings → Domains → Add**
3. Add:
   - `htdrollbook.com`
   - `www.htdrollbook.com`
4. Follow Vercel’s verification steps (DNS records or nameserver change)
5. Set **Production** domain to `htdrollbook.com` (redirect `www` → apex if desired)
6. Confirm **Environment Variables** include `VITE_API_BASE_URL=https://api.htdrollbook.com`
7. **Deployments → Redeploy** the latest production build

---

## Backend (already configured on server)

Server: `root@187.77.99.225`

### CORS

File: `/opt/htf-data-collection/htf-backend.env`

```env
CORS_ALLOWED_ORIGINS=http://187.77.99.225,http://187.77.99.225:8599,http://localhost:5173,https://htdrollbook.com,https://www.htdrollbook.com,https://*.vercel.app
```

Restart after changes:

```bash
systemctl restart htf-backend
```

The Spring `CorsConfig` treats these as origin **patterns**, so `https://*.vercel.app` covers Vercel preview deployments.

### Nginx reverse proxy

File: `/etc/nginx/sites-available/api.htdrollbook.com`

- `server_name api.htdrollbook.com`
- Listens on **80**
- `proxy_pass http://127.0.0.1:8599`
- Forwards `X-Forwarded-For`, `X-Forwarded-Proto`, `Host`

Enable / reload:

```bash
nginx -t && systemctl reload nginx
```

### Systemd service

```bash
systemctl status htf-backend
systemctl status nginx
```

### Re-run server setup from dev machine

```bash
export DEPLOY_SSH_HOST=187.77.99.225
export DEPLOY_SSH_USER=root
export DEPLOY_SSH_PASSWORD='your-ssh-password'
python scripts/setup_cloudflare_server.py
python scripts/verify_server.py
```

**Never commit SSH passwords or production secrets.**

---

## Verification

### After DNS propagates

Replace commands as needed if DNS is not live yet (use `-H 'Host: api.htdrollbook.com'` against the IP).

```bash
# API health (direct IP + Host header, before DNS)
curl -s -H "Host: api.htdrollbook.com" http://187.77.99.225/actuator/health

# API health (via Cloudflare + domain)
curl -s https://api.htdrollbook.com/actuator/health

# Swagger (optional)
curl -sI https://api.htdrollbook.com/swagger-ui.html

# Frontend
curl -sI https://htdrollbook.com
```

Expected API health response:

```json
{"status":"UP"}
```

### Browser checks

1. Open `https://htdrollbook.com`
2. DevTools → Network — login/API calls should go to `https://api.htdrollbook.com/api/...`
3. No CORS errors in the console

### CORS preflight (optional)

```bash
curl -sI -X OPTIONS "https://api.htdrollbook.com/api/auth/login" \
  -H "Origin: https://htdrollbook.com" \
  -H "Access-Control-Request-Method: POST"
```

Look for `Access-Control-Allow-Origin` matching your frontend origin.

---

## Troubleshooting

| Symptom | Check |
|---------|--------|
| `522` / `521` from Cloudflare | nginx running? `systemctl status nginx`. UFW allows 80/443? |
| CORS blocked | `CORS_ALLOWED_ORIGINS` in `htf-backend.env`; restart `htf-backend` |
| API calls go to wrong host | Vercel `VITE_API_BASE_URL` set? Redeploy after change |
| Login 404 `/api/api/...` | Env must be origin only (`https://api.htdrollbook.com`), not `.../api` |
| Domain not resolving | Domain registered? DNS records proxied correctly? Wait for propagation |

---

## Security notes

- Rotate the server root password after setup
- Keep `/opt/htf-data-collection/htf-backend.env` mode `600`
- Do not commit `.env`, passwords, JWT secrets, or DB credentials
- Prefer SSH keys over password auth for future server access
