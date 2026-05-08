# Onboarding Carousel Redesign

> Design document for the redesigned first-launch intro flow.

## Problems with Current Design

1. **Reads like a feature ad**: "Interactive Stories", "Branching Narratives", "Audio Narration" — lists features rather than explaining the product
2. **Generic illustrations**: Reuses art assets from other screens (astronaut, empty-state, sunset) that don't visually connect to the content
3. **No brand identity**: Doesn't establish what Cosmonaut IS before diving into features
4. **Mismatched messaging**: Doesn't align with the web landing page's much stronger copy

## Web Landing Page Messaging (Source of Truth)

The web landing has a clear narrative arc:

- **Hero**: "WELCOME TO COSMONAUT" → "Describe any world... Cosmonaut builds it into a branching story shaped by your choices."
- **How it works** (3 steps):
  1. **Describe your story** (Sparkles icon) — "A noir mystery in 1940s Chicago" or "A space station hiding dark secrets" — you set the stage, the AI fills in the details.
  2. **Every choice branches** (GitBranch icon) — Decisions create new paths. Write custom actions, explore unexpected directions.
  3. **See the whole picture** (Eye icon) — A visual map shows every path through your story.
- **Final CTA**: "What story are you building?" → "Create Your Story"

## Redesigned Flow (3 Pages)

### Page 1: Welcome

**Goal**: Establish brand identity and intrigue. Make the user feel like they've entered something special.

- **Background**: Animated starfield (ported from web's Canvas implementation)
- **Content**:
  - Cosmonaut logo (large, centered)
  - "COSMONAUT" in Orbitron font, gold on dark
  - Tagline: "Custom interactive stories for you & your family"
  - Subtle radial glow behind logo
- **Visual style**: Minimal, cinematic, breathtaking
- **No description of features** — just mood-setting

### Page 2: How It Works

**Goal**: Explain the product in 3 clear steps. Match the web's "How it works" section exactly.

- **Background**: Continued starfield
- **Header**: "HOW IT WORKS" (small caps, primary color, tracking-widest)
- **Subheader**: "Three steps to infinite stories" (matching web)
- **Three steps** (vertically stacked with icons):
  1. **Describe your story** — Icon in glowing container → "You set the stage, the AI fills in the details"
  2. **Every choice branches** — Icon → "Decisions create new paths with every click"
  3. **See the whole picture** — Icon → "A visual map of every path through your story"
- **Visual style**: Clean, icon-driven, readable

### Page 3: Begin

**Goal**: Action-oriented final screen. Create urgency and excitement.

- **Background**: Starfield with stronger glow
- **Header**: "What story are you building?" (matching web's final CTA)
- **Subheader**: "One prompt is all it takes" (matching web)
- **Illustration**: Astronaut + floating elements (placeholder for custom asset)
- **CTA**: "Begin Your Journey" (matching web's hero CTA)

## Technical Approach

### StarfieldComposable
- Canvas-based animated starfield matching the web implementation
- Twinkling stars with varying size, opacity, and speed
- Radial gradient nebula effects (gold-tinted to match brand)
- Respects reduced motion settings
- Reusable composable for potential use on login screen too

### Animations
- Staggered fade-up entrance for text elements on each page
- Cross-fade between pages via HorizontalPager
- Floating animation on astronaut illustration (matching web)
- Page indicator with animated transitions

### Assets Used
1. **Hero astronaut** (`art_hero_astronaut.png`): Transparent-background floating astronaut from web hero — identical asset
2. **Planets** (`art_planet1.webp`, `art_planet2.webp`, `art_planet3.webp`): Three planets floating at different rates behind astronaut — matching web hero parallax composition
3. **CTA image** (`art_tier_cosmonaut.webp`): Astronaut riding a rocket — dynamic action-oriented image for final page
4. **Step icons**: Material Icons Extended (AutoAwesome, AccountTree, Visibility)

## Design Principles Applied

1. **Tell, don't sell**: Explain what the product is, not why it's great
2. **Match the web**: 1:1 messaging parity with the landing page
3. **Progressive disclosure**: Brand → How → Action
4. **Visual quality**: Animated backgrounds, staggered animations, premium typography
5. **Minimal text**: Short, punchy copy that can be absorbed in seconds
