#!/usr/bin/env python3
import os
import sys
import paramiko

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(
    os.environ.get("DEPLOY_SSH_HOST", "187.77.99.225"),
    username=os.environ.get("DEPLOY_SSH_USER", "root"),
    password=os.environ["DEPLOY_SSH_PASSWORD"],
    timeout=60,
)

cmds = [
    "systemctl is-active htf-backend nginx",
    "curl -s -o /dev/null -w 'HTTP_CODE:%{http_code}' http://127.0.0.1:8599/actuator/health",
    "curl -s -H 'Host: api.htdrollbook.com' http://127.0.0.1/actuator/health",
    "grep CORS_ALLOWED_ORIGINS /opt/htf-data-collection/htf-backend.env",
    "nginx -t 2>&1",
    "ss -tlnp | grep nginx",
    "ss -tlnp | grep 8599",
]

for cmd in cmds:
    _, stdout, stderr = ssh.exec_command(cmd)
    code = stdout.channel.recv_exit_status()
    out = stdout.read().decode("utf-8", errors="replace")
    err = stderr.read().decode("utf-8", errors="replace")
    print(f"=== {cmd} (exit {code}) ===")
    print((out + err).strip() or "(empty)")
    print()

ssh.close()
