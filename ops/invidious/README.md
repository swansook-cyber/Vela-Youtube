# Invidious Home Hub pilot — preflight only

This is a **read-only preparation step**, not an installation. Run `bash ops/invidious/preflight.sh` on the Debian Home Hub after inspecting the script. It reads local memory, disk, Docker availability, open ports, and existing containers; does not change system settings, secrets, firewall, DNS, or deployment.

Official docs: https://docs.invidious.io/installation/

As of 2026-10-15, the official project requires Invidious + Invidious companion + PostgreSQL. It recommends **20 GB free disk and 2 GB available RAM**, and daily/regular restarts. The operator should not assume Docker is lightweight or the existing services have headroom.

### Gates before installing
1. Confirm 2 GiB available memory and 20 GiB free on **the filesystem intended for the stack**, not merely an unrelated media disk.
2. Confirm Docker Compose v2, container image architecture and available local loopback port (e.g. 127.0.0.1:8113:3000).
3. Record image versions/digests and inspect Docker Compose mounts and requested capabilities. Use separate named volumes and unique secrets for DB, Invidious HMAC, and companion. Never copy public documentation placeholder passwords.
4. Do not bind to 0.0.0.0 or touch cloudflared/Tailscale/proxy rules in this initial pilot.
5. Only run containers after reviewing the resulting command and getting operator permission. No `docker compose down -v`, prune, or destructive cleanup.
6. Observe CPU, RAM, disk growth, bandwidth and errors; test Thai search and a small permitted video sample.
7. Before public availability assess YouTube terms, upstream instance behavior, and AGPL-3.0 license compliance.

Rollback later: stop **only** the isolated pilot's Compose project without removing volumes. Leave existing Vela services untouched.
