#!/usr/bin/env bash
# Read-only preflight. No sudo, docker pull, container creation, file writes, or port binds.
set -u
echo "=== VELA YOUTUBE INVIDIOUS READ-ONLY PREFLIGHT ==="
date -Is
uname -a
echo
echo "=== MEMORY (minimum 2 GiB available recommended) ==="
free -h || true
awk '/MemAvailable:/ {printf "MemAvailable: %.2f GiB\n",$2/1048576}' /proc/meminfo
echo
echo "=== STORAGE (minimum 20 GiB available recommended) ==="
df -h / /opt /var/lib/docker 2>/dev/null || df -h /
echo
echo "=== LOAD ==="
uptime || true
echo
echo "=== DOCKER ==="
command -v docker || true
docker version --format '{{.Client.Version}}' 2>/dev/null || true
docker compose version 2>/dev/null || true
echo
echo "=== EXISTING CONTAINERS (metadata only) ==="
docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>/dev/null || true
echo
echo "=== LOCAL LISTENERS (do not disclose external IPs) ==="
ss -ltnH 2>/dev/null | awk '{print $4}' | sed -E 's/^[^:]*:/PORT /' | sort -u || true
echo
echo "=== ROOT FILESYSTEM TYPE ==="
findmnt -n -o SOURCE,FSTYPE,TARGET / 2>/dev/null || true
echo "=== DONE: No changes made ==="
