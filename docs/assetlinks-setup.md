# Android App Links — assetlinks.json Setup

For deep links to work as verified **App Links** (opening directly in the app without a disambiguation dialog), a `/.well-known/assetlinks.json` file must be served from each domain.

## File Location

The file lives in the **cosmonaut-web** repo at:

```
cosmonaut-web/static/.well-known/assetlinks.json
```

SvelteKit copies everything in `static/` to the build output. The existing deploy workflow (`cosmonaut-web/.github/workflows/deploy.yml`) syncs the build to S3, which means `assetlinks.json` deploys automatically with every web push to `main` or `develop`. No infra changes needed.

Both the prod and dev package names are included in a single file — Android ignores entries whose fingerprint doesn't match, so this is safe.

## Required Domains

| Domain | Flavor | S3 Bucket |
|--------|--------|-----------|
| `cosmonaut-ai.com` | prod | `cosmonaut-prod-frontend` |
| `www.cosmonaut-ai.com` | prod | same (CloudFront alias) |
| `dev.cosmonaut-ai.com` | dev | `cosmonaut-dev-frontend` |

## Completing the Fingerprints

The file currently has placeholder fingerprints. Replace them:

### Production fingerprint (`com.cosmonaut.app`)
1. Go to [Google Play Console](https://play.google.com/console) → Your App → Setup → App signing
2. Copy the **SHA-256 certificate fingerprint** under "App signing key certificate"
3. Replace `REPLACE_WITH_PLAY_APP_SIGNING_SHA256_FINGERPRINT` in the file

### Dev fingerprint (`com.cosmonaut.app.dev`)
Use your upload (release) keystore fingerprint:
```bash
keytool -list -v -keystore release-keystore.jks -alias cosmonaut | grep SHA256
```
Replace `REPLACE_WITH_UPLOAD_KEY_SHA256_FINGERPRINT` in the file.

## Verification

After the web pipeline deploys the file:
```bash
curl -s https://cosmonaut-ai.com/.well-known/assetlinks.json | python3 -m json.tool
curl -s https://dev.cosmonaut-ai.com/.well-known/assetlinks.json | python3 -m json.tool
```

Use [Google's verification tool](https://developers.google.com/digital-asset-links/tools/generator) to confirm.

On an Android device:
```bash
adb shell pm get-app-links com.cosmonaut.app
```
