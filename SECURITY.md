# Security Policy

Please do not report security issues in public GitHub issues.

Report vulnerabilities or accidental secret exposure to `support@cosmonaut-ai.com`. Include the affected repository, file path, relevant commit or release, reproduction details, and impact if known.

## Scope

This policy covers the Android application source, Gradle configuration, release-signing workflow, documentation, and public client configuration in this repository.

## Secret Handling

- Do not commit release keystores, signing passwords, `.envrc`, Sentry auth tokens, generated APKs, generated AABs, or local verification screenshots.
- Release signing and Sentry upload credentials belong in GitHub Actions secrets or local environment variables.
- Public Cognito IDs, Cognito domains, Sentry DSNs, and analytics project tokens are client-visible configuration, not private credentials. They should still be scoped to the correct environment.

## Public Contributions

When opening a pull request, scrub screenshots, logcat output, crash traces, emulator recordings, and sample data for private user content, credentials, and unpublished generated story text.
