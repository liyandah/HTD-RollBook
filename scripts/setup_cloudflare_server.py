#!/usr/bin/env python3
"""Configure nginx + CORS on 187.77.99.225 for api.htdrollbook.com. Run locally."""
import os
import re
import sys

import paramiko

HOST = os.environ.get("DEPLOY_SSH_HOST", "187.77.99.225")
USER = os.environ.get("DEPLOY_SSH_USER", "root")
SSH_PASSWORD = os.environ.get("DEPLOY_SSH_PASSWORD")
REMOTE_ENV = "/opt/htf-data-collection/htf-backend.env"
NGINX_SITE = "/etc/nginx/sites-available/api.htdrollbook.com"
API_DOMAIN = "api.htdrollbook.com"

PRODUCTION_CORS = (
    "http://187.77.99.225,http://187.77.99.225:8599,"
    "http://localhost:5173,"
    "https://htdrollbook.com,https://www.htdrollbook.com,"
    "https://htd-roll-book.vercel.app,"
    "https://*.vercel.app"
)

NGINX_CONF = f"""server {{
    listen 80;
    listen [::]:80;
    server_name {API_DOMAIN};

    client_max_body_size 50M;

    location / {{
        proxy_pass http://127.0.0.1:8599;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_set_header X-Forwarded-Host $host;
        proxy_read_timeout 300s;
        proxy_connect_timeout 75s;
    }}
}}
"""


def main() -> int:
    password = SSH_PASSWORD
    if not password:
        print("Set DEPLOY_SSH_PASSWORD environment variable", file=sys.stderr)
        return 1

    ssh = paramiko.SSHClient()
    ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
    ssh.connect(HOST, username=USER, password=password, timeout=60)
    sftp = ssh.open_sftp()

    def run(cmd: str, check: bool = True) -> tuple[int, str, str]:
        _, stdout, stderr = ssh.exec_command(cmd)
        code = stdout.channel.recv_exit_status()
        out = stdout.read().decode("utf-8", errors="ignore")
        err = stderr.read().decode("utf-8", errors="ignore")
        if check and code != 0:
            raise RuntimeError(f"Command failed ({code}): {cmd}\n{out}\n{err}")
        return code, out, err

    print("Reading current env file...")
    with sftp.open(REMOTE_ENV, "r") as f:
        env_text = f.read().decode("utf-8")

    if re.search(r"^CORS_ALLOWED_ORIGINS=", env_text, re.MULTILINE):
        env_text = re.sub(
            r"^CORS_ALLOWED_ORIGINS=.*$",
            f"CORS_ALLOWED_ORIGINS={PRODUCTION_CORS}",
            env_text,
            count=1,
            flags=re.MULTILINE,
        )
    else:
        env_text = env_text.rstrip() + f"\nCORS_ALLOWED_ORIGINS={PRODUCTION_CORS}\n"

    print("Updating CORS in htf-backend.env...")
    with sftp.open(REMOTE_ENV + ".tmp", "w") as f:
        f.write(env_text)
    run(f"mv {REMOTE_ENV}.tmp {REMOTE_ENV} && chmod 600 {REMOTE_ENV}")

    print("Installing nginx if needed...")
    _, out, _ = run("command -v nginx || true", check=False)
    if not out.strip():
        run("export DEBIAN_FRONTEND=noninteractive && apt-get update -qq && apt-get install -y nginx")

    print("Writing nginx site config...")
    with sftp.open(NGINX_SITE, "w") as f:
        f.write(NGINX_CONF)
    run(f"ln -sf {NGINX_SITE} /etc/nginx/sites-enabled/api.htdrollbook.com")
    run("rm -f /etc/nginx/sites-enabled/default")
    run("nginx -t")
    run("systemctl enable nginx")
    run("systemctl reload nginx || systemctl start nginx")

    print("Opening firewall for HTTP/HTTPS...")
    for rule in ("80/tcp", "443/tcp"):
        run(f"ufw allow {rule} comment 'nginx-http' || true", check=False)

    print("Restarting htf-backend...")
    run("systemctl restart htf-backend")
    run("systemctl is-active htf-backend nginx")

    print("Verification (backend may need a few seconds to start)...")
    import time

    time.sleep(10)
    _, out, _ = run("curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8599/actuator/health", check=False)
    print(f"  localhost:8599 health HTTP {out.strip()}")
    _, out, _ = run(
        f"curl -s -o /dev/null -w '%{{http_code}}' -H 'Host: {API_DOMAIN}' http://127.0.0.1/actuator/health",
        check=False,
    )
    print(f"  nginx proxy health HTTP {out.strip()}")
    _, out, _ = run(f"grep CORS_ALLOWED_ORIGINS {REMOTE_ENV}")
    print(f"  {out.strip()}")

    sftp.close()
    ssh.close()
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
