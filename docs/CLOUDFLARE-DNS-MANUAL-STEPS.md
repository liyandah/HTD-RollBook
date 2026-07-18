# HTD RollBook — Cloudflare DNS (manual setup)

Use this guide when no Cloudflare API token is available in the project or shell environment. Add DNS records manually in the Cloudflare dashboard.

**Account email:** `liyandahhella12@gmail.com`  
**Zone (domain):** `htdrollbook.com`

> Do **not** share or commit Cloudflare passwords or API tokens. Create an API token only if you want automated DNS updates later (see [Step 0 — Give authorization for automation](#step-0--give-authorization-for-automation) or [Optional: API token for automation](#optional-api-token-for-automation)).

---

## Step 0. Add domain (empty Domains overview)

Use this section first if your Cloudflare **Account Home** or **Domains** overview shows **“No domains or subdomains found”** (no websites listed).

### 0.1 Add the site to Cloudflare

1. On the **Domains** / **Account Home** page, click **Add domain** (top right)
2. Enter domain: **htdrollbook.com**
3. Choose how to proceed:

   **If you do NOT own `htdrollbook.com` yet:**

   - Click **Buy domain** (or register **htdrollbook.com** at your preferred registrar first)
   - Complete purchase/registration, then return to Cloudflare and add the domain

   **If you DO own `htdrollbook.com` elsewhere (GoDaddy, Namecheap, etc.):**

   - Click **Add site** / continue with **htdrollbook.com**
   - Select a plan (Free is fine for this project)
   - Cloudflare will scan existing DNS records (you can review later)
   - On the **nameservers** step, copy the two Cloudflare nameservers shown (for example `ada.ns.cloudflare.com` and `bob.ns.cloudflare.com` — yours will differ)
   - Log in to your **registrar** (GoDaddy, Namecheap, etc.) → domain **htdrollbook.com** → **DNS** or **Nameservers**
   - Replace the current nameservers with the two Cloudflare nameservers
   - Save at the registrar

4. Back in Cloudflare, wait until **htdrollbook.com** appears under **Websites** / **Domains** with status **Active**
   - Pending status is normal until nameserver changes propagate (often minutes, sometimes up to 24–48 hours)
   - Do not add the DNS records below until the zone is **Active**

5. Once **Active**, continue with [§1 Log in to Cloudflare](#1-log-in-to-cloudflare) (confirm the domain is listed), then [§2 Open DNS settings](#2-open-dns-settings) and [§3 Add DNS records](#3-add-dns-records) for `@`, `www`, and `api`.

### Step 0 — Give authorization for automation

If a developer or agent will configure DNS for you, create a **Cloudflare API token** (never commit it to git):

1. Cloudflare dashboard → **My Profile** (top right avatar) → **API Tokens**
2. Click **Create Token**
3. Use template **Edit zone DNS** (or create a custom token with):
   - **Permissions:** Zone → DNS → Edit; Zone → Zone → Read
   - **Zone Resources:** Include → Specific zone → **htdrollbook.com**
4. Click **Continue to summary** → **Create Token**
5. **Copy the token immediately** (shown only once) and send it securely to your developer — for example in a password manager share or private message, **not** in email/chat logs if avoidable

**Token format to paste (example — yours will differ):**

```text
CLOUDFLARE_API_TOKEN=AbCdEf1234567890_your_actual_token_from_cloudflare
```

Or paste only the token value if asked:

```text
AbCdEf1234567890_your_actual_token_from_cloudflare
```

The developer sets it locally (never in the repo):

```powershell
$env:CLOUDFLARE_API_TOKEN = "AbCdEf1234567890_your_actual_token_from_cloudflare"
```

---

## 1. Log in to Cloudflare

1. Open [https://dash.cloudflare.com/](https://dash.cloudflare.com/)
2. Click **Log in**
3. Enter email: **liyandahhella12@gmail.com**
4. Enter your Cloudflare password (or use email magic link / SSO if configured)
5. Complete any two-factor authentication if prompted
6. On the **Account Home** page, confirm **htdrollbook.com** appears under **Websites**
   - If the domain is missing, complete [Step 0. Add domain](#step-0-add-domain-empty-domains-overview) first
   - Status should be **Active** before relying on public DNS

---

## 2. Open DNS settings

1. Click **htdrollbook.com** on the account home page
2. In the left sidebar, click **DNS**
3. Click **Records**
4. Review existing records — delete or edit duplicates that conflict with the table below (same **Type** + **Name**)

---

## 3. Add DNS records

For each row, click **Add record**, fill in the fields, then click **Save**.

Cloudflare **TTL** for proxied records is always **Auto**. For DNS-only (grey cloud) records, **Auto** is recommended unless you have a specific reason to set a fixed TTL.

### Record 1 — Apex frontend (Vercel)

| Field | Value |
|-------|--------|
| **Type** | `A` |
| **Name** | `@` (represents `htdrollbook.com`) |
| **IPv4 address (Content)** | `76.76.21.21` |
| **Proxy status** | **DNS only** (grey cloud) — recommended for Vercel |
| **TTL** | Auto |

**Steps:**

1. **Add record**
2. Type: **A**
3. Name: **@**
4. IPv4 address: **76.76.21.21**
5. Click the orange cloud icon so it turns **grey** (DNS only)
6. TTL: **Auto**
7. **Save**

> After you add `htdrollbook.com` in the Vercel dashboard, Vercel may show a different A/CNAME target. If Vercel’s instructions differ, **use Vercel’s values** instead of `76.76.21.21`.

---

### Record 2 — WWW frontend (Vercel)

| Field | Value |
|-------|--------|
| **Type** | `CNAME` |
| **Name** | `www` |
| **Target (Content)** | `cname.vercel-dns.com` |
| **Proxy status** | **DNS only** (grey cloud) — recommended |
| **TTL** | Auto |

**Steps:**

1. **Add record**
2. Type: **CNAME**
3. Name: **www**
4. Target: **cname.vercel-dns.com**
5. Proxy: **grey cloud** (DNS only)
6. TTL: **Auto**
7. **Save**

---

### Record 3 — Backend API

| Field | Value |
|-------|--------|
| **Type** | `A` |
| **Name** | `api` |
| **IPv4 address (Content)** | `187.77.99.225` |
| **Proxy status** | **Proxied** (orange cloud) — required |
| **TTL** | Auto |

**Steps:**

1. **Add record**
2. Type: **A**
3. Name: **api**
4. IPv4 address: **187.77.99.225**
5. Proxy: **orange cloud** (Proxied)
6. TTL: **Auto**
7. **Save**

This makes `https://api.htdrollbook.com` terminate TLS at Cloudflare and forward to nginx on the server (port 80 → Spring Boot on `127.0.0.1:8599`).

---

## 4. Summary table (all records)

| # | Type | Name | Content | Proxy | TTL |
|---|------|------|---------|-------|-----|
| 1 | A | `@` | `76.76.21.21` | DNS only (grey) | Auto |
| 2 | CNAME | `www` | `cname.vercel-dns.com` | DNS only (grey) | Auto |
| 3 | A | `api` | `187.77.99.225` | Proxied (orange) | Auto |

**Hostnames after setup:**

| Role | Hostname | Points to |
|------|----------|-----------|
| Frontend | `htdrollbook.com`, `www.htdrollbook.com` | Vercel |
| Backend API | `api.htdrollbook.com` | `187.77.99.225` (via Cloudflare proxy) |

---

## 5. SSL/TLS mode (required for API)

1. With **htdrollbook.com** selected, open **SSL/TLS** in the left sidebar
2. Click **Overview**
3. Under **Configure**, set **Encryption mode** to **Full**
   - Use **Full**, not **Full (strict)** — nginx on the server listens on port **80** only (no origin certificate yet)
   - **Flexible** is incorrect for this setup (can cause redirect/API issues)

---

## 6. Vercel custom domains (after DNS)

1. Open [Vercel Dashboard](https://vercel.com/dashboard) → **HTD-RollBook** project
2. **Settings → Domains → Add**
3. Add `htdrollbook.com` and `www.htdrollbook.com`
4. Complete Vercel verification if prompted
5. Set production domain to `htdrollbook.com` (optional: redirect `www` → apex)
6. Ensure **Environment Variables** include `VITE_API_BASE_URL=https://api.htdrollbook.com`
7. **Redeploy** production after env changes

See `docs/CLOUDFLARE-SETUP.md` for full frontend/backend verification commands.

---

## 7. Verify DNS (after propagation, usually minutes)

```bash
# API health via domain
curl -s https://api.htdrollbook.com/actuator/health

# Frontend headers
curl -sI https://htdrollbook.com
```

Expected API response: `{"status":"UP"}`

In a browser:

1. Open `https://htdrollbook.com`
2. DevTools → Network — API calls should go to `https://api.htdrollbook.com/api/...`
3. No CORS errors in the console

---

## Troubleshooting

| Symptom | What to check |
|---------|----------------|
| Domain not in account | Complete [Step 0. Add domain](#step-0-add-domain-empty-domains-overview) — register, buy, or add site + nameservers |
| Vercel “Invalid configuration” | Match DNS to values shown in Vercel → Domains |
| Cloudflare **521** / **522** on API | `systemctl status nginx` and `systemctl status htf-backend` on `187.77.99.225` |
| SSL errors on API | SSL/TLS mode must be **Full**, and `api` A record must be **Proxied** |
| CORS errors | Server CORS config — see `docs/CLOUDFLARE-SETUP.md` |

---

## Optional: API token for automation

If you want an agent or script to manage DNS via the Cloudflare API later:

1. Cloudflare dashboard → **My Profile** (top right) → **API Tokens**
2. **Create Token** → use template **Edit zone DNS** or custom permissions:
   - Zone → DNS → Edit
   - Zone → Zone → Read
   - Include zone: **htdrollbook.com**
3. Copy the token once (it is shown only at creation)
4. Set it locally (never commit to git):

   ```powershell
   $env:CLOUDFLARE_API_TOKEN = "your-token-here"
   ```

5. Re-run DNS automation; the token is **not** stored in this repository.

API endpoint for adding records:

```http
POST https://api.cloudflare.com/client/v4/zones/{zone_id}/dns_records
Authorization: Bearer {CLOUDFLARE_API_TOKEN}
```

---

## Related docs

- Full stack setup: `docs/CLOUDFLARE-SETUP.md`
- Server deployment: `docs/DEPLOYMENT-SERVER-187.77.99.225.md`
