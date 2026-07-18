# Deploying to a Linux Server

This guide covers deploying the **HTF Data collection** application (Spring Boot backend + React frontend) on a Linux server (Ubuntu/Debian or similar).

---

## 1. Server requirements

- **OS:** Linux (Ubuntu 20.04+ or Debian 11+ recommended)
- **Java:** OpenJDK 17 (for backend)
- **Node.js:** 18+ (only needed to **build** the frontend; optional on server if you build elsewhere)
- **PostgreSQL:** 14+ (or 15+)
- **Memory:** At least 1 GB RAM; 2 GB recommended for production
- **Disk:** Enough space for app, uploads, and PostgreSQL data

---

## 2. Install dependencies on the server

### 2.1 Java 17

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install -y openjdk-17-jdk

# Verify
java -version
# Should show openjdk version "17.x.x"
```

### 2.2 PostgreSQL

```bash
# Ubuntu/Debian
sudo apt install -y postgresql postgresql-contrib

# Start and enable
sudo systemctl start postgresql
sudo systemctl enable postgresql
```

Create database and user:

```bash
sudo -u postgres psql
```

In the PostgreSQL prompt:

```sql
CREATE USER your_db_user WITH PASSWORD 'your_secure_password';
CREATE DATABASE salvation_army_db OWNER your_db_user;
\q
```

(Use the same database name as in your `application.properties` or set it via `DATABASE_URL`.)

### 2.3 Node.js (for building the frontend on the server)

If you build the frontend on the same server:

```bash
# Using NodeSource for Node 18
curl -fsSL https://deb.nodesource.com/setup_18.x | sudo -E bash -
sudo apt install -y nodejs

# Verify
node -v
npm -v
```

---

## 3. Get the application on the server

### Option A: Clone from Git

```bash
cd /opt   # or your preferred directory
sudo git clone <your-repo-url> htf-data-collection
cd htf-data-collection
```

### Option B: Copy built artifacts from your machine

From your dev machine, build and copy:

- Backend: `mvn clean package -DskipTests` → copy `target/*.jar` and (if needed) `uploads/`
- Frontend: `cd frontend && npm ci && npm run build` → copy the whole `frontend/dist` folder

---

## 4. Build the application

### 4.1 Backend (Spring Boot)

```bash
cd /opt/htf-data-collection   # or your project path
./mvnw clean package -DskipTests
```

Or with system Maven:

```bash
mvn clean package -DskipTests
```

The runnable JAR will be at: `target/htf-data-collection-0.0.1.jar` (or the name from `pom.xml`).

### 4.2 Frontend (React / Vite)

Set the API URL for production (replace with your server’s public URL):

```bash
cd frontend
npm ci
export VITE_API_URL=https://your-domain.com/api
npm run build
```

If you don’t set `VITE_API_URL`, the app uses `http://localhost:8081` (only suitable if the backend is on the same host and you proxy by path).

Output is in `frontend/dist/`. You will serve this with Nginx (or another web server).

---

## 5. Configuration and environment variables

### 5.1 Backend configuration

Create a production config file or use environment variables so secrets are not in the repo.

**Option 1: Environment variables (recommended)**

Create a file, e.g. `/opt/htf-data-collection/app.env` (and restrict access: `chmod 600 app.env`):

```bash
# Database
export DATABASE_URL="jdbc:postgresql://localhost:5432/salvation_army_db"
export SPRING_DATASOURCE_USERNAME="your_db_user"
export SPRING_DATASOURCE_PASSWORD="your_secure_password"

# JWT (use a long random secret in production)
export JWT_SECRET="your-very-long-secret-key-at-least-256-bits-for-hs256"

# Admin (optional override)
export ADMIN_USERNAME="admin"
export ADMIN_PASSWORD="your_secure_admin_password"

# CORS: allow your frontend origin(s)
export CORS_ALLOWED_ORIGINS="https://your-domain.com"

# WhatsApp (if used)
export WHATSAPP_ACCESS_TOKEN="your_token"

# Mail (for OTP) – optional
# spring.mail.* can be set in application.properties or here
```

Then before starting the app:

```bash
source /opt/htf-data-collection/app.env
```

**Option 2: `application-prod.properties`**

Create `src/main/resources/application-prod.properties` (or put it on the server next to the JAR) with production values and activate with `--spring.profiles.active=prod`. Prefer env variables for passwords and secrets.

### 5.2 Uploads directory

The app expects an `uploads/` directory (for file uploads). Create it where the JAR runs:

```bash
mkdir -p /opt/htf-data-collection/uploads
chmod 755 /opt/htf-data-collection/uploads
```

If you use a different path, set `app.upload.dir` accordingly.

---

## 6. Run the backend as a systemd service

Running the JAR with systemd gives automatic restarts and logging.

Create the service file:

```bash
sudo nano /etc/systemd/system/htf-backend.service
```

Paste (adjust paths and user):

```ini
[Unit]
Description=HTF Data Collection Backend
After=network.target postgresql.service

[Service]
Type=simple
User=www-data
Group=www-data
WorkingDirectory=/opt/htf-data-collection

# Load env file if you use one
EnvironmentFile=/opt/htf-data-collection/app.env

ExecStart=/usr/bin/java -jar /opt/htf-data-collection/target/htf-data-collection-0.0.1.jar --spring.profiles.active=prod
Restart=on-failure
RestartSec=10

# Logs
StandardOutput=journal
StandardError=journal
SyslogIdentifier=htf-backend

[Install]
WantedBy=multi-user.target
```

**Notes:**

- Replace `htf-data-collection-0.0.1.jar` with the actual JAR name from `target/`.
- If you don’t use a profile, remove `--spring.profiles.active=prod`.
- Ensure the user (`www-data` or your choice) can read the JAR, `uploads/`, and any config files.

Enable and start:

```bash
sudo systemctl daemon-reload
sudo systemctl enable htf-backend
sudo systemctl start htf-backend
sudo systemctl status htf-backend
```

View logs:

```bash
sudo journalctl -u htf-backend -f
```

---

## 7. Nginx (reverse proxy and frontend)

Nginx can serve the built frontend and proxy API requests to the backend.

### 7.1 Install Nginx

```bash
sudo apt install -y nginx
```

### 7.2 Site configuration

Create a config (replace `your-domain.com` and paths):

```bash
sudo nano /etc/nginx/sites-available/htf-data-collection
```

Example (HTTPS with Let’s Encrypt later):

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # Frontend (built React app)
    root /opt/htf-data-collection/frontend/dist;
    index index.html;
    location / {
        try_files $uri $uri/ /index.html;
    }

    # Proxy API to Spring Boot (port 8081); path-preserving so backend receives /api/...
    location /api/ {
        proxy_pass http://127.0.0.1:8081/api/;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }

    # Optional: proxy WebSocket if you add WS later
    # location /ws { ... }
}
```

Enable and reload:

```bash
sudo ln -s /etc/nginx/sites-available/htf-data-collection /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 7.3 HTTPS with Let’s Encrypt (recommended)

```bash
sudo apt install -y certbot python3-certbot-nginx
sudo certbot --nginx -d your-domain.com
```

Follow the prompts. Certbot will adjust the Nginx config for HTTPS.

---

## 8. Frontend API URL when using Nginx

The frontend uses `VITE_API_BASE_URL` with default fallback `/api`. For production behind nginx (same origin), leave it unset or set:

```bash
export VITE_API_BASE_URL=/api
npm run build
```

For local dev with the backend on port 8081:

```bash
export VITE_API_BASE_URL=http://localhost:8081/api
npm run dev
```

---

## 9. Firewall

Allow HTTP/HTTPS and optionally SSH:

```bash
sudo ufw allow 22
sudo ufw allow 80
sudo ufw allow 443
sudo ufw enable
```

Do **not** expose 8081 publicly; only Nginx (localhost) should talk to the backend.

---

## 10. Quick checklist

| Step | Action |
|------|--------|
| 1 | Install Java 17, PostgreSQL, (optional) Node.js |
| 2 | Create PostgreSQL database and user |
| 3 | Clone or copy project and build backend (`mvn package`) |
| 4 | Build frontend with `VITE_API_URL` set |
| 5 | Create `uploads/` and config (env or application-prod.properties) |
| 6 | Run backend with systemd |
| 7 | Configure Nginx (frontend + `/api` proxy) |
| 8 | Enable HTTPS with Certbot |
| 9 | Set CORS and JWT/DB credentials for production |

---

## 11. Optional: Deploy with Docker

The project includes a `Dockerfile` that builds the backend. The app listens on **port 8081** in `application.properties`; the Dockerfile may expose 8080 — adjust if needed.

Build and run backend only (database and frontend are separate):

```bash
# Build
docker build -t htf-backend .

# Run (link DB or use host network)
docker run -d \
  --name htf-backend \
  -p 8081:8081 \
  -e DATABASE_URL=jdbc:postgresql://host.docker.internal:5432/salvation_army_db \
  -e SPRING_DATASOURCE_USERNAME=postgres \
  -e SPRING_DATASOURCE_PASSWORD=your_password \
  -v $(pwd)/uploads:/app/uploads \
  htf-backend
```

For a full Docker setup (backend + DB + Nginx), add a `docker-compose.yml` and use the same env and paths as above.

---

## 12. Troubleshooting

- **Backend won’t start:** Check `journalctl -u htf-backend -n 100`. Common: wrong `DATABASE_URL`, DB not running, or missing `uploads/`.
- **502 Bad Gateway:** Backend not running or not listening on 8081; check `systemctl status htf-backend` and Nginx `proxy_pass` address.
- **CORS errors:** Set `CORS_ALLOWED_ORIGINS` (or `app.cors.allowed-origins`) to the exact frontend origin (e.g. `https://your-domain.com`).
- **OTP emails not sent:** Ensure `spring.mail.enabled=true` and SMTP settings (and `app.mail.from`) are correct; check logs for mail errors.

---

For app-specific configuration (JWT, WhatsApp, mail), see `src/main/resources/application.properties` and override with environment variables or a production profile.
