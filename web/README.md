# Vela YouTube iOS/iPadOS PWA playback proof

This directory is an isolated **browser playback experiment**, not an alternative source of YouTube media streams. It uses YouTube's official embedded IFrame Player API and does not proxy, extract, download, or strip audio/video.

## Run locally

From repository root: `python3 -m http.server 8080 --directory web` and open `http://localhost:8080` on the same machine. For iPhone/iPad testing, deploy this folder to a temporary **HTTPS** preview URL; do not deploy to the production Vela Music site. Do not add to Home Screen until served via HTTPS.

## Test matrix

- Safari iPhone: user-initiated playback, screen lock, 30 seconds elapsed, unlock, observe player/log and actual audio.
- Home Screen web app on iPhone: same test.
- Safari iPad and Home Screen iPad: same test.
- Track whether lock-screen play/pause controls appear, whether sound continues, and whether restarting requires tapping Play.
- Test real devices and document iOS/iPadOS version; never infer support from successful page loading.
- Also check player error events and cross-origin iframe restrictions.

## Acceptance boundary

A pass for this phase means **the page embeds and plays a permitted YouTube video after a user gesture**, and the observed lock-screen behavior is documented. Background audio / media controls are **not guaranteed** and may be restricted by YouTube, browsers, device settings, and platform policy. The web prototype does not include playback workarounds.

## Production note

The existing Android code remains unchanged. This lab deliberately has no service worker, login, YouTube API key, backend, analytics, autoplay, or custom Media Session implementation. Add PWA manifest and caching only after testing browser behavior and confirming an appropriate deployment origin.
