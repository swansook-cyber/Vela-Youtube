# Alternative YouTube frontend audit (2026-10-15)

## Goal
Vela YouTube: YouTube-like web experience on iPhone/iPad, without mandatory Google login, while preserving the already operational Android fork and Cloudflare Pages preview. Background audio deferred.

## Candidates
- TeamPiped/Piped — Vue frontend and PWA support, with separate backend and proxy; no official YouTube API. Recent October 2026 issue reports include broken results and unexpectedly high data usage. It is useful as a UI reference, but not recommended for immediate production adoption.
- iv-org/invidious — full alternative frontend, Docker deployment supported. Active issues/PRs still visible in September 2026. Requires an Invidious app, PostgreSQL, and the Invidious companion per current installation docs. Ongoing risk of upstream YouTube changes and streaming regressions; it is not a static Cloudflare Pages site.

References:
- https://github.com/TeamPiped/Piped
- https://github.com/TeamPiped/Piped/issues
- https://github.com/iv-org/invidious
- https://github.com/iv-org/invidious/pulls
- https://docs.invidious.io/installation/

## Decision
Run **Invidious as an isolated proof-of-concept first**, rather than replacing existing Vela YouTube or cloning it as production. Do not run public proxy/backend until security, YouTube terms, and bandwidth implications are evaluated. Project uses AGPL-3.0: check source-sharing/attribution obligations for modified deployments.

## Evaluation gates
1. Deploy on a fresh isolated path/stack with dedicated containers, separate database, secrets, and persistent storage; no modification to Vela Music.
2. Check health, Thai search, channel pages, video player with different formats, Shorts, and playlists.
3. Test on Safari iPhone/iPad in normal and added-to-home-screen modes; focus on foreground functionality, not background playback.
4. Measure CPU/RAM, network bandwidth, disk use, refresh/stability, and error rate.
5. Confirm upstream licensing, allowable uses, and source code distribution obligations.
6. Only after gates pass, plan Vela brand/UI adaptation and migration behind a separate preview hostname.

## Rollback
Keep Cloudflare Pages deployment at vela-youtube.pages.dev unchanged. Use a separate preview address and locally bound service. Avoid merges to main and never replace existing production.
