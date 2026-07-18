# Remote server deployment notes (187.77.99.225)
# Do NOT put passwords in this file.

## Method
- Built locally: `mvn clean package -DskipTests`
- Deployed via Paramiko SFTP + systemd (JAR + env file), aligned with `docs/DEPLOYMENT-LINUX.md`
- Docker Compose in repo is DB-only; server uses system PostgreSQL

## Backend
- **URL:** http://187.77.99.225:8599
- **Health:** http://187.77.99.225:8599/actuator/health
- **Swagger UI:** http://187.77.99.225:8599/swagger-ui.html
- **API docs:** http://187.77.99.225:8599/api-docs
- **Service:** `htf-backend.service` (enabled, restart on failure)
- **Paths:** `/opt/htf-data-collection/htf-data-collection-0.0.1.jar`, logs `/opt/htf-data-collection/backend.log`
- **Secrets:** `/opt/htf-data-collection/htf-backend.env` (mode 600) — DB password, JWT secret, admin password

## Database
- PostgreSQL 14+ on localhost:5432 (system service)
- Database: `salvation_army_db`
- App user: `htf_app` (password in env file only)
- Flyway migrations run on startup

## Firewall
- UFW allows TCP **8599** (`htf-backend`)

## Redeploy (from dev machine)
1. `mvn clean package -DskipTests` — confirm JAR ~64MB (fat jar with BOOT-INF)
2. Set env: `DEPLOY_SSH_HOST`, `DEPLOY_SSH_USER`, `DEPLOY_SSH_PASSWORD`
3. Run `python scripts/upload_jar_once.py` (upload + restart; does not rotate DB secrets)

## Manual follow-ups
- Rotate root SSH password after deployment
- Read admin login from `/opt/htf-data-collection/htf-backend.env` on the server
- Point frontend `VITE_API_BASE_URL` to `https://api.htdrollbook.com` (see `docs/CLOUDFLARE-SETUP.md`)
- Production CORS and nginx for `api.htdrollbook.com` are configured via `scripts/setup_cloudflare_server.py`
- Set WhatsApp/Meta tokens and mail settings in the env file for production
- Optional: nginx reverse proxy on 443 instead of exposing 8599
