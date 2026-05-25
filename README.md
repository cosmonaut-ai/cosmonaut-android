# Cosmonaut Android

Native Android client for Cosmonaut AI, an AI-powered interactive storytelling platform.

## Stack

- Kotlin and Jetpack Compose
- Single-activity architecture with Navigation Compose
- Hilt for dependency injection
- Retrofit and kotlinx.serialization for API calls
- AWS Amplify Cognito authentication
- Sentry crash reporting and PostHog product analytics

## Prerequisites

- Android Studio
- Android SDK at `~/Library/Android/sdk`
- Java 17. For local Gradle commands, use the Android Studio bundled JDK:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

## Setup

```bash
./gradlew assembleDevDebug
```

The dev flavor points at `dev.cosmonaut-ai.com` and uses the application id `com.cosmonaut.app.dev`.

## Verification

Run the full local verification workflow before merging Android code changes:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew ktlintCheck detekt lintDevDebug assembleDevDebug
```

## Release Signing

Release builds read signing configuration from environment variables or GitHub Actions secrets:

- `SIGNING_KEY`
- `SIGNING_KEY_ALIAS`
- `SIGNING_KEY_PASSWORD`
- `SIGNING_STORE_PASSWORD`
- `SIGNING_STORE_FILE`
- `POSTHOG_API_KEY`
- `POSTHOG_HOST`

Do not commit keystores, local signing files, generated APKs, or verification screenshots.

## Public Configuration

The Cognito IDs, Sentry DSN, and PostHog project token in the app build config are client-visible public configuration, not server-side secrets. Runtime signing material and Sentry upload tokens must stay in GitHub Actions secrets or local environment variables.
