# HTD RollBook — Cloudflare for cybercothtechnetworks.co.zw

Move DNS for **`cybercothtechnetworks.co.zw`** from NivaCity (`ns1–ns4.mydata.city`) to Cloudflare, then point the HTD RollBook frontend (Vercel) and API (VM) at the subdomains below.

| Role | Hostname | Target | Cloudflare proxy |
|------|----------|--------|------------------|
| Frontend (Vercel) | `htdrollbook.cybercothtechnetworks.co.zw` | `6897f6315804af9b.vercel-dns-017.com` (CNAME) | **DNS only** (grey cloud) — required for Vercel SSL |
| Backend API | `api.cybercothtechnetworks.co.zw` | `187.77.99.225` (A) | **Proxied** (orange cloud) — Cloudflare SSL to visitors |

**Related docs:** [SUBDOMAIN-SETUP-cybercothtechnetworks.md](./SUBDOMAIN-SETUP-cybercothtechnetworks.md) (NivaCity-only path), [CLOUDFLARE-SETUP.md](./CLOUDFLARE-SETUP.md) (`htdrollbook.com`).

---

## API check (2026-07-19)

| Item | Result |
|------|--------|
| Zone `cybercothtechnetworks.co.zw` in Cloudflare | **Not present** — must be added in the dashboard |
| Cloudflare account | `liyandahhella12@gmail.com` (account id `27337e34b88764b3bb14c8cce1a4c92c`) |
| API token | Valid (`/user/tokens/verify` success) |
| Token scope (observed) | `#zone:read`, `#dns_records:read`, `#dns_records:edit` — **cannot** create/delete zones via API |
| Stray zone `htd-roll-book.vercel.app` | Still listed (**pending**, activation failure `unresolvable`). **Delete manually** in dashboard (API delete returned **9109 Unauthorized**) |
| DNS records created via API | **None** — blocked until the `.co.zw` zone exists |

---

## Step 1 — Add the domain in Cloudflare (required)

1. Sign in: [Cloudflare Dashboard](https://dash.cloudflare.com/) as **liyandahhella12@gmail.com**.
2. **Add a site** → enter **`cybercothtechnetworks.co.zw`** → choose **Free** plan.
3. Cloudflare scans existing DNS (optional). On the nameserver step, copy the **two assigned nameservers** (example shape: `xxxx.ns.cloudflare.com`, `yyyy.ns.cloudflare.com`). **Use the exact pair shown in your dashboard**, not a guess from another zone.
4. **Do not** change nameservers at NivaCity until you have copied Cloudflare’s pair from this step.

### Remove mistaken zone (recommended)

**Websites** → **`htd-roll-book.vercel.app`** → **Manage** → **Remove site from Cloudflare**.  
You cannot delegate `*.vercel.app` at Vercel to Cloudflare; this zone will never activate.

---

## Step 2 — Update nameservers at NivaCity

1. Log in to **NivaCity** → domain **`cybercothtechnetworks.co.zw`** → **Nameservers** (or registrar DNS settings).
2. Replace current nameservers:

   | Current (NivaCity) |
   |--------------------|
   | `ns1.mydata.city` |
   | `ns2.mydata.city` |
   | `ns3.mydata.city` |
   | `ns4.mydata.city` |

   with the **two Cloudflare nameservers** from Step 1.

3. Save. Propagation often takes **15 minutes to 48 hours** (usually under a few hours).
4. In Cloudflare, wait until the zone status is **Active** (not “Pending nameserver update”).

Verify (optional):

```bash
nslookup -type=NS cybercothtechnetworks.co.zw
```

---

## Step 3 — DNS records in Cloudflare

After the zone is **Active**, add records in **DNS → Records** (or use the API below).

| Type | Name | Content | Proxy status | TTL |
|------|------|---------|--------------|-----|
| **CNAME** | `htdrollbook` | `6897f6315804af9b.vercel-dns-017.com` | **DNS only** (grey cloud) | Auto |
| **A** | `api` | `187.77.99.225` | **Proxied** (orange cloud) | Auto |

**Notes**

- Grey cloud on the Vercel CNAME lets Vercel issue and renew the frontend certificate.
- Orange cloud on `api` terminates HTTPS at Cloudflare; origin nginx on the VM stays on **port 80** (see server section).
- If Vercel shows a different CNAME target after you add the custom domain, **prefer Vercel’s value** and update this record to match.

### Create records via API (after zone exists)

Set a token in your shell (**do not commit** the token):

```powershell
$env:CLOUDFLARE_API_TOKEN = "<your-token>"
```

List the zone id:

```powershell
curl.exe -s "https://api.cloudflare.com/client/v4/zones?name=cybercothtechnetworks.co.zw" `
  -H "Authorization: Bearer $env:CLOUDFLARE_API_TOKEN"
```

Create CNAME (replace `ZONE_ID`):

```powershell
curl.exe -s -X POST "https://api.cloudflare.com/client/v4/zones/ZONE_ID/dns_records" `
  -H "Authorization: Bearer $env:CLOUDFLARE_API_TOKEN" `
  -H "Content-Type: application/json" `
  -d "{\"type\":\"CNAME\",\"name\":\"htdrollbook\",\"content\":\"6897f6315804af9b.vercel-dns-017.com\",\"proxied\":false,\"ttl\":1}"
```

Create A record for API:

```powershell
curl.exe -s -X POST "https://api.cloudflare.com/client/v4/zones/ZONE_ID/dns_records" `
  -H "Authorization: Bearer $env:CLOUDFLARE_API_TOKEN" `
  -H "Content-Type: application/json" `
  -d "{\"type\":\"A\",\"name\":\"api\",\"content\":\"187.77.99.225\",\"proxied\":true,\"ttl\":1}"
```

---

## Step 4 — Vercel custom domain

1. [htd-roll-book → Settings → Domains](https://vercel.com/liyandah/htd-roll-book/settings/domains)
2. Add **`htdrollbook.cybercothtechnetworks.co.zw`**
3. Wait for **Valid Configuration** and **SSL: Active**
4. Production API URL for builds (if not using same-origin `/api` rewrites only):

   | Variable | Value |
   |----------|-------|
   | `VITE_API_BASE_URL` | `https://api.cybercothtechnetworks.co.zw` |

Redeploy after DNS and env changes.

---

## Step 5 — SSL / Cloudflare settings (API hostname)

For **`api.cybercothtechnetworks.co.zw`** with orange cloud:

1. **SSL/TLS** → Overview → **Flexible** or **Full**:
   - **Flexible**: Cloudflare → origin over HTTP (matches current nginx on `:80` only).
   - **Full**: Origin must accept HTTPS or Cloudflare may show errors until you add a cert on the VM.
2. Start with **Flexible** if nginx has no Let’s Encrypt cert yet; move to **Full (strict)** after certbot on the server.

---

## Server verification (187.77.99.225, 2026-07-19)

Checked over SSH:

| Check | Status |
|-------|--------|
| `nginx` | **active** |
| `htf-backend` | **active** |
| Vhost `/etc/nginx/sites-enabled/api.cybercothtechnetworks.co.zw` | **`server_name api.cybercothtechnetworks.co.zw`** → `proxy_pass http://127.0.0.1:8599` |
| Health via nginx `Host: api.cybercothtechnetworks.co.zw` | **HTTP 200** (`/actuator/health`) |
| `CORS_ALLOWED_ORIGINS` in `/opt/htf-data-collection/htf-backend.env` | Includes `https://htdrollbook.cybercothtechnetworks.co.zw` and `https://rollbook.cybercothtechnetworks.co.zw` |

No nginx/CORS changes required for these subdomains unless you add new frontend hostnames.

---

## Spelling reminder

Registered zone: **`cybercothtechnetworks.co.zw`** (**cyber** + **coth** + **technetworks**).  
Do not use `cyberoottechnetworks` in DNS, Vercel, or env vars.

---

## Troubleshooting

| Symptom | Action |
|---------|--------|
| Zone stuck pending | Confirm NivaCity nameservers match Cloudflare exactly; wait for NS propagation |
| Vercel SSL pending | CNAME must be **grey cloud**; target must match Vercel domain page |
| Cloudflare **521/522** on API | `systemctl status nginx htf-backend` on VM; SSL mode **Flexible** if origin is HTTP-only |
| CORS errors | Ensure frontend origin is in `CORS_ALLOWED_ORIGINS`; `systemctl restart htf-backend` after edits |
| API token cannot add zone | Create zone in dashboard; use a token with **Zone → Edit** only if automating zone creation |

---

## Security

- Store `CLOUDFLARE_API_TOKEN` in a password manager or CI secret — **never** commit it to git.
- Rotate the token if it was pasted into chat or logs.
