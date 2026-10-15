# Isolated HTTPS preview (no production changes)

Use **Cloudflare Pages**, not the existing Home Hub services or Vela Music domain.

1. In Cloudflare Dashboard open Workers & Pages > Create > Pages > Connect to Git.
2. Connect `swansook-cyber/Vela-Youtube`; select branch `feature/pwa-ios-proof-20261015` as the production branch **of this preview Pages project only** (not the GitHub default branch).
3. Framework preset: None. Build command: leave blank. Build output directory: `web`.
4. Give the Pages project a unique name such as `vela-youtube-lab`; use its generated `*.pages.dev` hostname. Do NOT map music.velalab.net or any existing Vela service domain.
5. Visit its HTTPS URL on iPhone Safari, then try Share > Add to Home Screen. This is a preview experiment, not a signed native iOS app.
6. If Cloudflare asks for permissions, limit access to the single intended repo where possible. Do not configure repository secrets or deploy tokens in the source tree.

**Acceptance:** foreground YouTube video playback after a user gesture. Document if audio *actually continues* when the display locks for 30 seconds. iOS may stop embedded YouTube playback, and this is not a substitute for native media playback or a workaround for YouTube policy.

**Limitations:** static assets make the site installable where supported, but this prototype intentionally has no service worker and cannot play YouTube offline. YouTube's iframe player requires a network connection. iOS Home Screen behavior varies with device/browser version.
