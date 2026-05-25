# Cosmonaut Android Documentation

This directory contains Android implementation notes, Play Store preparation docs, and planning references for parity with the web app.

## Current References

- [`assetlinks-setup.md`](assetlinks-setup.md): Android App Links and `assetlinks.json` setup.
- [`play-store-listing.md`](play-store-listing.md): Play Store listing copy, data-safety notes, and required assets.
- [`04-google-play-external-billing.md`](04-google-play-external-billing.md): billing-policy implementation brief.
- [`03-tanstack-store5-translation.md`](03-tanstack-store5-translation.md): Store5 translation guide for developers familiar with TanStack Query.
- [`onboarding-redesign.md`](onboarding-redesign.md): onboarding UX notes.

## Planning References

- [`planning/00-master-plan.md`](planning/00-master-plan.md): staged Android roadmap.
- [`planning/01-web-app-feature-catalog.md`](planning/01-web-app-feature-catalog.md): web app feature catalog used for Android parity.
- [`planning/02-android-technology-stack.md`](planning/02-android-technology-stack.md): technology stack research and implementation guidance.

Planning documents may describe intended work as well as implemented behavior. Prefer the current source code and root [`README.md`](../README.md) for commands that must run today.

## Maintenance Notes

- Keep screenshots, emulator recordings, and generated APK/AAB files out of git.
- Do not paste release-signing material, Sentry upload tokens, or private Play Console data into docs.
- Public client identifiers are acceptable when they are already shipped in the Android app, but keep them scoped and documented.
