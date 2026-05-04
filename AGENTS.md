---
description: Cosmonaut Android project-specific rules and development workflow
alwaysApply: true
---

# Cosmonaut Android — Agent Instructions

## Environment Setup

Before running any Gradle command, set `JAVA_HOME` to the Android Studio bundled JDK:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

The Android SDK is at `/Users/ianmatson/Library/Android/sdk` (configured in `local.properties`).

All Gradle commands below assume you are in the `cosmonaut-android/` directory.

## Build Verification Workflow (MANDATORY)

**After EVERY code change, you MUST run the full verification command:**

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew ktlintCheck detekt lintDevDebug assembleDevDebug
```

This runs, in order:
1. **ktlint** — Kotlin code style enforcement
2. **detekt** — Kotlin static analysis (complexity, naming, magic numbers, etc.)
3. **Android Lint** — Android-specific correctness, accessibility, performance checks
4. **Compile** — Full dev debug APK assembly

**If any step fails, fix the issue and re-run before considering the change complete.**

Do NOT skip this step. Do NOT move on to the next task until this passes. The user should never encounter a broken build.

### Quick compile-only check (use sparingly, e.g. mid-refactor)

```bash
./gradlew compileDevDebugKotlin
```

### Auto-format ktlint issues

```bash
./gradlew ktlintFormat
```

This fixes most style issues automatically. One exception: filename mismatches (e.g. file named `Foo.kt` containing `class Bar`) must be fixed manually by renaming the file.

## Linting Tools

| Tool | Version | Command | What it checks |
|------|---------|---------|----------------|
| **ktlint** | 14.2.0 | `./gradlew ktlintCheck` | Code style, formatting, import ordering |
| **detekt** | 2.0.0-alpha.3 | `./gradlew detekt` | Static analysis, complexity, naming, code smells |
| **Android Lint** | Built into AGP 9.2 | `./gradlew lintDevDebug` | Android correctness, accessibility, performance |

### ktlint configuration
- Config: `.editorconfig` at project root
- Style: `android_studio`
- Compose function naming rule is disabled (Composables use PascalCase)
- Wildcard imports and trailing commas are allowed

### detekt configuration
- Config: `config/detekt/detekt.yml`
- Compose-aware: `@Composable` annotated functions are excluded from naming/parameter rules
- Hilt modules excluded from TooManyFunctions
- Magic numbers allowed in property declarations, local variables, enums, ranges, and `@Composable` functions

## Project Structure

```
cosmonaut-android/
├── app/
│   ├── build.gradle.kts          # App module build config (flavors, deps)
│   └── src/
│       ├── main/java/com/cosmonaut/app/
│       │   ├── CosmoApp.kt       # Application class (Hilt, Coil, Timber)
│       │   ├── MainActivity.kt   # Single-Activity entry point
│       │   ├── data/
│       │   │   ├── local/        # DataStore, preferences
│       │   │   └── remote/       # Retrofit, API service, DTOs, interceptors
│       │   ├── di/               # Hilt modules (NetworkModule, DataStoreModule)
│       │   ├── navigation/       # Routes, NavHost, BottomNavItem
│       │   ├── ui/
│       │   │   ├── components/   # Reusable composables (TopAppBar, etc.)
│       │   │   ├── screens/      # Feature screens (home/, create/, settings/)
│       │   │   └── theme/        # Color, Type, Shape, CosmoTheme
│       │   └── util/             # Logging, helpers
│       ├── dev/res/values/       # Dev flavor resources (app_name)
│       ├── prod/res/values/      # Prod flavor resources (app_name)
│       └── main/res/             # Shared resources (strings, colors, themes, icons, font)
├── build.gradle.kts              # Root build file (plugin declarations)
├── gradle/libs.versions.toml     # Version catalog (ALL dependency versions)
├── config/detekt/detekt.yml      # detekt rules
├── .editorconfig                 # ktlint style config
└── docs/planning/                # Architecture & planning docs
```

## Key Conventions

### Architecture
- **Single-Activity** with Navigation Compose
- **Clean Architecture + MVVM**: data → domain → ui layers
- **Hilt** for dependency injection (use `@HiltViewModel`, `@Inject`, `@Module`)
- **kotlinx.serialization** for JSON (NOT Gson/Moshi)
- **Type-safe navigation** using `@Serializable` route objects in `CosmoRoute`

### Naming
- Files must be named after their single top-level class/interface (ktlint enforces this)
- Composable functions use PascalCase (e.g. `CosmoTopAppBar`)
- Package: `com.cosmonaut.app`
- Build config fields accessed via `BuildConfig.FIELD_NAME`

### Build Variants
- **Flavors**: `dev` (suffix `.dev`) and `prod` (no suffix)
- **Build types**: `debug` and `release`
- Environment-specific values (API URLs, Cognito config) are in `buildConfigField` entries in `app/build.gradle.kts`
- Flavor-specific resources (app name) go in `src/{dev,prod}/res/values/strings.xml`

### Dependencies
- ALL versions go in `gradle/libs.versions.toml` — never hardcode versions in build files
- Use `libs.` aliases in build files (e.g. `implementation(libs.retrofit)`)
- Compose versions managed by BOM (`androidx.compose:compose-bom`)

### AGP 9 Specifics
- No `kotlin-android` plugin — AGP 9 has built-in Kotlin support
- Use `kotlin { compilerOptions { } }` instead of `android { kotlinOptions { } }`
- No `resValue()` in product flavors — use flavor-specific `res/values/strings.xml` instead

## Planning Documents

Before implementing any stage, read the relevant planning docs:
- `docs/planning/00-master-plan.md` — Overall roadmap and stage dependencies
- `docs/planning/01-web-app-feature-catalog.md` — Web app feature reference
- `docs/planning/02-android-technology-stack.md` — Technology choices and rationale

## CI/CD

GitHub Actions workflow at `.github/workflows/android-ci.yml`:
- **Lint job**: ktlint → detekt → Android Lint
- **Build Dev**: assembleDevDebug (APK artifact)
- **Build Prod**: bundleProdRelease (AAB artifact, main branch only, needs signing secrets)
- **Unit Tests**: testDevDebugUnitTest
