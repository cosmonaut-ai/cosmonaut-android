# Cosmonaut Android — Technology Stack Research

> Researched April 2026. Covers the modern Android stack, library choices, architecture patterns, and rationale for each decision.

---

## 1. Core Stack Decision

| Layer | Technology | Version | Rationale |
|-------|-----------|---------|-----------|
| **Language** | Kotlin | 2.1+ | Google's preferred language; full coroutine support, null safety |
| **UI Framework** | Jetpack Compose | Latest BOM | Declarative, modern, 30-50% less UI code than XML |
| **Design System** | Material Design 3 (Expressive) | M3 latest | Android-native feel, dynamic color, edge-to-edge |
| **Min SDK** | 26 (Android 8.0) | — | Covers ~97% of active devices |
| **Target SDK** | 36 (Android 16) | — | Latest platform features, Play Store requirement |
| **Build System** | Gradle (Kotlin DSL) | 8.x | Version Catalogs for dependency management |
| **Annotation Processing** | KSP | Latest | Replaces KAPT, 2x faster compilation |

---

## 2. Architecture

### Clean Architecture + MVVM (Recommended by Google)

```
┌─────────────────────────────────────────────┐
│                 UI Layer                     │
│  Composables → ViewModel → UiState          │
│  (Stateless)   (StateFlow)  (Sealed class)  │
├─────────────────────────────────────────────┤
│               Domain Layer                   │
│  Use Cases (pure Kotlin, no Android deps)    │
│  Repository Interfaces                       │
├─────────────────────────────────────────────┤
│                Data Layer                    │
│  Repository Implementations                  │
│  Remote Data Sources (API)                   │
│  Local Data Sources (Room)                   │
│  Data Models ↔ Domain Models mappers         │
└─────────────────────────────────────────────┘
```

### Key Patterns
- **Single UiState per screen**: one sealed class/interface holding all UI state, exposed as `StateFlow` from ViewModel
- **Unidirectional data flow**: UI → Events → ViewModel → UiState → UI
- **Repository pattern**: single source of truth, mediates between remote and local data
- **Use cases**: optional but recommended for complex business logic; keeps ViewModels thin

---

## 3. Library Selections

### Networking

| Library | Purpose | Notes |
|---------|---------|-------|
| **Retrofit 2** | REST API client | Industry standard, annotation-based, excellent Kotlin support |
| **OkHttp 4** | HTTP client | Interceptors for auth, logging, retry; underpins Retrofit |
| **kotlinx.serialization** | JSON parsing | Kotlin-native, compile-time safe, no reflection |
| **OkHttp SSE** | Server-Sent Events | For story text streaming; better than Ktor for this use case since Retrofit already uses OkHttp |

### Dependency Injection

| Library | Purpose | Notes |
|---------|---------|-------|
| **Hilt** | DI framework | Google-recommended, Compose integration, ViewModel injection |

### Local Storage

| Library | Purpose | Notes |
|---------|---------|-------|
| **DataStore** | Key-value preferences | Replaces SharedPreferences; for user settings, auth tokens |

> **Note**: Room/SQLite is intentionally excluded from v1. No offline caching — network is required for all features. Room can be added in v2 if offline support becomes a priority.

### Image Loading

| Library | Purpose | Notes |
|---------|---------|-------|
| **Coil 3** | Image loading | Kotlin-first, Compose `AsyncImage`, disk/memory caching |

### Navigation

| Library | Purpose | Notes |
|---------|---------|-------|
| **Navigation Compose** | In-app navigation | Type-safe routes (Kotlin 2.0+), deep links, back gesture support |

### Audio Playback

| Library | Purpose | Notes |
|---------|---------|-------|
| **Media3 (ExoPlayer)** | Audio playback | Jetpack standard; notification controls, lock screen, audio focus |
| **Media3 UI Compose** | Player UI | Material 3 compose components for media controls |

### Authentication

| Library | Purpose | Notes |
|---------|---------|-------|
| **AWS Amplify Android** | Cognito auth | Direct port of web auth; sign-in/up, Google OAuth, token management |
| **Credential Manager** | Google Sign-In | Android-native Google sign-in (newer API, better UX than web redirect) |

### Payments

| Library | Purpose | Notes |
|---------|---------|-------|
| **Google Play Billing** | Subscriptions | `com.android.billingclient:billing-ktx`; required for Play Store distribution |

### Analytics & Monitoring

| Library | Purpose | Notes |
|---------|---------|-------|
| **Sentry Android SDK** | Error tracking | Crash reporting, ANR detection, performance monitoring |
| **PostHog Android SDK** | Product analytics | Event tracking, user identification, feature flags |

### Graph Visualization

| Library | Purpose | Notes |
|---------|---------|-------|
| **Custom Compose Canvas** | Story map | No XYFlow equivalent for Compose; fully custom with Canvas API, `graphicsLayer` transforms for pan/zoom, `detectTransformGestures` |

---

## 4. Cosmonaut Design System (shadcn → Compose)

The web app uses **shadcn/bits-ui** primitives styled with Tailwind Variants (`tv()`). Each component defines variants (e.g. `default`, `destructive`, `outline`, `ghost`) and sizes. Cosmonaut adds custom styling on top — most notably, the **3D button depth effect** via `box-shadow` + `translateY` on hover/active.

The Compose equivalent is a **custom design system layer** built on top of Material 3, following the same variant-based pattern.

### Architecture

```
Material 3 (foundation)
    └── CosmoTheme (custom colors, typography, shapes)
         └── Cosmo* composables (CosmoButton, CosmoCard, CosmoBadge, etc.)
              └── Feature composables use Cosmo* components
```

### Implementation Pattern

Each reusable component becomes a Compose function with `enum`-based variants, mirroring the shadcn pattern:

```kotlin
enum class CosmoButtonVariant { Default, Destructive, Outline, Secondary, Ghost, Link }
enum class CosmoButtonSize { Default, Small, Large, Icon, IconSmall }

@Composable
fun CosmoButton(
    onClick: () -> Unit,
    variant: CosmoButtonVariant = CosmoButtonVariant.Default,
    size: CosmoButtonSize = CosmoButtonSize.Default,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
)
```

### Custom Styling Rules

These are the Cosmonaut-specific styles that go **beyond** stock Material 3 and must be implemented in the design system:

| Web Pattern | Source File | Compose Equivalent |
|-------------|------------|-------------------|
| **Button depth** (3D shadow + translateY on press) | `layout.css` lines 220-247 | Custom `Modifier` with `graphicsLayer { translationY }` + `drawBehind` for shadow |
| **Card glow on hover** (primary border + elevated shadow) | `WorldCard.svelte` `<style>` | `Modifier.pointerInput` for press state + animated border/elevation |
| **Gold blinking cursor** | `StoryCard.svelte` `.story-cursor` | Custom `Canvas` composable with `InfiniteTransition` |
| **Shimmer loading** | `WorldCard.svelte` `.world-card-shimmer` | `Modifier.shimmer()` extension using `Brush.linearGradient` + `InfiniteTransition` |
| **Gradient card headers** | `WorldCard.svelte` `.world-card-gradient` | `Modifier.background(Brush.linearGradient())` |
| **Choice entrance animation** | `ChoiceList.svelte` `@keyframes choice-enter` | `AnimatedVisibility` with staggered delays per item |
| **Badge variants** (pill-shaped with variant colors) | `badge.svelte` | `Surface` composable with `RoundedCornerShape(50%)` + variant colors |
| **Segmented control** | `SegmentedControl.svelte` | Material 3 `SegmentedButton` or custom `Row` with selection indicator |

### Theme Tokens

Map the web's CSS custom properties (`--primary`, `--card`, etc.) to a `CosmoColorScheme`:

```kotlin
data class CosmoColorScheme(
    val primary: Color,            // Gold in dark: oklch(0.9536 0.0872 97.9082)
    val primaryForeground: Color,
    val primaryDepth: Color,       // Shadow color for button depth effect
    val card: Color,
    val cardForeground: Color,
    val destructive: Color,
    val destructiveDepth: Color,
    val muted: Color,
    val mutedForeground: Color,
    val border: Color,
    // ... etc, matching layout.css :root and .dark blocks
)
```

### Typography

| Role | Web Font | Compose Equivalent |
|------|---------|-------------------|
| Body / UI | Inter | `FontFamily` from Google Fonts or bundled `.ttf` |
| Display / Brand | Orbitron | Bundled `static/fonts/Orbitron-VariableFont_wght.ttf` (copy from web repo) |
| Mono (prompts) | JetBrains Mono | `FontFamily.Monospace` or bundled |
| Story prose | Inter + Typography plugin | Custom `TextStyle` with generous `lineHeight`, `letterSpacing` |

### Icon System

The web uses **Lucide** icons. For Android:
- Use **Lucide's Android/Compose package** if available, OR
- Use **Material Symbols** (Google's icon set) as the closest native equivalent
- Map icon names: `ChevronRight` → equivalent Material icon, `Rocket` → custom drawable, etc.

---

## 5. Mobile-Specific UX Adaptations

### Navigation: Bottom Nav Instead of Top Header

The web app uses a top header with logo + user menu. On Android:

- **Bottom Navigation Bar** (Material 3 `NavigationBar`): 3-5 primary destinations
  - Suggested tabs: **Home** (dashboard), **Create** (new world), **Settings**
  - The story reader and world home do NOT appear in bottom nav — they're push destinations
- **Top App Bar**: contextual title + actions (back, share, audio, map)
- **No footer**: all footer links move to Settings or are handled in-app

### Story Reader Adaptations

- **Full-screen immersive reading**: edge-to-edge content, minimal chrome
- **Bottom sheet for choices**: instead of inline choice list, use a bottom sheet that slides up with choices after story text is read — more thumb-friendly
- **Swipe gestures**: swipe left/right for forward/back navigation between nodes
- **Pull-to-refresh**: for retrying failed generations

### Audio Mini-Player Layout (Spotify-Style)

When audio narration is active, a mini-player bar sits **between the page content and the bottom navigation bar**, exactly like Spotify, YouTube Music, or Apple Music. The layout stacks as:

```
┌──────────────────────────────┐
│                              │
│        Page Content          │
│    (scrollable, padded       │
│     at bottom to clear       │
│     mini-player height)      │
│                              │
├──────────────────────────────┤  ← Mini-player bar (56-64dp)
│ ▶  Node Title    ──●──── ✕  │    Play/pause, title, progress, close
├──────────────────────────────┤  ← Bottom Navigation Bar (80dp)
│  🏠 Home    ✚ Create   ⚙    │
└──────────────────────────────┘
```

**Key behaviors:**
- Mini-player appears with a slide-up animation when narration starts
- Content area adds bottom padding to prevent the mini-player from covering text
- Tapping the mini-player expands it to a **full-screen or bottom sheet player** with seek, volume, playback speed, and voice picker
- On the Story Reader screen, the bottom nav may be hidden (immersive mode), so the mini-player sits at the very bottom
- Mini-player persists across screen navigation while audio is playing (managed by a top-level composable in the scaffold)
- MediaSession provides lock screen and notification controls independently

### World Cards

- **Vertical list or 2-column grid**: on phones, single column list may feel more native than grid
- **Swipe-to-delete**: instead of delete button on card, use swipe gesture with confirmation
- **Long-press context menu**: share, delete, view details

### Create World

- **Full-screen form**: not a card within a page — each section gets full breathing room
- **Sticky submit button**: floating action button or sticky bottom bar
- **Keyboard management**: proper IME handling, scroll-into-view for focused fields

### Authentication

- **Credential Manager**: native Google Sign-In UI (one-tap) instead of web redirect
- **Biometric unlock**: optional fingerprint/face unlock for returning users
- **Login screen**: single column, no illustration panel (save space for mobile)

### General Mobile Patterns

- **Edge-to-edge**: content draws behind system bars (Android 15+ default)
- **Predictive back gestures**: system back animation support
- **Dynamic color**: Material You color theming from device wallpaper (with Cosmonaut brand override option)
- **Haptic feedback**: subtle vibration on choice selection, world creation
- **Deep links**: `/worlds/[id]` and `/sessions/[id]` open directly in app
- **Share intents**: native Android share sheet for world links
- **Push notifications**: optional notifications for world generation completion (via FCM)

---

## 6. Challenges & Mitigations

### Challenge 1: SSE Streaming Performance
- **Problem**: Story text streams token-by-token via SSE; naive approach causes per-token recomposition jank in Compose
- **Mitigation**: Batch tokens in ~48ms windows using `Flow.conflate()` or custom time-windowed buffer. Use `derivedStateOf` for computed display text.

### Challenge 2: Story Map Graph
- **Problem**: No XYFlow equivalent for Compose; need fully custom graph visualization
- **Mitigation**: Build custom with Compose Canvas + `graphicsLayer` transforms for pan/zoom + `detectTransformGestures`. Consider using `LazyLayout` for node recycling in large graphs. Start with simple tree layout, iterate on advanced gestures.

### Challenge 3: Audio Playback Integration
- **Problem**: Need seamless audio with lock screen controls, background playback, audio focus management
- **Mitigation**: Media3 + MediaSession provides all of this out of the box. More capable than web's `<audio>` element.

### Challenge 4: Authentication Parity
- **Problem**: Must match web's Cognito auth exactly (same user pool, same tokens)
- **Mitigation**: AWS Amplify Android SDK connects to the same Cognito User Pool. Google Sign-In uses Credential Manager API for native feel, but same Cognito backend.

### Challenge 5: Google Play Billing + Dual-Billing Backend
- **Problem**: The web uses Stripe; Android must use Google Play Billing. Backend needs to support both billing systems for the same user.
- **Mitigation**: New API endpoint for Google Play receipt verification. Backend stores billing provider per user. Google Play RTDN (Real-Time Developer Notifications) via Cloud Pub/Sub for lifecycle events. Both systems map to the same tier model.

### Challenge 7: Text Rendering Quality
- **Problem**: Story text needs beautiful typography with italic emphasis, proper paragraph spacing
- **Mitigation**: Compose's `AnnotatedString` with `SpanStyle` handles inline formatting. Use custom `TextStyle` with proper line height, letter spacing, and serif font option for reading.

---

## 7. Environments & Build Variants

The web app uses two environments (dev + prod) with separate API endpoints, Cognito pools, and CloudFront distributions. Android achieves the same thing using **Gradle Build Variants** — specifically, **product flavors**.

### How It Works

Gradle build variants = **build type** × **product flavor**:

| Build Type | Purpose |
|-----------|---------|
| `debug` | Local development — debuggable, ProGuard off, debug signing |
| `release` | Production builds — minified, ProGuard/R8 on, release signing |

| Product Flavor | API Base URL | Cognito Config | Notes |
|---------------|-------------|---------------|-------|
| `dev` | `api.dev.cosmonaut-ai.com` | Dev user pool | Internal testing, deploy first |
| `prod` | `api.cosmonaut-ai.com` | Prod user pool | Public release |

This produces 4 build variants: `devDebug`, `devRelease`, `prodDebug`, `prodRelease`.

### Configuration

In `build.gradle.kts`:

```kotlin
android {
    // ...
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            applicationIdSuffix = ".dev"        // com.cosmonaut.app.dev
            versionNameSuffix = "-dev"
            buildConfigField("String", "API_BASE_URL", "\"https://api.dev.cosmonaut-ai.com\"")
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"us-east-2_XXXDEV\"")
            buildConfigField("String", "COGNITO_CLIENT_ID", "\"devClientId\"")
            // ... other env-specific values
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "API_BASE_URL", "\"https://api.cosmonaut-ai.com\"")
            buildConfigField("String", "COGNITO_USER_POOL_ID", "\"us-east-2_XXXPROD\"")
            buildConfigField("String", "COGNITO_CLIENT_ID", "\"prodClientId\"")
        }
    }
}
```

### Key Benefits

- **Separate app IDs**: `com.cosmonaut.app.dev` and `com.cosmonaut.app` can be installed **side by side** on the same device
- **Separate Play Store listings** (optional): dev flavor can be deployed to an internal testing track
- **Build-time configuration**: all env-specific values are compiled in, no runtime switching
- **Flavor-specific resources**: different app icons, app names, or config files per flavor (e.g., `src/dev/res/values/strings.xml` with `app_name = "Cosmonaut Dev"`)
- **Amplify config per flavor**: place `amplifyconfiguration.json` in `src/dev/` and `src/prod/` directories

### Workflow (Mirrors Web)

1. Develop and test against `devDebug` locally
2. Deploy `devRelease` to Play Store internal testing track for QA
3. After validation, build `prodRelease` and deploy to production track
4. The `dev` flavor always deploys first — same safety model as the web's dev environment

### Google Play Signing & Tracks

| Track | Flavor | Purpose |
|-------|--------|---------|
| **Internal testing** | `dev` or `prod` | Immediate deploy, team only (up to 100 testers) |
| **Closed testing** | `prod` | Invite-only beta |
| **Open testing** | `prod` | Public beta |
| **Production** | `prod` | Full release with staged rollout |

---

## 8. Play Store Launch Requirements

### Developer Account
- Google Play Developer account ($25 one-time fee)
- D-U-N-S number for organization accounts
- Identity verification (can take days)

### Store Listing Assets
- App icon: 512×512 PNG
- Feature graphic: 1024×500
- Phone screenshots: 1080×1920 (8 slots)
- Tablet screenshots: if tablet support
- Short description: 80 chars
- Full description: 4000 chars

### Compliance
- Content rating questionnaire
- Data safety section (data collection disclosures)
- Privacy policy URL (can use existing cosmonaut-ai.com/privacy)
- Target audience declaration
- COPPA compliance (relevant since app is family-oriented)

### Technical
- Android App Bundle (AAB) format (not APK)
- Play App Signing enrollment
- Minimum target SDK requirements (currently SDK 34+)
- Edge-to-edge compliance (Android 15+)

### Testing Tracks
1. **Internal testing**: immediate deploy, up to 100 testers
2. **Closed testing**: invite-only, larger group
3. **Open testing**: anyone can join
4. **Production**: full public release

---

## 9. Project Structure (Recommended)

```
cosmonaut-android/
├── app/
│   ├── src/main/
│   │   ├── java/com/cosmonaut/app/
│   │   │   ├── CosmoApp.kt              # Application class
│   │   │   ├── MainActivity.kt           # Single Activity
│   │   │   ├── di/                        # Hilt modules
│   │   │   ├── navigation/                # Nav graph, routes
│   │   │   ├── ui/
│   │   │   │   ├── theme/                 # Material 3 theme, colors, typography
│   │   │   │   ├── components/            # Reusable composables
│   │   │   │   ├── screens/
│   │   │   │   │   ├── auth/              # Login, signup, onboarding
│   │   │   │   │   ├── dashboard/         # World list, create
│   │   │   │   │   ├── world/             # World home, story reader
│   │   │   │   │   ├── story/             # Story node, choices, map
│   │   │   │   │   ├── settings/          # Settings, pricing
│   │   │   │   │   └── shared/            # Shared screen components
│   │   │   │   └── state/                 # Shared UI state holders
│   │   │   ├── domain/
│   │   │   │   ├── model/                 # Domain models
│   │   │   │   ├── repository/            # Repository interfaces
│   │   │   │   └── usecase/               # Use cases
│   │   │   ├── data/
│   │   │   │   ├── remote/                # API service, DTOs
│   │   │   │   ├── repository/            # Repository implementations
│   │   │   │   └── mapper/                # DTO ↔ Domain mappers
│   │   │   └── util/                      # Extensions, helpers
│   │   ├── res/                           # Resources (drawables, strings, etc.)
│   │   └── AndroidManifest.xml
│   ├── src/test/                          # Unit tests
│   └── src/androidTest/                   # Instrumented tests
├── gradle/
│   └── libs.versions.toml                 # Version Catalog
├── build.gradle.kts                       # Root build file
├── settings.gradle.kts
└── docs/planning/                         # This documentation
```
