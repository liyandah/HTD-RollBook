#!/usr/bin/env python3
"""Configure nginx + CORS on 187.77.99.225 for htdrollbook-api.cybercothtechnetworks.co.zw.

NEVER configure api.cybercothtechnetworks.co.zw — that subdomain is AngaPay production.
See docs/ANGAPAY-OFF-LIMITS.md.
"""
import os
import re
import sys

import paramiko

HOST = os.environ.get("DEPLOY_SSH_HOST", "187.77.99.225")
USER = os.environ.get("DEPLOY_SSH_USER", "root")
SSH_PASSWORD = os.environ.get("DEPLOY_SSH_PASSWORD")
REMOTE_ENV = "/opt/htf-data-collection/htf-backend.env"
NGINX_SITE = "/etc/nginx/sites-available/htdrollbook-api.cybercothtechnetworks.co.zw"
API_DOMAIN = "htdrollbook-api.cybercothtechnetworks.co.zw"

PRODUCTION_CORS = (
    "http://187.77.99.225,http://187.77.99.225:8599,"
    "http://localhost:5173,"
    "https://htdrollbook.cybercothtechnetworks.co.zw,"
    "https://rollbook.cybercothtechnetworks.co.zw,"
    "https://htdrollbook.com,https://www.htdrollbook.com,"
    "https://htd-roll-book.vercel.app,"
    "https://*.vercel.app,"
    "http://htdrollbook-api.cybercothtechnetworks.co.zw,"
    "https://htdrollbook-api.cybercothtechnetworks.co.zw"
)

NGINX_CONF = f"""server {{
    listen 80;
    listen [::]:80;
    server_name {API_DOMAIN};

    client_max_body_size 50M;

    location /.well-known/acme-challenge/ {{
        root /var/www/html;
    }}

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

    with sftp.open(REMOTE_ENV + ".tmp", "w") as f:
        f.write(env_text)
    run(f"mv {REMOTE_ENV}.tmp {REMOTE_ENV} && chmod 600 {REMOTE_ENV}")

    with sftp.open(NGINX_SITE, "w") as f:
        f.write(NGINX_CONF)
    run(
        f"ln -sf {NGINX_SITE} /etc/nginx/sites-enabled/htdrollbook-api.cybercothtechnetworks.co.zw"
    )
    run("nginx -t")
    run("systemctl reload nginx")
    run("systemctl restart htf-backend")

    _, out, _ = run(f"grep CORS_ALLOWED_ORIGINS {REMOTE_ENV}")
    print(out.strip())
    _, out, _ = run(
        f"curl -s -o /dev/null -w '%{{http_code}}' -H 'Host: {API_DOMAIN}' http://127.0.0.1/actuator/health",
        check=False,
    )
    print(f"nginx proxy health HTTP {out.strip()}")

    sftp.close()
    ssh.close()
    print("Done.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
