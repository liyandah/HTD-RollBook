# HTD RollBook — Monitoring & Uptime

External uptime monitoring for the HTD RollBook stack (frontend on Vercel, backend on `187.77.99.225`).

**Credentials are not stored in this repo.** Admin login values live on the server in `/opt/htf-data-collection/htf-backend.env` (`ADMIN_USERNAME`, `ADMIN_PASSWORD`).

---

## Login URLs

| Environment | URL | Status (2026-07-19) |
|-------------|-----|---------------------|
| Vercel (primary) | https://htd-roll-book.vercel.app/login | **Up** (HTTP 200) |
| Custom domain | https://htdrollbook.cybercothtechnetworks.co.zw/login | **Up** (HTTP 200) |

After login, users land on `/dashboard` (registration overview — see [Internal app dashboard](#internal-app-dashboard-not-infrastructure-monitoring)).

**Where to read admin credentials:** SSH to the VM and run:

```bash
grep -E '^ADMIN_(USERNAME|PASSWORD)=' /opt/htf-data-collection/htf-backend.env
```

---

## Endpoints to monitor

Add these to UptimeRobot, Better Stack, Pingdom, or Cloudflare Observability.

### Backend API (Spring Boot Actuator)

| Monitor name | URL | Method | Expected | Notes |
|--------------|-----|--------|----------|-------|
| HTD API — direct (VM) | `http://187.77.99.225:8599/actuator/health` | GET | HTTP **200**, body contains `"status":"UP"` | Primary backend health check |
| HTD API — nginx subdomain | `http://htdrollbook-api.cybercothtechnetworks.co.zw/actuator/health` | GET | HTTP **200**, `"status":"UP"` | After DNS A record for `htdrollbook-api` is added |

**Do not monitor** `api.cybercothtechnetworks.co.zw` for HTD — that hostname is AngaPay production ([ANGAPAY-OFF-LIMITS.md](./ANGAPAY-OFF-LIMITS.md)).

Actuator is **public** (no JWT): `SecurityConfig` permits `/actuator/**`. Dependency: `spring-boot-starter-actuator` in `pom.xml`.

Optional deeper check (when DB matters): `GET /actuator/health` returns component status if `management.endpoint.health.show-details` is enabled; default response is sufficient for uptime.

### Frontend (Vercel / custom domain)

| Monitor name | URL | Method | Expected |
|--------------|-----|--------|----------|
| HTD Frontend — Vercel | `https://htd-roll-book.vercel.app/login` | GET | HTTP **200** |
| HTD Frontend — custom | `https://htdrollbook.cybercothtechnetworks.co.zw/login` | GET | HTTP **200** |

Optional: also watch the app root (`/` redirects to login or dashboard when authenticated).

### Smoke checks (manual)

```powershell
# Backend health (direct — recommended)
curl.exe -s http://187.77.99.225:8599/actuator/health

# Backend health (htdrollbook-api subdomain — after DNS)
curl.exe -s http://htdrollbook-api.cybercothtechnetworks.co.zw/actuator/health

# Frontends
curl.exe -s -o NUL -w "%{http_code}`n" https://htd-roll-book.vercel.app/login
curl.exe -s -o NUL -w "%{http_code}`n" https://htdrollbook.cybercothtechnetworks.co.zw/login
```

---

## UptimeRobot (recommended)

1. Sign in at [UptimeRobot](https://uptimerobot.com/) (free tier supports 50 monitors).
2. **Add New Monitor** for each URL above:
   - **Monitor Type:** HTTP(s)
   - **URL:** one of the endpoints in the table
   - **Monitoring Interval:** 5 minutes (free) or 1 minute (paid)
   - **Keyword monitoring (optional):** for health URLs, keyword `UP` in response body
3. **Alert contacts:** add email/SMS/Slack for downtime.
4. Suggested monitor set (minimum):
   - `http://187.77.99.225:8599/actuator/health`
   - `https://htd-roll-book.vercel.app/login`
   - `https://htdrollbook.cybercothtechnetworks.co.zw/login`
   - `http://htdrollbook-api.cybercothtechnetworks.co.zw/actuator/health` (optional, after DNS)

---

## Cloudflare Observability (manual)

Cloudflare **Health Checks** apply to zones you manage in Cloudflare. The `cybercothtechnetworks.co.zw` zone must be **Active** before API monitors work through the public hostname.

### Prerequisites

- Zone added and nameservers updated per [CLOUDFLARE-cybercothtechnetworks.md](./CLOUDFLARE-cybercothtechnetworks.md)
- DNS: `htdrollbook-api` → A `187.77.99.225`, `htdrollbook` → Vercel CNAME (DNS only)
- **Do not** point HTD monitors at `api.cybercothtechnetworks.co.zw` (AngaPay)

### Add a health check (dashboard)

1. [Cloudflare Dashboard](https://dash.cloudflare.com/) → select **cybercothtechnetworks.co.zw**
2. **Traffic** → **Health Checks** → **Create**
3. Configure:
   - **Name:** `HTD RollBook API`
   - **Address:** `htdrollbook-api.cybercothtechnetworks.co.zw`
   - **Path:** `/actuator/health`
   - **Port:** 443 (HTTPS)
   - **Interval:** 60s (or default)
   - **Retries / timeout:** defaults are fine
   - **Expected codes:** 200
   - **Response body:** optional match on `"status":"UP"`
4. Attach notifications (email, webhook, PagerDuty) under **Notifications**.

**Note:** Vercel frontend (`htdrollbook.*`) is DNS-only on Cloudflare; use UptimeRobot for the frontend or Cloudflare **Synthetic monitoring** if on a paid plan.

### API automation (optional)

Requires a token with `Zone:Read` and health-check permissions. **Do not commit tokens.**

```powershell
$env:CLOUDFLARE_API_TOKEN = "<your-token>"
# Verify token
curl.exe -s "https://api.cloudflare.com/client/v4/user/tokens/verify" `
  -H "Authorization: Bearer $env:CLOUDFLARE_API_TOKEN"
```

Health check creation via API uses `POST /client/v4/zones/{zone_id}/healthchecks`. See [Cloudflare Health Checks API](https://developers.cloudflare.com/api/resources/healthchecks/methods/create/).

---

## Internal app dashboard (not infrastructure monitoring)

The in-app **Dashboard** (`frontend/src/pages/Dashboard.jsx`, route `/dashboard`) is for **registration and verification workflow**, not server uptime.

It shows:

- **Registration status:** total registered, pending verification, verified, declined
- **Department breakdown:** Home League, Men's Fellowship, seniors, youth, junior soldiers, cradle roll
- **Recent enrollments** table with filters
- **Verification queue** (pending approvals)

Data sources:

- `GET /api/records/dashboard` → `DashboardStatsResponse` (totals, verified/declined counts)
- `GET /api/records` (paginated records for tables)

Requires JWT login via `POST /api/auth/login` — this is separate from `/actuator/health`.

---

## Related docs

- [DEPLOYMENT-SERVER-187.77.99.225.md](./DEPLOYMENT-SERVER-187.77.99.225.md) — VM layout, ports, systemd
- [CLOUDFLARE-cybercothtechnetworks.md](./CLOUDFLARE-cybercothtechnetworks.md) — DNS for `htdrollbook` + `api` subdomains
- [VERCEL-SETUP.md](./VERCEL-SETUP.md) — frontend deployment
