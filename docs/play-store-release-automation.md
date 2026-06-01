# Play Store Release Automation

Cosmonaut Android is wired so that pushes to `main` build the signed `prodRelease` app bundle and publish it to the Google Play internal testing track.

The publishing step lives in `.github/workflows/android-ci.yml` and uploads:

```text
app/build/outputs/bundle/prodRelease/app-prod-release.aab
```

## Current Release Path

1. Merge to `main`.
2. GitHub Actions computes a Play-safe `VERSION_CODE`.
3. GitHub Actions decodes the upload keystore from secrets.
4. Gradle builds `bundleProdRelease` and `assembleProdRelease`.
5. GitHub Actions verifies that the release AAB/APK exist and that the AAB is signed.
6. `r0adkll/upload-google-play` uploads the AAB to the `internal` track with `completed` status.

This intentionally starts with internal testing instead of production. After at least one successful internal release, we can switch to a staged production rollout by changing the workflow track/status.

## Required GitHub Configuration

Add these repository secrets before the first `main` release can publish to Play:

```text
GOOGLE_PLAY_SERVICE_ACCOUNT_JSON
SIGNING_KEY
SIGNING_KEY_ALIAS
SIGNING_KEY_PASSWORD
SIGNING_STORE_PASSWORD
```

Add these recommended release-observability secrets when Sentry source/mapping uploads should run:

```text
SENTRY_AUTH_TOKEN
SENTRY_ORG
SENTRY_PROJECT
```

`GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` should be the raw JSON contents of a Google Cloud service-account key, not a base64-encoded string.

`SIGNING_KEY` should remain the base64-encoded upload keystore, matching the current workflow decode step.

Add these repository variables if they are not already present:

```text
PUBLIC_POSTHOG_PROJECT_TOKEN
PUBLIC_POSTHOG_HOST
PLAY_VERSION_CODE_OFFSET
```

`PLAY_VERSION_CODE_OFFSET` is optional. If it is unset, CI uses `100000 + GITHUB_RUN_NUMBER`. Set it higher if the Play Console already has a version code above that range.

## Required Play Console Setup

These steps cannot be completed from the repository:

1. Create or confirm the Play Console app for package `com.cosmonaut.app`.
2. Enroll the app in Play App Signing.
3. Upload the first AAB manually if Play Console/API access still reports that the package does not exist.
4. Complete app content, content rating, privacy policy, data safety, target audience, and store listing requirements.
5. Enable the Google Play Developer API for the Google Cloud project that owns the service account.
6. Invite the service account email in Play Console under Users and permissions.
7. Grant the service account app-level release permission for `com.cosmonaut.app`.
8. Configure internal testers for the internal testing track.

## Promoting Later

Once internal uploads are working, the conservative production path is:

```yaml
PLAY_RELEASE_TRACK: "production"
status: inProgress
userFraction: 0.05
```

When the staged rollout is healthy, complete it in Play Console or update the workflow to publish with `status: completed`.

Google review and managed publishing can still delay availability after CI uploads a release.
