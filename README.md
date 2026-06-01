# Cosmonaut Android

Native Android client for [Cosmonaut AI](https://cosmonaut-ai.com), an AI-powered interactive storytelling platform.

## Repository Role

Cosmonaut is split across several public repositories:

- [`cosmonaut-web`](https://github.com/cosmonaut-ai/cosmonaut-web): SvelteKit frontend.
- [`cosmonaut-api`](https://github.com/cosmonaut-ai/cosmonaut-api): Backend API and workers.
- [`cosmonaut-infra`](https://github.com/cosmonaut-ai/cosmonaut-infra): Terraform infrastructure.
- [`cosmonaut-android`](https://github.com/cosmonaut-ai/cosmonaut-android): Native Android client.

## Stack

- Kotlin and Jetpack Compose
- Single-activity architecture with Navigation Compose
- Hilt for dependency injection
- Retrofit and kotlinx.serialization for API calls
- AWS Amplify Cognito authentication
- Store5 planning for cache/source-of-truth patterns
- Sentry crash reporting and PostHog product analytics

## World And Session Model

Android mirrors the current public API split:

- Root world routes (`WorldHome`) use `/worlds/{worldId}` for canonical/shareable metadata and invite links.
- Session routes (`SessionHome`, `StoryNode`, `StoryMap`) use `/sessions/{sessionId}` for the current user's saved playthrough, progress, node state, text streaming, and audio.
- Creating a world returns both the root world and the owner's first session, so the create flow navigates to the returned session.
- Shared session links are accepted as deep links; inaccessible sessions are handed off to the root world when the backend says the viewer can read it.

## Local Setup

Prerequisites:

- Android Studio
- Android SDK at `~/Library/Android/sdk`
- Java 17. For local Gradle commands on this machine, use the Android Studio bundled JDK:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

Build the dev APK:

```bash
./gradlew app:assembleDevDebug
```

The dev flavor uses application id `com.cosmonaut.app.dev` and points at the development Cosmonaut API.

## Verification

Run the current local verification workflow before merging Android code changes:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew app:lintDevDebug app:assembleDevDebug
```

For UI changes, install and inspect the app on an emulator or device:

```bash
adb install -r app/build/outputs/apk/dev/debug/app-dev-debug.apk
adb shell am force-stop com.cosmonaut.app.dev
adb shell am start -n com.cosmonaut.app.dev/com.cosmonaut.app.MainActivity
```

## Release Signing

Release builds read signing configuration from environment variables or GitHub Actions secrets:

- `SIGNING_KEY`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_STORE_FILE`
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`
- `SENTRY_AUTH_TOKEN`
- `SENTRY_ORG`
- `SENTRY_PROJECT`
- `POSTHOG_API_KEY`
- `POSTHOG_HOST`

Do not commit keystores, local signing files, generated APKs, generated AABs, or verification screenshots.

## Public Configuration

Cognito IDs, Cognito domains, Sentry DSNs, and PostHog project tokens in the app build config are client-visible public configuration, not server-side secrets. Runtime signing material and Sentry upload tokens must stay in GitHub Actions secrets or local environment variables.

## Documentation

Start with [`docs/README.md`](docs/README.md). The most useful references are:

- [`docs/planning/00-master-plan.md`](docs/planning/00-master-plan.md): Android roadmap and feature sequencing.
- [`docs/planning/01-web-app-feature-catalog.md`](docs/planning/01-web-app-feature-catalog.md): web feature reference for Android parity.
- [`docs/planning/02-android-technology-stack.md`](docs/planning/02-android-technology-stack.md): technology choices and implementation notes.
- [`docs/play-store-listing.md`](docs/play-store-listing.md): Play Store copy and data-safety notes.
- [`docs/assetlinks-setup.md`](docs/assetlinks-setup.md): Android App Links setup.

## CI/CD

GitHub Actions builds dev APKs from `develop`. Pushes to `main` build signed production artifacts and publish the production AAB to the Google Play internal testing track when Play Console secrets are configured. Artifact upload is disabled when the repository is public. See [`docs/play-store-release-automation.md`](docs/play-store-release-automation.md).

## Security

See [`SECURITY.md`](SECURITY.md) for disclosure and secret-handling guidance.

## Contributing

Issues and pull requests are welcome. Include emulator/device verification for UI behavior changes and keep generated build outputs out of commits.

## License

Apache-2.0. See [`LICENSE`](LICENSE).
