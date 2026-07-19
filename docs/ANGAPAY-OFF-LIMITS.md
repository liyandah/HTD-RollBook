# AngaPay production — off limits for HTD RollBook

**Do not modify, stop, restart, or reconfigure any AngaPay service or nginx vhost on `187.77.99.225`.**

## Why

`api.cybercothtechnetworks.co.zw` is **AngaPay production**, not HTD RollBook. A mistaken HTD nginx vhost on that hostname hijacked HTTP traffic to Spring Boot on port **8599**, while HTTPS still served the AngaPay SPA — breaking AngaPay login for users hitting that URL.

## AngaPay vs HTD RollBook on `187.77.99.225`

| Service | Hostname(s) | Port / upstream | Notes |
|---------|-------------|-----------------|-------|
| **AngaPay SPA** | `angwapayadmin.com`, `api.cybercothtechnetworks.co.zw` (HTTPS + HTTP after fix), IP default | nginx **443** / **80** → `/var/www/angwapay` | Production — **do not touch** |
| **AngaPay API** | `api.angwapayadmin.com` | nginx **443** → `127.0.0.1:8083` | Java backend — **do not touch** |
| **AngaPay monitor** | `monitor.angwapayadmin.com` | nginx **80** / **443** | **do not touch** |
| **HTD RollBook API** | `htdrollbook-api.cybercothtechnetworks.co.zw` (optional), `api.htdrollbook.com`, direct `187.77.99.225:8599` | Spring Boot **8599** (`htf-backend.service`) | HTD only |
| **HTD RollBook frontend** | `htdrollbook.cybercothtechnetworks.co.zw`, `htd-roll-book.vercel.app` | Vercel | Proxies `/api/*` to `:8599` |

## Off-limits paths and units

- `/etc/nginx/sites-enabled/angwapay*`
- `/var/www/angwapay`
- `/opt/angwapay`
- `/opt/angwapay-monitor`
- Any Java process on port **8083**
- **`api.cybercothtechnetworks.co.zw`** — reserved for AngaPay; never add HTD `server_name` or DNS docs pointing HTD at this host

## HTD-safe changes only

- `htf-backend.service` and `/opt/htf-data-collection/htf-backend.env` (CORS)
- `/etc/nginx/sites-available/htdrollbook-api.cybercothtechnetworks.co.zw`
- `/etc/nginx/sites-available/api.htdrollbook.com`
- Port **8599** (HTD Spring Boot)

## Correct HTD login URL

**https://htdrollbook.cybercothtechnetworks.co.zw/login**

Alternative: **https://htd-roll-book.vercel.app/login**

Do **not** use `api.cybercothtechnetworks.co.zw` for HTD — that is AngaPay.

## DNS (HTD only — new subdomain)

Add in NivaCity / registrar DNS (do **not** change the existing `api` A record if AngaPay uses it):

| Type | Name | Value |
|------|------|-------|
| **A** | `htdrollbook-api` | `187.77.99.225` |

Optional later: point Vercel `vercel.json` rewrite at `http://htdrollbook-api.cybercothtechnetworks.co.zw/api/:path*` instead of direct `:8599`.
