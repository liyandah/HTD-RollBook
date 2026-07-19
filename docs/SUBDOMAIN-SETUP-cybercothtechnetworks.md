# HTD RollBook — Subdomains on cybercothtechnetworks.co.zw

Use **NivaCity / mydata.city** DNS (nameservers `ns1.mydata.city` … `ns4.mydata.city`). This guide does **not** use Cloudflare unless you migrate the zone later.

| Item | Value |
|------|-------|
| Registrar / DNS panel | NivaCity → **DNS Management** for `cybercothtechnetworks.co.zw` |
| Vercel project | [vercel.com/liyandah/htd-roll-book](https://vercel.com/liyandah/htd-roll-book) |
| Backend VM | `187.77.99.225` (Spring Boot `:8599`, nginx reverse proxy on `:80`) |
| Current Vercel URL | https://htd-roll-book.vercel.app |

---

## Common typo — read before adding domains

The registered domain is **`cybercothtechnetworks.co.zw`** (spelling: **cyber** + **COTH** + **technetworks**).

| Wrong (invalid) | Correct |
|-----------------|---------|
| `cyberoottechnetworks.co.zw` | `cybercothtechnetworks.co.zw` |
| `htdrollbook.cyberoottechnetworks.co.zw` | `htdrollbook.cybercothtechnetworks.co.zw` |
| `api.cyberoottechnetworks.co.zw` | `api.cybercothtechnetworks.co.zw` |

If Vercel shows **Invalid Configuration** or DNS never validates, search every panel (Vercel Domains, NivaCity DNS, nginx, CORS) for **`cyberoot`** and replace with **`cybercoth`**. The repo, nginx on `187.77.99.225`, and CORS already use the correct spelling only.

---

## Recommended subdomain names

> **AngaPay conflict:** `api.cybercothtechnetworks.co.zw` is **AngaPay production** — never use it for HTD. See [ANGAPAY-OFF-LIMITS.md](./ANGAPAY-OFF-LIMITS.md).

| Role | HTD hostname | Notes |
|------|--------------|-------|
| **Frontend (SPA)** | `htdrollbook.cybercothtechnetworks.co.zw` | Vercel CNAME |
| **Backend API** | `htdrollbook-api.cybercothtechnetworks.co.zw` | A → `187.77.99.225`, nginx → `:8599` |
| **Backend API (direct)** | `187.77.99.225:8599` | Used by Vercel rewrite today — no DNS needed |

Optional alias frontend: `rollbook.cybercothtechnetworks.co.zw`.

**Do not use** `api.cybercothtechnetworks.co.zw` for HTD — that hostname serves AngaPay.

---

## Step 1 — DNS records (NivaCity panel)

Log in to NivaCity → domain **cybercothtechnetworks.co.zw** → **DNS Management**.

Add these records (TTL **300** or **Auto** while testing; increase to **3600** when stable).

### Frontend → Vercel

**Option A — CNAME (preferred if NivaCity allows CNAME on a subdomain):**

| Type | Name / Host | Value / Points to | Notes |
|------|-------------|-------------------|-------|
| **CNAME** | `htdrollbook` | `cname.vercel-dns.com` | Vercel may show a project-specific target after you add the domain — **use Vercel’s value if it differs** |

Optional alias (same Vercel project):

| Type | Name / Host | Value / Points to |
|------|-------------|-------------------|
| **CNAME** | `rollbook` | `cname.vercel-dns.com` |

**Option B — A record (if CNAME is not supported for that host):**

| Type | Name / Host | Value | Notes |
|------|-------------|-------|-------|
| **A** | `htdrollbook` | `76.76.21.21` | Vercel anycast IP; confirm in Vercel **Domains** if they show a different A target |

### API → backend VM (HTD only)

| Type | Name / Host | Value | Proxy |
|------|-------------|-------|-------|
| **A** | `htdrollbook-api` | `187.77.99.225` | **DNS only** |

**Do not add or change** the existing `api` A record if AngaPay uses `api.cybercothtechnetworks.co.zw` for production.

After the A record propagates, optional Vercel rewrite target: `http://htdrollbook-api.cybercothtechnetworks.co.zw/api/:path*`. Current default: direct `http://187.77.99.225:8599/api/:path*`.

### Do not change nameservers

Keep NivaCity nameservers (`ns1-4.mydata.city`). Only add/edit **records** in DNS Management.

### Verify DNS (after a few minutes)

```bash
nslookup htdrollbook.cybercothtechnetworks.co.zw
nslookup htdrollbook-api.cybercothtechnetworks.co.zw
```

Frontend should resolve to Vercel; HTD API should resolve to `187.77.99.225`.

---

## Step 2 — Custom domain in Vercel

1. Open [htd-roll-book → Settings → Domains](https://vercel.com/liyandah/htd-roll-book/settings/domains).
2. Add **`htdrollbook.cybercothtechnetworks.co.zw`**.
3. Vercel shows required DNS (CNAME or A). Match those values in NivaCity if they differ from this doc.
4. Wait for **Valid Configuration** and **SSL: Active** (Vercel issues the certificate automatically).
5. Optional: add **`rollbook.cybercothtechnetworks.co.zw`** and set redirect to the primary hostname.
6. Optional: set **Production** domain to `htdrollbook.cybercothtechnetworks.co.zw` once SSL is green.

**Environment variables:** leave **`VITE_API_BASE_URL` unset**. The app uses same-origin `/api/...` via `vercel.json` rewrites.

Redeploy after DNS + domain are valid (push to `main` or **Redeploy** in Vercel).

---

## Step 3 — SSL

### Frontend

Vercel handles HTTPS for `htdrollbook.cybercothtechnetworks.co.zw`. No cert work on your VM for the SPA.

### Backend API (nginx on 187.77.99.225)

**Current state:** nginx listens on **port 80** for `htdrollbook-api.cybercothtechnetworks.co.zw` and proxies to Spring Boot on `127.0.0.1:8599`. Vercel uses direct `:8599` rewrites (no HTD nginx hostname required).

**HTTPS (optional, after `htdrollbook-api` DNS is live):**

```bash
certbot --nginx -d htdrollbook-api.cybercothtechnetworks.co.zw
nginx -t && systemctl reload nginx
```

Do **not** run certbot for `api.cybercothtechnetworks.co.zw` — that hostname is AngaPay.

---

## Step 4 — `vercel.json` API rewrite

Root `vercel.json` rewrites browser `/api/*` to the backend.

| Phase | Rewrite destination | When |
|-------|---------------------|------|
| **Current (default)** | `http://187.77.99.225:8599/api/:path*` | Works today; bypasses AngaPay `api` hostname |
| **After `htdrollbook-api` DNS** | `http://htdrollbook-api.cybercothtechnetworks.co.zw/api/:path*` | Optional; nginx vhost already on server |
| **After certbot on HTD API** | `https://htdrollbook-api.cybercothtechnetworks.co.zw/api/:path*` | After Let’s Encrypt on HTD subdomain only |

Never point rewrites at `api.cybercothtechnetworks.co.zw` — AngaPay production.

---

## Step 5 — Server configuration (already applied)

On **`187.77.99.225`**:

| Item | Path / value |
|------|----------------|
| nginx site | `/etc/nginx/sites-available/htdrollbook-api.cybercothtechnetworks.co.zw` |
| Enabled link | `/etc/nginx/sites-enabled/htdrollbook-api.cybercothtechnetworks.co.zw` |
| `server_name` | `htdrollbook-api.cybercothtechnetworks.co.zw` |
| Upstream | `http://127.0.0.1:8599` |
| CORS file | `/opt/htf-data-collection/htf-backend.env` |

**Removed (AngaPay conflict):** HTD must not use `/etc/nginx/sites-enabled/api.cybercothtechnetworks.co.zw`.

**CORS origins include:**

- `https://htdrollbook.cybercothtechnetworks.co.zw`
- `https://rollbook.cybercothtechnetworks.co.zw`
- `https://htd-roll-book.vercel.app`
- `https://*.vercel.app`
- Legacy `htdrollbook.com` hostnames and local/dev entries

Re-apply from a dev machine (password via env only):

```bash
export DEPLOY_SSH_PASSWORD='your-password'
python scripts/setup_cybercoth_subdomain_server.py
```

---

## Step 6 — Smoke tests

After DNS + Vercel domain + (optional) certbot:

```bash
curl -s https://api.cybercothtechnetworks.co.zw/actuator/health
# or before SSL:
curl -s http://api.cybercothtechnetworks.co.zw/actuator/health

curl -sI https://htdrollbook.cybercothtechnetworks.co.zw
```

In the browser:

1. Open `https://htdrollbook.cybercothtechnetworks.co.zw`
2. DevTools → Network — API calls should hit same-origin `/api/...` (proxied by Vercel)
3. No CORS errors in the console

---

## Optional — migrate DNS to Cloudflare later

1. Add site `cybercothtechnetworks.co.zw` in Cloudflare.
2. Replace nameservers at NivaCity with Cloudflare’s pair.
3. Recreate the same A/CNAME records in Cloudflare.
4. Enable proxy (orange cloud) on API only if you want Cloudflare DDoS/cache; use SSL **Full** with nginx on port 80, or **Full (strict)** with origin certificates.

Until then, **mydata.city / NivaCity DNS only** is sufficient.

---

## Related docs

- [VERCEL-SETUP.md](./VERCEL-SETUP.md)
- [DEPLOYMENT-SERVER-187.77.99.225.md](./DEPLOYMENT-SERVER-187.77.99.225.md)
- [CLOUDFLARE-SETUP.md](./CLOUDFLARE-SETUP.md) — only if you move the zone to Cloudflare
