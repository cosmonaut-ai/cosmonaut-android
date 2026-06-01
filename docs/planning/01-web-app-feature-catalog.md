# Cosmonaut Web App — Complete Feature Catalog

> Reference document for the Android port. Describes every screen, feature, interaction, and design detail of the existing SvelteKit web application.
>
> **Source file paths are relative to `cosmonaut-web/`** — use them to read the actual implementation for styling, logic, and interaction details.

---

## 1. Application Identity

- **Name**: Cosmonaut
- **Tagline**: "Custom interactive stories for you & your family"
- **Domain**: cosmonaut-ai.com
- **Creator**: Matson Software LLC
- **Brand Fonts**: Inter (body), Orbitron (display/logo), JetBrains Mono (code/mono)
- **Theme**: Dark-first (default dark mode), space/cosmos motif
- **Primary Color (dark)**: Gold/amber — `oklch(0.9536 0.0872 97.9082)` ≈ warm gold
- **Primary Color (light)**: Indigo/blue — `oklch(0.6377 0.1202 260.4911)`
- **Icon Set**: Lucide (clean, consistent line icons)
- **UI Framework**: shadcn/bits-ui primitives with Tailwind CSS v4

### Key Design System Files

- **Theme / CSS tokens (colors, fonts, shadows, radii)**: `src/routes/layout.css`
- **Tailwind + Vite config**: `vite.config.ts`, no `tailwind.config.js` (Tailwind v4 config is in CSS)
- **shadcn component registry**: `components.json`
- **Utility (`cn()`, class merge)**: `src/lib/utils.ts`

---

## 2. User Flows

### 2.1 Unauthenticated Flows

#### Landing Page (`/`)

> **Source**: `src/routes/+page.svelte`
> **Components**: `src/lib/components/features/landing/Hero.svelte`, `DemoStory.svelte`, `Features.svelte`, `Starfield.svelte`

- Animated **Starfield** background (canvas-based particle animation)
- **Sticky header** with logo + "Get Started" CTA (transparent → blur-bg on scroll)
- **Hero section**: headline, subtext, CTA button
- **Demo Story section**: interactive embedded demo of the story experience
- **Features section**: feature cards showcasing capabilities
- **Final CTA section**: scroll-reveal animated call to action (see `intersectionReveal` utility at `src/lib/utils/intersectionReveal.ts`)
- **Footer**: links to About, Terms, Privacy, Pricing

#### Login Page (`/login`)

> **Source**: `src/routes/login/+page.svelte`
> **Auth flow hook**: `src/lib/auth/useLoginFlow.svelte.ts`
> **Form components**: `src/lib/components/features/auth/SignInForm.svelte`, `SignUpForm.svelte`, `VerifyForm.svelte`, `ForgotPasswordForm.svelte`, `AccountSuspendedNotice.svelte`
> **Art assets**: `static/art/sign-in-astronaut.webp`, `static/art/sign-in-doorway.webp`

- **Two-panel layout** (desktop): left panel = astronaut/doorway illustration with random tagline, right panel = form
- **Mobile**: form only, vertically centered
- **Auth views** (state machine):
    - **Sign In**: email + password, "Sign in with Google" button, links to Sign Up and Forgot Password
    - **Sign Up**: email + password + confirm password, password strength indicators (8+ chars, uppercase, lowercase, number), "Sign in with Google"
    - **Verify**: 6-digit code input after sign-up, resend code option
    - **Forgot Password**: email input → code + new password reset flow
    - **Account Suspended**: informational notice (no actions)
- **In-app browser detection**: warns users when accessing from embedded browsers (social media apps) — see `src/lib/utils/in-app-browser.ts`
- **Redirect handling**: saves intended URL pre-login, redirects after successful auth — see `src/lib/auth/redirect.ts`
- Footer links: Terms, Privacy, About

#### OAuth Callback (`/callback`)

> **Source**: `src/routes/callback/+page.svelte`

- Handles Cognito OAuth code exchange
- Refreshes session, handles suspended users
- Redirects to saved redirect URL or dashboard

### 2.2 Authenticated Flows

#### Onboarding (`/onboarding`)

> **Source**: `src/routes/onboarding/+page.svelte`
> **Guard**: `src/lib/components/shared/OnboardingGuard.svelte`
> **API**: `src/lib/api/subscription.ts` (`checkUsernameAvailability`, `updateNewsletter`)

- Shown once after first sign-up (guarded by `OnboardingGuard` component)
- **Username selection**: alphanumeric, 3-30 chars, real-time availability check with debounce
- **Newsletter opt-in**: toggle switch
- **Age verification**: checkbox confirming 13+ years old (required)
- **Submit**: sets username, opts into newsletter, redirects to dashboard

#### Dashboard (`/dashboard`)

> **Source**: `src/routes/dashboard/+page.svelte`
> **Components**: `src/lib/components/features/worlds/WorldCard.svelte`, `WorldCardSkeleton.svelte`, `FeaturedWorldsCarousel.svelte`
> **Subscription**: `src/lib/components/features/subscription/SubscriptionStatusBanner.svelte`, `UsageLimitTooltip.svelte`, `UpgradePrompt.svelte`
> **Queries**: `src/lib/queries/worlds.ts` (`useWorlds`, `useDeleteWorld`), `src/lib/queries/subscription.ts` (`useUser`)
> **Art**: `static/art/no-worlds-astronaut.webp`

- **Subscription Status Banner**: warnings for payment issues, cancellation, paused subscriptions
- **Featured Worlds Carousel**: highlighted/public worlds
- **"Your Stories" section**:
    - Header with "Create Story" button (disabled with tooltip if at world limit)
    - **World cards grid** (responsive: 1/2/3 columns):
        - Cover image (or animated gradient placeholder, or shimmer for pending image)
        - Title, genre badge, description (2-line clamp)
        - Status badge (generating, completed, failed — with pulse animation for generating)
        - Content filter icons (shield icons for moderate/strict)
        - Footer: creation date, story length, action buttons (Delete, Play/Continue)
        - **Hover effects**: lift + glow + primary border
        - **Entrance animation**: staggered fade-up (see `WorldCard.svelte` `<style>` block for keyframes)
    - **Delete confirmation dialog**: different copy for owner vs. shared user
    - **Play button**: uses the dashboard session summary to resume from `last_visited_node_id` or the embedded world's `root_node_id`
    - **Empty state**: astronaut illustration with floating animation, glow effect, CTA
    - **Error state**: destructive card with retry button
    - **Loading state**: skeleton cards (3 placeholders)
    - **Pagination**: "Load More" button for infinite scroll via TanStack Query
- **Upgrade prompt modal**: when at world creation limit

#### Create World (`/worlds/new`)

> **Source**: `src/routes/worlds/new/+page.svelte`
> **Mutations**: `src/lib/queries/worlds.ts` (`useCreateWorld`)
> **Custom control**: `src/lib/components/shared/SegmentedControl.svelte`
> **Visibility**: `src/lib/components/shared/VisibilitySelect.svelte`
> **Prompts file**: `static/story-prompts.txt`
> **Preference storage**: `src/lib/utils/storage.ts`

- **Sub-header**: Back button + "Create a New Story" with rocket icon
- **Quota alert**: warning banner when at world limit with link to pricing
- **Form card**:
    - **Story Prompt**: multiline textarea (mono font), 2000 char max with counter, validation on submit
    - **Random Prompt button**: loads from `/story-prompts.txt`, dice icon
    - **Visibility selector**: Private / Unlisted / Public dropdown
    - **"More settings" accordion**:
        - **Story Length**: segmented control (Short/Medium/Long) with descriptions, persisted to localStorage
        - **Vocabulary Level**: segmented control (Child/Teen/Adult), persisted
        - **Content Filter**: segmented control (None/Moderate/Strict), persisted
    - **Actions**: Cancel + Create Story (with loading spinner)

#### World Home (`/worlds/[worldId]`) and Session Home (`/sessions/[sessionId]`)

> **Layout**: `src/routes/worlds/[worldId]/+layout.svelte`
> **Page**: `src/routes/worlds/[worldId]/+page.svelte`
> **Components**: `src/lib/components/features/worlds/WorldHomePage.svelte`, `WorldHeroSection.svelte`, `WorldQuickActions.svelte`, `WorldDetailsSection.svelte`, `WorldGenerationProgress.svelte`, `WorldGenerationFailed.svelte`
> **World context**: `src/lib/contexts/world.ts`

- **Layout wrapper**: loads world data with polling for generation status
- **Generation states**:
    - **In Progress**: `WorldGenerationProgress` component with status updates
    - **Failed**: `WorldGenerationFailed` component with retry/delete options
    - **Completed**: `WorldHomePage` component
- **WorldHomePage** (when complete):
    - **Hero section**: world image, title, description, genre
    - **Quick actions**: Continue/Start Story, View Map, Share
    - **Details section**: characters, locations, settings, narrative context
    - **Footer metadata**: created/updated dates, genre, length, vocab level, content filter, node text length
- **Invite token handling**: processes `?invite=` URL parameter for shared worlds
- **Playthrough start/resume**: root world pages call `POST /worlds/{worldId}/sessions`; session pages read `GET /sessions/{sessionId}`
- **Shared session handoff**: inaccessible session links call `GET /sessions/{sessionId}/handoff` and redirect to the root world when allowed

#### Story Node Reader (`/sessions/[sessionId]/nodes/[nodeId]`)

> **Page**: `src/routes/sessions/[sessionId]/nodes/[nodeId]/+page.svelte`
> **Core component**: `src/lib/components/features/stories/StoryNodeView.svelte`
> **Story card**: `src/lib/components/features/stories/StoryCard.svelte` — typewriter, text rendering, ending state
> **Choice list**: `src/lib/components/features/stories/ChoiceList.svelte` — choice buttons, custom choice input
> **Streaming hook**: `src/lib/components/features/stories/useStreamingNode.svelte.ts` — SSE connection, token buffering
> **Choice execution hook**: `src/lib/components/features/stories/useChoiceExecution.svelte.ts` — choose endpoint logic
> **Transition**: `src/lib/components/features/stories/SlideTransition.svelte`
> **API**: `src/lib/api/nodes.ts` (`chooseOption`, streaming endpoints)
> **Art**: `static/art/ending-sunset.webp`

- **Toolbar** (top):
    - Left: Undo/Back button (go to parent node)
    - Right: Map button, Audio narration toggle, Share button
- **Story Card** (main content):
    - **Parent choice context**: "You chose: [label]" with arrow icon in highlighted box
    - **Story text**: prose formatting with paragraph breaks, italic emphasis via `*text*` markup
    - **Streaming state**: typewriter "flavor text" while waiting for first token (rotating phrases like "Weaving the threads of fate..."), gold blinking cursor during streaming
    - **Generating state**: loading indicator when another session is generating
    - **Failed state**: error card with Retry + Go Back + Dashboard buttons
    - **Wrong session state**: informational card directing to map or restart
- **Choice List** (after story text):
    - Header: "What do you do?"
    - **Base choices**: numbered buttons with hover effects (lift + primary border + glow), staggered entrance animation
    - **Explored choices**: dimmed appearance with checkmark instead of number
    - **Pre-generated ("Quick") choices**: rabbit badge
    - **Custom choices**: "Custom" badge with creator display name
    - **Quota limit state**: disabled choices with "View Plans" link
    - **Custom choice input**: "Or write your own action..." textarea (200 char max) with "Take Action" submit
- **Slide transitions**: left/right slide animation when navigating between nodes (forward = right-to-left, back = left-to-right)
- **Ending state**: sunset illustration with "This path has ended" overlay + "Start Over" button
- **Scroll reset**: auto-scrolls to top when navigating to new node
- **Upgrade prompts**: modals for node quota and audio quota limits

#### Audio Narration System

> **Main component**: `src/lib/components/features/narrator/AudioNarration.svelte`
> **Player bar**: `src/lib/components/features/narrator/AudioPlayerBar.svelte`
> **Voice picker**: `src/lib/components/features/narrator/VoicePicker.svelte`
> **Player hook**: `src/lib/components/features/narrator/useAudioPlayer.svelte.ts`
> **API**: `src/lib/api/voices.ts`, `src/lib/queries/voices.ts`, audio generation in `src/lib/queries/nodes.ts`

- **Speaker icon toggle** in story toolbar
- **Disabled states**: tooltip explaining why (node not complete, text too long >3000 chars)
- **Voice selection**: VoicePicker popover with voice samples (pauses main audio during preview)
- **Audio player bar** (bottom fixed):
    - Play/pause, seek slider, current time / duration
    - Volume control with mute toggle
    - Playback rate control
    - Voice picker
    - Close button
- **Generation**: on-demand TTS via API, cached per voice per node
- **Auto-play**: plays immediately after generation completes
- **State management**: resets on node navigation, cleanup on component destroy

#### Story Map (`/sessions/[sessionId]/graph`)

> **Page**: `src/routes/sessions/[sessionId]/graph/+page.svelte`
> **Graph component**: `src/lib/components/shared/StoryGraph.svelte`
> **Node renderer**: `src/lib/components/shared/FlowNode.svelte`
> **Transform utility**: `src/lib/utils/nodeTransform.ts`
> **Node queries**: `src/lib/queries/nodes.ts` (`useSessionNodes`)

- **XYFlow (SvelteFlow)** graph visualization
- Custom `FlowNode` components for each story node
- Nodes/edges derived from story node tree via `nodeTransform` utility
- Click node to navigate to reader
- Supports `?node=` query param to highlight/center on specific node

#### Settings (`/settings`)

> **Page**: `src/routes/settings/+page.svelte`
> **Components**: `src/lib/components/features/subscription/AccountSection.svelte`, `SubscriptionSection.svelte`, `DangerZone.svelte`

- **Account Section**: user profile information
- **Subscription Section**: current plan, usage stats, upgrade/manage links
- **Email Preferences**: newsletter toggle with autosave
- **Danger Zone**: delete account with confirmation

#### Pricing (`/pricing`)

> **Page**: `src/routes/pricing/+page.svelte`
> **Components**: `src/lib/components/features/subscription/PricingCard.svelte`
> **Tier config**: `src/lib/config/tiers.ts`
> **Mutations**: `src/lib/queries/subscription.ts` (`useCheckout`, `useBillingPortal`)

- **Three-tier grid** (responsive 1/3 columns):
    - **FREE**: 3 worlds/7 days, 30 nodes/7 days, 10 audio (lifetime)
    - **EXPLORER**: $X/mo, 20 worlds/30 days, 500 nodes/30 days, 30 audio/30 days
    - **COSMONAUT**: $X/mo, 100 worlds/30 days, 2000 nodes/30 days, 150 audio/30 days
- **Pricing cards**: feature lists, current plan indicator, upgrade/manage CTA
- **Stripe integration**: checkout redirect for new subscriptions, billing portal for existing
- **Post-checkout handling**: success/cancelled toast messages, query cache invalidation

#### Sharing System (ShareModal)

> **Component**: `src/lib/components/features/worlds/ShareModal.svelte`
> **Visibility control**: `src/lib/components/shared/VisibilitySelect.svelte`
> **Autosave hook**: `src/lib/utils/useAutosave.svelte.ts`
> **Mutations**: `src/lib/queries/worlds.ts` (`useUpdateWorldSharing`, `useInviteToken`, `useCreateInviteToken`, `useDeleteInviteToken`)

- **Visibility control**: Private / Unlisted / Public selector (owner only)
- **Link sharing**: copy-to-clipboard for public/unlisted worlds
- **Invite links** (private worlds, owner only):
    - Create/delete invite links (24-hour expiry)
    - Copy invite URL with expiry countdown
- **Shared users list**: badges with remove button + confirmation dialog
- **Auto-save**: changes saved automatically with debounce, "Saving..."/"Saved" indicators
- **Non-owner view**: read-only notice

#### Feedback (`/feedback`)

> **Page**: `src/routes/feedback/+page.svelte`
> **API**: `src/lib/api/feedback.ts`, `src/lib/queries/feedback.ts`

- Category selection
- Message textarea
- Submit via API mutation

#### Static Pages

- **About** (`/about`): `src/routes/about/+page.svelte` — marketing/about page, prerendered for SEO
- **Terms** (`/terms`): `src/routes/terms/+page.svelte` — terms of service, prerendered
- **Privacy** (`/privacy`): `src/routes/privacy/+page.svelte` — privacy policy, prerendered

### 2.3 Global UI Components

#### Root Layout

> **Source**: `src/routes/+layout.svelte` — global layout with auth guards, header, footer, providers
> **Layout loader**: `src/routes/+layout.ts` — disables SSR, sets trailing slash, calls `initializeAuth()`

#### Header

> **Source**: rendered inline in `src/routes/+layout.svelte` (authenticated header) and `src/routes/+page.svelte` (landing header)

- **Landing page**: transparent → blur on scroll, logo + "Get Started" / "Dashboard" CTA
- **Authenticated pages**: solid bg with border, logo (links to dashboard), UserMenu or Sign In button
- **Hidden on**: login, onboarding pages

#### UserMenu

> **Source**: `src/lib/components/shared/UserMenu.svelte`

- Avatar/initials dropdown
- Links to Settings, Logout

#### Footer (AppFooter)

> **Source**: `src/lib/components/shared/AppFooter.svelte`

- Links to About, Terms, Privacy, Pricing
- Hidden on: login, callback, onboarding, graph/map pages

#### OnboardingGuard

> **Source**: `src/lib/components/shared/OnboardingGuard.svelte`

- Redirects unauthenticated or un-onboarded users to appropriate page

#### Toast System (Sonner)

> **Source**: `src/lib/components/ui/sonner/sonner.svelte`
> **Helpers**: `src/lib/utils/toast.ts` (`showSuccess`, `showError`, `showWarning`, `showInfo`)

- Success, error, warning, info toasts
- Rich colors, auto-dismiss

#### SEO Component

> **Source**: `src/lib/components/shared/SEO.svelte` (not needed on Android, but useful for understanding metadata)

---

## 3. Design Language

### 3.1 Visual Style

> **Theme tokens**: `src/routes/layout.css` — all color values, shadow definitions, font declarations
> **Button depth system**: `src/routes/layout.css` lines 220-247 — `data-slot="button"` + `data-variant` selectors with `box-shadow` depth and hover/active transforms
> **Art assets directory**: `static/art/` — all illustrations in WebP format

- **Space/cosmos theme**: dark backgrounds, star fields, astronomical imagery
- **Art assets**: custom illustrations (astronaut, doorway, sunset, planet) in WebP format
- **Glow effects**: radial gradient glows on primary actions (oklch color blending)
- **Glass morphism**: card backgrounds with `backdrop-blur-sm`, semi-transparent borders
- **Button depth**: 3D shadow effect on non-ghost/link buttons (shadow shifts on hover/active)
- **Animations**: entrance fade-ups, staggered delays, hover lifts, gradient shifts, shimmer loading
- **Reduced motion**: all animations respect `prefers-reduced-motion`

### 3.2 Component Patterns (shadcn/bits-ui)

> **All UI primitives live in**: `src/lib/components/ui/` — each subfolder is one component type
> **Variant system**: components use `tailwind-variants` (`tv()`) for variant definitions — see `button/button.svelte`, `badge/badge.svelte` for examples
> **Custom shared components**: `src/lib/components/shared/` — app-specific composites (SegmentedControl, VisibilitySelect, FlowNode, etc.)
> **Feature components**: `src/lib/components/features/` — organized by domain (auth, landing, narrator, stories, subscription, worlds)

- **Cards**: `ui/card/` — rounded corners (`0.5rem` radius), subtle borders, card-bg color
- **Badges**: `ui/badge/` — pill-shaped, variant colors (default, outline, secondary, destructive)
- **Buttons**: `ui/button/` — variants: default, destructive, outline, secondary, ghost, link; sizes: default, sm, lg, icon, icon-sm
- **Forms**: `ui/input/`, `ui/textarea/`, `ui/label/` — label + input/textarea + helper text, validation error states
- **Segmented controls**: `shared/SegmentedControl.svelte` — pill-style tab selector with active indicator
- **Dialogs/modals**: `ui/dialog/` — overlay + centered content with header/footer
- **Alert dialogs**: `ui/alert-dialog/` — destructive confirmations with cancel/action buttons
- **Tooltips**: `ui/tooltip/` — hover-triggered, positioned content
- **Dropdowns**: `ui/dropdown-menu/` — animated open/close, checkable items
- **Skeletons**: `ui/skeleton/` — pulse-animated placeholders during loading
- **Spinners**: `ui/spinner/` — rotating circular indicator
- **Select**: `ui/select/` — styled dropdown selector
- **Switch**: `ui/switch/` — toggle switch
- **Accordion**: `ui/accordion/` — expandable sections
- **Popover**: `ui/popover/` — floating positioned content

### 3.3 Responsive Breakpoints

- Mobile-first design approach
- `sm`: 640px — 2-column grids, show/hide elements
- `md`: 768px — side-by-side layouts (login panels), 3-column grids
- `lg`: 1024px — larger grids, wider content

### 3.4 Typography

- Body: Inter, clean sans-serif
- Display/Brand: Orbitron, geometric/space-themed
- Mono: JetBrains Mono (prompt input, code-like elements)
- Prose: Tailwind Typography plugin for story text rendering

---

## 4. Technical Integration Points

### 4.1 Authentication (AWS Cognito via Amplify)

> **Auth module**: `src/lib/auth/auth.svelte.ts` — main auth state, all auth methods, token management
> **Amplify wrapper**: `src/lib/auth/amplify.ts` — Amplify SDK calls
> **Auth types**: `src/lib/auth/types.ts`
> **Error handling**: `src/lib/auth/errors.ts`
> **Redirect helpers**: `src/lib/auth/redirect.ts`
> **Config**: `src/lib/config.ts` — Cognito pool IDs, environment detection

- Email/password sign-up + sign-in
- Google OAuth via `signInWithRedirect`
- JWT tokens (ID token sent as Bearer)
- Session management with auto-refresh
- Streaming session (CloudFront signed cookies)

### 4.2 API Layer

> **Core request handler**: `src/lib/api/core.ts` — `apiRequest()`, `getAuthHeaders()`, `parseApiError()`, constants (`POLL_INTERVAL_MS`, `POST_STREAM_DELAY_MS`)
> **Auth retry**: `src/lib/api/fetchWithAuthRetry.ts`
> **Worlds API**: `src/lib/api/worlds.ts`
> **Nodes API**: `src/lib/api/nodes.ts`
> **Subscription API**: `src/lib/api/subscription.ts`
> **Voices API**: `src/lib/api/voices.ts`
> **Feedback API**: `src/lib/api/feedback.ts`
> **TanStack Query client**: `src/lib/queries/client.ts`
> **Query keys**: `src/lib/queries/keys.ts`
> **Query hooks**: `src/lib/queries/worlds.ts`, `nodes.ts`, `subscription.ts`, `voices.ts`, `feedback.ts`

- RESTful API at `api.cosmonaut-ai.com`
- JWT Bearer auth on all authenticated endpoints
- PostHog correlation headers (`X-PostHog-Distinct-Id`, `X-PostHog-Session-Id`)
- Auto-retry on 401 with token refresh
- SSE streaming for story text generation
- Polling for world generation status (2s intervals, 120 max attempts)

### 4.3 State Management

- **Server state**: TanStack Query (worlds, nodes, user/subscription, voices)
- **Auth state**: Svelte 5 runes (module-level `$state`) — see `src/lib/auth/auth.svelte.ts`
- **Context**: Svelte context API for world data sharing in route tree — see `src/lib/contexts/world.ts`
- **URL state**: query params for navigation state (`?node=`, `?invite=`, `?checkout=`)
- **Local storage**: user preferences (world length, vocab, content filter), auth tokens — see `src/lib/utils/storage.ts`

### 4.4 External Services

- **Stripe**: checkout session creation, billing portal redirect
- **ElevenLabs**: TTS audio generation (via API proxy)
- **Sentry**: error tracking and monitoring — init in `src/hooks.client.ts`
- **PostHog**: product analytics — init in `src/hooks.client.ts`, helpers in `src/lib/utils/analytics.ts`

### 4.5 Key Data Models

> **All TypeScript types**: `src/lib/types/api.ts` — World, StoryNode, Choice, ApiError, etc.
> **Subscription types**: `src/lib/types/subscription.ts`

- **World**: id, title, description, genre, generation_status, images, settings (length, vocab, filter), sharing config, characters, locations, potential_endings
- **StoryNode**: id, world_id, title, text, choices[], parent_id, parent_choice, ancestors[], generation_status, audio{}
- **Choice**: label, outcome, target (node id), is_created, is_explored, is_custom, creator info
- **UserUsage**: tier, worlds_created/limit, nodes_used/limit, audio_used/limit, period_end, newsletter, is_onboarded, subscription_status

---

## 5. Feature Priority Matrix

| Feature                            | User Impact   | Complexity | Notes                                     |
| ---------------------------------- | ------------- | ---------- | ----------------------------------------- |
| Auth (email + Google)              | Critical      | Medium     | Must-have for launch                      |
| Dashboard + World List             | Critical      | Medium     | Core navigation hub                       |
| Create World                       | Critical      | Low-Medium | Form with options                         |
| Story Reader + Streaming           | Critical      | High       | SSE streaming, transitions, typewriter    |
| Choice Selection + Custom          | Critical      | Medium     | Core gameplay mechanic                    |
| World Home                         | High          | Medium     | World details + progress                  |
| Story Map/Graph                    | High          | High       | Complex graph visualization               |
| Audio Narration                    | High          | High       | TTS generation, player, voice selection   |
| Sharing + Invites                  | Medium        | Medium     | Visibility, invite links, user management |
| Settings                           | Medium        | Low        | Account, subscription, preferences        |
| Pricing + Stripe                   | Medium        | Medium     | Tier cards, checkout, billing portal      |
| Onboarding                         | Medium        | Low        | Username, newsletter, age gate            |
| Landing Page                       | Low (for app) | Medium     | May not need full port for mobile         |
| Feedback                           | Low           | Low        | Simple form                               |
| Static Pages (About/Terms/Privacy) | Low           | Low        | Can link to web versions                  |
