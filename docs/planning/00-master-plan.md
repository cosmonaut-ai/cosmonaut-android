# Cosmonaut Android — Master Plan

> A staged roadmap for building the Cosmonaut Android app from zero to Play Store launch.
> Each stage is designed to be independently plannable and implementable.
>
> **Companion documents:**
>
> - [01-web-app-feature-catalog.md](./01-web-app-feature-catalog.md) — Complete feature reference
> - [02-android-technology-stack.md](./02-android-technology-stack.md) — Stack research and rationale

---

## Key Decisions (Resolved)


| Decision               | Choice                        | Rationale                                                             |
| ---------------------- | ----------------------------- | --------------------------------------------------------------------- |
| **Billing**            | External (Stripe via cosmonaut-ai.com) | Google Play allows external billing; app is consumption-only with no in-app purchases |
| **Story Map**          | Native Compose Canvas         | Highest quality, fully native feel — worth the investment             |
| **Landing/Onboarding** | Simple onboarding carousel    | 2-3 slides showing features, then login — standard native app pattern |
| **Offline Support**    | None for v1                   | Network required at all times; simplifies architecture; can add later |
| **Tablet Support**     | Basic adaptive                | 2-column grids, wider content areas — no major redesigns for v1       |


---

## Guiding Principles

1. **Mobile-native, not a port.** Every screen should feel like it was designed for Android first. Bottom navigation, gesture-based interactions, edge-to-edge, Material 3 Expressive.
2. **Quality over speed.** Every stage ships a polished, testable artifact. No placeholder screens that "we'll fix later."
3. **Vertical slices over horizontal layers.** Each stage delivers a complete, working feature path — not "all networking" then "all UI." This enables testing real flows early.
4. **Online-first for v1.** Network connectivity is required for all features. No offline caching complexity in the initial release — this is a deliberate scope cut that can be revisited in v2.
5. **API-first, shared backend.** The Android app consumes the exact same API as the web app. No backend changes needed except potentially mobile-specific push notification endpoints. All billing flows use the existing Stripe integration via cosmonaut-ai.com.

---

## Stage Overview


| #   | Stage                                  | Scope                                            | Depends On |
| --- | -------------------------------------- | ------------------------------------------------ | ---------- |
| 1   | Project Scaffolding & Foundation       | Gradle, DI, theming, navigation shell            | —          |
| 2   | Authentication                         | Cognito sign-in/up, Google OAuth, onboarding     | Stage 1    |
| 3   | Dashboard & World Management           | World list, create world, world home             | Stage 2    |
| 4   | Story Reader (Core Experience)         | Node reader, SSE streaming, choices              | Stage 3    |
| 5   | Audio Narration                        | TTS generation, Media3 player, voice selection   | Stage 4    |
| 6   | Story Map                              | Graph visualization, node navigation             | Stage 4    |
| 7   | Sharing & Social                       | Share modal, invite links, visibility            | Stage 3    |
| 8   | Subscription UI & Explorer Tier Change | External billing UI, usage tracking, tier update | Stage 2    |
| 9   | Settings & Account                     | Account management, preferences, deletion        | Stage 2    |
| 10  | Polish, Accessibility & Performance    | Animations, a11y audit, performance optimization | All        |
| 11  | Analytics, Monitoring & Error Tracking | Sentry, PostHog integration                      | Stage 1    |
| 12  | Play Store Preparation & Launch        | Store listing, compliance, release               | All        |


---

## Stage 1: Project Scaffolding & Foundation

**Goal**: A running Android app with the complete architectural skeleton — every subsequent stage plugs into this foundation without restructuring.

### Deliverables

- Android Studio project with Gradle Kotlin DSL + Version Catalog
- Hilt DI setup with all module placeholders
- Material 3 theme matching Cosmonaut brand (dark/light, custom colors, Orbitron font, dynamic color support)
- Single-Activity architecture with Navigation Compose
- Bottom navigation shell (Home, Create, Settings tabs) with empty placeholder screens
- Top App Bar component with back navigation support
- Networking foundation: Retrofit + OkHttp + kotlinx.serialization configured
- DataStore setup for preferences
- Coil image loading configured
- Logging utility
- Build variants: `dev` and `prod` product flavors with separate API URLs, Cognito configs, and app IDs (see tech stack doc §7)
- CI-ready build (linting with ktlint/detekt, debug/release build types)
- Basic app icon and splash screen (Android 12+ splash API)

### Key Decisions

- Package naming: `com.cosmonaut.app`
- Module structure: single module initially, extract features later if needed
- Min SDK 26, Target SDK 36
- Compose BOM for version alignment

### Exit Criteria

- App launches, shows bottom nav with 3 tabs, each showing a placeholder composable
- Network layer can make a test request to the health endpoint
- Theme matches Cosmonaut brand colors in dark mode
- All dependencies compile cleanly with no deprecation warnings

---

## Stage 2: Authentication

**Goal**: Users can sign in with email/password or Google, sign up, verify their email, reset their password, and complete onboarding — using the same Cognito User Pool as the web app.

### Deliverables

- **Onboarding carousel** (first launch only): 2-3 slides showcasing Cosmonaut's features (interactive stories, branching narratives, audio narration), then CTA to sign in or sign up
- AWS Amplify Android SDK integration with existing Cognito User Pool config
- Sign-in screen: email/password + Google Sign-In (Credential Manager API)
- Sign-up screen: email/password with real-time password strength validation
- Email verification screen: 6-digit code input with resend
- Forgot/reset password flow
- Onboarding screen: username selection (availability check), newsletter opt-in, age verification
- Auth state management: global auth state via ViewModel or Hilt singleton
- Token management: auto-refresh, secure storage (EncryptedSharedPreferences or DataStore)
- Auth interceptor: OkHttp interceptor that attaches JWT to all API requests
- Protected route guard: navigation guard redirecting unauthenticated users to login
- Session expiry handling: redirect to login with "session expired" message

### Mobile-Specific Design

- Single-column login form (no desktop illustration panel)
- Native Google one-tap sign-in instead of web redirect
- Biometric re-auth consideration (future enhancement, not required for launch)
- In-app browser warning (same as web for social app browsers)
- Keyboard-aware scroll (form fields scroll above keyboard)

### Exit Criteria

- New user can sign up with email, verify, complete onboarding, and land on dashboard
- Existing user can sign in with email/password or Google
- Password reset flow works end-to-end
- Auth tokens are securely stored and automatically refreshed
- Unauthenticated API calls are properly rejected and redirect to login

---

## Stage 3: Dashboard & World Management

**Goal**: Users can see their worlds, create new ones, and navigate to a world's home screen. This is the primary hub of the app.

### Deliverables

- Dashboard screen with "Your Stories" section
- World card composable: cover image (Coil), title, genre, status badge, metadata footer
- World list: vertical scrolling list (or 2-column grid on larger screens) with LazyColumn/LazyVerticalGrid
- Loading state: skeleton cards with shimmer animation
- Empty state: astronaut illustration with CTA
- Error state: error card with retry
- Pagination: infinite scroll with automatic next-page fetching
- Pull-to-refresh
- Swipe-to-delete with undo snackbar (or long-press → delete confirmation)
- Create World screen: prompt input, visibility selector, story length/vocab/content filter controls
- Random prompt feature (load from bundled prompts file)
- Preference persistence (DataStore for length/vocab/filter defaults)
- World Home screen: hero image, title/description, quick actions (Continue, Map, Share)
- World generation progress: polling indicator with status text
- World generation failed: error state with retry/delete
- Repository layer: WorldRepository with remote data source

### Mobile-Specific Design

- Bottom sheet or dialog for delete confirmation
- Floating Action Button for "Create Story" (in addition to or replacing header button)
- Shimmer effect for loading states (Material 3 style)
- Entrance animations: staggered card appearance
- Quick-play button: tap world card to continue from last node (fetch progress first)

### Exit Criteria

- User sees their worlds list after login
- Can create a new world and see it appear (with generation progress)
- Can delete a world with confirmation
- Can navigate to a completed world's home screen
- Pull-to-refresh works
- Empty state and error states display correctly

---

## Stage 4: Story Reader (Core Experience)

**Goal**: The heart of the app — reading story nodes with streaming text generation, making choices, and navigating the story tree. This must be a premium, immersive reading experience.

### Deliverables

- Story node reader screen (full-screen, immersive):
  - Top app bar: back/undo, map button, audio button, share button
  - Parent choice context banner ("You chose: ...")
  - Story text with rich typography (serif font option, proper line height, paragraph spacing)
  - Inline italic emphasis (`*text`* → styled spans)
- SSE streaming integration:
  - OkHttp EventSource for `/generate-text` endpoint
  - Token batching (~48ms window) to prevent recomposition jank
  - Typewriter "flavor text" while waiting for first token
  - Blinking cursor composable during streaming
  - Stream abort on navigation/back
- Choice system:
  - Choice list composable with numbered buttons
  - Explored choice state (dimmed, checkmark)
  - Pre-generated ("Quick") badge
  - Custom choice badge with creator name
  - Custom choice input: expandable text field with character counter
  - Quota limit state: disabled choices with upgrade prompt
- Node navigation:
  - Forward/back navigation with slide transitions (Compose `AnimatedContent`)
  - Auto-scroll to top on new node
  - URL-based node ID tracking
- Generation states:
  - Generating (by another session): polling indicator
  - Failed: error card with Retry, Go Back, Dashboard buttons
  - Wrong session: informational card with Map/Start Over options
- Story ending state: custom ending composable with illustration + "Start Over"
- Upgrade prompt dialogs for node and audio quota limits
- Choose endpoint integration: base choices and custom choices

### Mobile-Specific Design

- Immersive reading mode: hide system bars during reading, tap to reveal
- Swipe-right for "go back" (predictive back gesture with cross-fade)
- Bottom sheet for choice selection (optional: user preference between inline and sheet)
- Larger touch targets for choices (minimum 48dp)
- Haptic feedback on choice selection
- Reading progress indicator (how deep in the story tree)

### Exit Criteria

- User can read a completed story from root to ending
- SSE streaming displays text smoothly without jank
- Typewriter effect plays while waiting for content
- All choice types work (base, explored, pre-generated, custom)
- Custom choice creation works
- Back navigation returns to parent node with correct transition
- Ending state displays correctly
- Failed/generating states are handled
- All generation/error states display correctly with appropriate actions

---

## Stage 5: Audio Narration

**Goal**: Users can generate and listen to TTS narration for story nodes with voice selection and full media controls.

### Deliverables

- Audio generation: trigger TTS via API, handle quota exceeded
- Media3 ExoPlayer integration:
  - Audio playback with play/pause/seek
  - Lock screen controls (MediaSession)
  - Audio focus handling
  - Background playback support
  - Notification media controls
- Mini-player composable (Spotify-style): compact bar positioned **between content and bottom nav bar**
  - Play/pause, progress indicator, voice name, close
  - Tap to expand to full player (bottom sheet)
  - Persists across navigation while audio is playing (managed at scaffold level)
  - Content area adds bottom padding to prevent overlap
- Expanded player (optional bottom sheet or full dialog):
  - Seek bar, current time/duration
  - Volume control
  - Playback speed selector
  - Voice picker
- Voice picker: list of available voices with sample playback
- Audio cache: per-voice, per-node caching (generated audio URL tracking)
- State management: player state persists across node navigation, resets on explicit close
- Disabled states: node not complete, text too long (>3000 chars)

### Mobile-Specific Design

- Media notification with lockscreen art (world cover image)
- Audio ducking when other apps play audio
- Bluetooth/headphone controls
- Auto-pause on headphone disconnect

### Exit Criteria

- User can generate and play narration for a completed node
- Voice selection works with sample preview
- Media notification appears with playback controls
- Lock screen controls work
- Player state is managed correctly across navigation
- Disabled tooltips explain why narration is unavailable

---

## Stage 6: Story Map

**Goal**: Users can see a visual graph of their story tree and navigate to any explored node.

### Deliverables

- Custom graph visualization:
  - Compose Canvas rendering of node tree
  - Nodes as tappable composables (title, explored state, current node highlight)
  - Edges as path lines between parent-child nodes
  - Tree layout algorithm (horizontal or radial)
- Gesture support:
  - Pinch-to-zoom
  - Pan/scroll
  - Double-tap to reset view
- Node interaction: tap node → navigate to story reader at that node
- Auto-center on current/specified node (via navigation argument)
- Visual states: current node glow, explored nodes, unexplored nodes, generating nodes

### Mobile-Specific Design

- Full-screen (no bottom nav while in map)
- Pinch-to-zoom feels native (follow Android gesture conventions)
- Mini-map overview indicator (optional)
- Haptic feedback on node selection

### Exit Criteria

- Story tree is visualized correctly with proper layout
- Nodes are tappable and navigate to the reader
- Pan/zoom gestures work smoothly
- Current node is highlighted and auto-centered
- Performance is acceptable for trees with 50+ nodes

---

## Stage 7: Sharing & Social

**Goal**: World owners can manage sharing settings, create invite links, and share worlds with other users.

### Deliverables

- Share modal (bottom sheet on mobile):
  - Visibility selector (Private / Unlisted / Public)
  - Copy link button (clipboard + toast)
  - Invite link management (create, copy, delete)
  - Shared users list with remove capability
  - Confirmation dialogs for destructive actions (switch to private, remove user)
  - Auto-save with debounced mutations
  - Saving/Saved status indicators
- Android share intent integration: native share sheet with world link
- Deep link handling: `cosmonaut-ai.com/worlds/[id]` opens in app (App Links)
- Invite token processing: handle `?invite=` deep links to join shared worlds
- Non-owner view: read-only sharing info

### Mobile-Specific Design

- Bottom sheet instead of dialog (more natural on mobile)
- Native share sheet via Android Intent for "copy link" convenience
- Deep link verification (Android App Links with `assetlinks.json`)
- Haptic feedback on copy-to-clipboard

### Exit Criteria

- Owner can change world visibility
- Copy link works for public/unlisted worlds
- Invite links can be created, copied, and deleted for private worlds
- Users can be removed from shared list
- Changes auto-save correctly
- Deep links from web open in app
- Share intent launches native Android share sheet

---

## Stage 8: Subscription UI & Explorer Tier Change

**Goal**: Build all subscription-related UI for the Android app using an external-billing model (all transactions happen on cosmonaut-ai.com via Stripe — no in-app purchases), and implement the Explorer tier pricing/feature change across web and API.

> **Key policy constraint (see `04-google-play-external-billing.md`):**
> The Android app must be **consumption-only** — zero in-app purchase UI.
> If any in-app purchase flow exists anywhere, the consumption-only exemption is void for non-US users.

### Deliverables

#### 8A — Region-Aware Subscription CTAs

Every screen, dialog, or banner that references subscriptions or upgrades must render dynamically based on the user's region:

- **US users**: Clickable links/buttons pointing directly to cosmonaut-ai.com subscription and management pages (pricing, checkout, billing portal)
- **Non-US users**: Plain text only — e.g. *"Manage your subscription at cosmonaut-ai.com"* or *"Subscribe at cosmonaut-ai.com"* — with **no** clickable links to transactional pages

**Region detection strategy:**

1. Primary signal: Google Play Billing Library region/country code
2. Fallback: IP geolocation via API
3. Default (ambiguous/unavailable): Non-US behavior (text only) — this is the safe default

**Surfaces that require dynamic rendering:**

- Upgrade prompt dialogs (quota limit reached for worlds, nodes, audio)
- Subscription status banners (expiring, expired, payment issue)
- Settings → Subscription section (current plan, manage link)
- Any future surfaces that reference upgrading or subscription management

**Shared composable:**

- Build a reusable `SubscriptionCta` composable (or similar) that encapsulates the US/non-US logic so every call site renders correctly without duplicating the region check

#### 8B — Subscription Status & Usage UI

- **Usage tracking display**: worlds/nodes/audio used vs. limits, sourced from the existing `/auth/usage` API
- **Current plan indicator**: tier badge with period info
- **Subscription status banners**:
  - Pending cancellation: "Your plan expires on [date]"
  - Payment issue / past due
  - Downgrade pending
- **Quota enforcement**: disable actions (create world, make choice, generate audio) when at limit, showing the appropriate region-aware upgrade CTA
- **Post-upgrade handling**: invalidate usage cache when tier changes are detected (poll or observe on app foreground)

#### 8C — Explorer Tier Pricing & Feature Change (Web + API)

Modify the Explorer tier across the full stack:

| | Before | After |
|---|--------|-------|
| **Price** | $10/month | $3/month |
| **Nodes** | 500/month | 200/month |
| **Audio** | 30 narrations/month (resetting) | No audio included |
| **Audio carryover** | Audio counter reset on tier change | 10 lifetime audio generations persist across all tier changes |

**Audio carryover behavior (critical):**

- The 10 lifetime audio generations are an **account-level allowance**, not a tier-level feature
- When upgrading from Free → Explorer: `audio_narrations_used` must **not** be reset; the user keeps their remaining lifetime audio
- When downgrading from Explorer → Free: `audio_narrations_used` must **not** be reset; the user keeps their remaining lifetime audio
- Cosmonaut tier continues to have its own monthly audio pool (150/month, resetting)
- Explorer `audio_limit` becomes effectively 10 (lifetime, same pool as Free — never resets)

**Files requiring changes (non-exhaustive — verify with a fresh search at implementation time):**

- `cosmonaut-api/app/core/config.py` — `TIER_LIMITS["EXPLORER"]`: set `audio_limit` to `10`, align reset behavior
- `cosmonaut-api/app/services/usage.py` — `update_tier()`: do **not** reset `audio_narrations_used` when changing between FREE and EXPLORER; period rollover must skip audio reset for EXPLORER (same as FREE)
- `cosmonaut-api/app/services/email.py` — `_TIER_DISPLAY["EXPLORER"]`: update limit descriptions
- `cosmonaut-api/docs/audio-implementation.md` — tier table and prose
- `cosmonaut-web/src/lib/config/tiers.ts` — Explorer `price`, `audioNarrationsLimit`, `features` array
- `cosmonaut-web/src/routes/pricing/+page.svelte` — verify rendering with new tier data
- `cosmonaut-web/docs/audio-implementation.md` — tier table
- `cosmonaut-web/docs/subscription-frontend-guide.md` — Explorer references
- `cosmonaut-admin/src/lib/config.ts` — `TIER_LIMITS.EXPLORER`
- `README.md` — subscription table
- `ARCHITECTURE.md` — tier table
- `cosmonaut-infra/docs/audio-implementation.md` — tier table
- Stripe price IDs in `cosmonaut-infra/envs/dev/main.tf` and `cosmonaut-infra/envs/prod/main.tf` — **handled by user separately**

#### Explicitly Out of Scope

- **No pricing screen in the Android app** — displaying tier comparison cards with pricing would violate external billing policy for non-US users (it constitutes purchase-encouraging UI)
- **No Google Play Billing integration** — no `BillingClient`, no in-app purchase flow, no receipt verification, no RTDN webhooks, no dual-billing backend support
- **No Stripe price ID changes** — user handles these directly in Stripe and Terraform

### Exit Criteria

- Region detection correctly identifies US vs. non-US users
- US users see clickable links to cosmonaut-ai.com in all upgrade/subscription surfaces
- Non-US users see text-only messaging with no links to transactional pages
- Usage stats and limits display correctly for all tiers
- Quota prompts appear at correct thresholds with region-appropriate CTAs
- Subscription status banners render for payment issues, pending cancellation, etc.
- Explorer tier reflects $3/month and no audio across web, API, and admin
- The 10 lifetime audio generations persist correctly across Free ↔ Explorer tier changes
- Cosmonaut tier audio behavior is unchanged (150/month, resetting)

---

## Stage 9: Settings & Account

**Goal**: Users can manage their account, view subscription info, adjust preferences, and delete their account.

### Deliverables

- Settings screen with sections:
  - Account: email, username (read-only), sign-in method
  - Subscription: current plan, usage, manage link
  - Email Preferences: newsletter toggle
  - App Preferences (mobile-specific): theme mode (dark/light/system), reading font preferences
  - Danger Zone: delete account with confirmation dialog
- Feedback screen: category + message form
- About screen: app version, credits, links
- Terms & Privacy: either in-app rendering or open web browser to existing pages
- Sign out: clear all local data, navigate to login

### Mobile-Specific Design

- Settings list using Material 3 `ListItem` composables
- Toggle switches for preferences (native Material 3 Switch)
- Destructive actions require explicit confirmation (multi-step)
- App version and build info in "About" section

### Exit Criteria

- All settings sections display and function correctly
- Newsletter preference toggle persists
- Account deletion works with proper confirmation
- Sign out clears all local state and navigates to login
- Feedback can be submitted

---

## Stage 10: Polish, Accessibility & Performance

**Goal**: Elevate every interaction to Apple/Anthropic caliber quality. Audit accessibility. Optimize performance.

### Deliverables

- **Animations & Transitions**:
  - Shared element transitions between dashboard cards and world home
  - Smooth page transitions (slide, fade, cross-fade) via Navigation Compose
  - Staggered list entrance animations
  - Skeleton shimmer loading states
  - Choice button entrance animations (staggered fade-up)
  - Micro-interactions: button depth effects, ripple feedback, haptics
- **Accessibility Audit**:
  - TalkBack compatibility for all screens
  - Content descriptions for all images and icons
  - Focus management for screen readers
  - Keyboard/D-pad navigation support
  - `semantics` modifiers on all interactive composables
  - Text scaling support (user font size preference)
- **Performance Optimization**:
  - Profile with Android Studio profiler
  - Recomposition tracking (identify unnecessary recompositions)
  - Image loading optimization (proper sizes, disk cache policies)
  - Memory leak audit (LeakCanary in debug builds)
  - Startup time optimization (baseline profiles, avoid blocking main thread)
  - SSE streaming smoothness verification (no frame drops)
  - LazyList performance (proper key usage, item content types)
- **Reduced Motion Support**:
  - Respect `Settings.Global.ANIMATOR_DURATION_SCALE`
  - Disable non-essential animations when system animations are off
- **Dark Theme Polish**:
  - Verify all screens in dark theme for visual consistency

### Exit Criteria

- TalkBack can navigate every screen and perform every action
- No dropped frames during normal usage (verified with profiler)
- All animations feel fluid and purposeful
- Startup time under 1 second on mid-range device
- No memory leaks detected by LeakCanary
- Dark theme is visually polished across all screens

---

## Stage 11: Analytics, Monitoring & Error Tracking

**Goal**: Full observability matching the web app's Sentry + PostHog setup.

### Deliverables

- **Sentry Android SDK**:
  - Crash reporting with stack traces
  - ANR detection
  - Performance monitoring (transaction tracing)
  - Source map / ProGuard mapping upload for symbolication
  - User context (auth user ID, email)
- **PostHog Android SDK**:
  - Event tracking (matching web event names: `cta_clicked`, `story_ended`, `narration_started`, etc.)
  - User identification
  - Page/screen view tracking
  - Feature flags (if used in future)
  - Session replay (if supported on Android)
- **Custom Analytics Events** (parity with web):
  - `cta_clicked`, `checkout_initiated`, `checkout_completed`, `billing_portal_opened`
  - `random_prompt_used`, `narration_started`, `story_restarted`, `story_ended`
  - `world_shared`, `share_link_copied`, `onboarding_completed`

### Exit Criteria

- Crashes are reported to Sentry with proper symbolication
- Key user actions are tracked in PostHog
- Events match web event naming for cross-platform analytics
- User identity is set correctly after authentication

---

## Stage 12: Play Store Preparation & Launch

**Goal**: Prepare all assets, metadata, and compliance requirements. Execute a phased rollout.

### Deliverables

- **Store Listing**:
  - App title, short/full descriptions with keywords
  - Screenshots: 8 phone screenshots showcasing key flows
  - Feature graphic (1024×500)
  - App icon (512×512)
  - Promotional video (optional but recommended)
- **Compliance**:
  - Content rating questionnaire completion
  - Data safety section (full disclosure of data collection)
  - Privacy policy (link to existing cosmonaut-ai.com/privacy)
  - Target audience declaration (13+, family-friendly with guardian use)
  - COPPA compliance review
- **Build Configuration**:
  - Release signing key setup (Play App Signing)
  - ProGuard/R8 optimization and obfuscation
  - Android App Bundle (AAB) generation
  - Version code and version name strategy
  - Baseline profiles for startup optimization
- **Deep Links**:
  - App Links verification (`assetlinks.json` on cosmonaut-ai.com)
  - Deep link handling for world URLs and invite links
- **Launch Strategy**:
  1. Internal testing track (team only)
  2. Closed beta (invite 50-100 existing web users)
  3. Open beta (available to all, clearly marked as beta)
  4. Production release (staged rollout: 5% → 25% → 50% → 100%)
- **Post-Launch**:
  - Monitor crash rates (Sentry) — target <1% crash-free rate
  - Monitor ANR rates — target <0.5%
  - Monitor Play Store reviews and respond
  - Plan first patch release for launch-day issues

### Exit Criteria

- App approved on Play Store internal testing track
- Beta feedback incorporated
- Crash-free rate exceeds 99%
- Store listing is polished and complete
- Deep links work correctly from web
- Staged rollout begins

---

## Dependencies & Sequencing

```
Stage 1 (Foundation)
    │
    ├── Stage 2 (Auth)
    │       │
    │       ├── Stage 3 (Dashboard + Worlds)
    │       │       │
    │       │       ├── Stage 4 (Story Reader) ★ Critical Path
    │       │       │       │
    │       │       │       ├── Stage 5 (Audio)
    │       │       │       └── Stage 6 (Story Map)
    │       │       │
    │       │       └── Stage 7 (Sharing)
    │       │
    │       ├── Stage 8 (Billing)
    │       └── Stage 9 (Settings)
    │
    └── Stage 11 (Analytics) — can start early, enriched as features land
    
Stage 10 (Polish) — continuous, intensifies after Stage 9
Stage 12 (Testing) — continuous, intensifies after Stage 9
Stage 13 (Launch) — after all other stages
```

**Critical path**: 1 → 2 → 3 → 4 (Story Reader is the core value prop and must be highest quality)

**Parallelizable after Stage 4**: Stages 5, 6, 7 can be developed concurrently once the reader exists.

**Parallelizable after Stage 2**: Stages 8, 9 can be developed concurrently once auth exists.

---

## Risk Register


| Risk                                       | Likelihood | Impact | Mitigation                                                                            |
| ------------------------------------------ | ---------- | ------ | ------------------------------------------------------------------------------------- |
| External billing policy compliance        | Medium     | High   | Region detection must be robust; default to non-US (safe); test with Play Store review |
| SSE streaming performance issues           | Medium     | High   | Token batching architecture; test on low-end devices early                            |
| Story map (Compose Canvas) complexity      | High       | Medium | Start with simple tree layout; iterate on zoom/pan gestures; allocate extra time      |
| Cognito SDK compatibility issues           | Low        | High   | Test auth integration in Stage 2 before building on it                                |
| Play Store content review delays           | Medium     | Medium | Submit early, use test tracks, respond quickly to feedback                            |
| COPPA compliance complexity                | Medium     | Medium | Legal review; may need separate kids' profile flow                                    |
| Explorer tier change cross-stack consistency | Medium     | Medium | Comprehensive file audit at implementation time; verify audio carryover with integration tests |


---

## Success Metrics

- **Launch**: App live on Play Store within agreed timeline
- **Quality**: 4.5+ star rating target, <1% crash rate
- **Parity**: All critical web features available in Android app
- **Performance**: <1s cold start, 60fps UI, smooth SSE streaming
- **Accessibility**: TalkBack fully functional, WCAG 2.1 AA compliance

