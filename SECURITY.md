# Security Policy

Please do not report security issues in public GitHub issues.

Report vulnerabilities or accidental secret exposure to `support@cosmonaut-ai.com` with the affected repository, file path, and reproduction details.

## Secret Handling

- Do not commit release keystores, signing passwords, `.envrc`, Sentry auth tokens, generated APKs, generated AABs, or local verification screenshots.
- Release signing and Sentry upload credentials belong in GitHub Actions secrets or local environment variables.
- Public Cognito IDs, domains, Sentry DSNs, and analytics project tokens are client-visible configuration, not private credentials.
