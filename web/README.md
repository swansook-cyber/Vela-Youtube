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

## Observed device result — 2026-10-15

User-confirmed on iPhone Safari via deployed Cloudflare Pages preview: YouTube embed loads and plays picture and sound in foreground; **audio stops immediately after locking the screen**. This is a failed background-audio test, not a failed foreground playback test. Do not claim Media Session API alone can override an iframe player's behavior.

### Next test: Home Screen installed web app

1. In Safari open the Cloudflare Pages preview, tap Share > Add to Home Screen, then launch the added icon.
2. Paste the *same* embeddable YouTube video URL used for the Safari test.
3. Start the player manually and confirm foreground audio/video.
4. Lock the screen for 30 seconds. Note if sound continues, stops, or can be resumed with lock-screen media controls.
5. Record iOS version, device, and whether the site was launched from Safari or Home Screen. Report the actual result; do not assume installing as a web app changes background privileges.

### Decision gate

If foreground playback passes but lock-screen audio stops in both Safari and installed mode, mark YouTube background audio on iOS as unsupported by the tested official embed path. Preserve YouTube's authorized embed controls; do not add stream extraction, audio-only proxying, or attempts to bypass YouTube player restrictions. Continue with a video-first iOS experience or user-approved alternative instead.
