## Google Play External Billing Policy — Cosmonaut Implementation Brief

### Core Strategy
The Cosmonaut Android app must be **consumption-only**: no in-app purchase UI of any kind. Users log in and consume content; all subscription transactions happen on cosmonaut-ai.com via Stripe.

---

### US Users
**Allowed:**
- Tell users a subscription is available on the website
- Include a direct clickable link to cosmonaut-ai.com (including checkout/pricing pages)
- Mention pricing, tiers, and promotions
- Link directly to subscription management/cancellation pages

**Not allowed:**
- Nothing relevant — US users have full freedom as of October 29, 2025 (Epic v. Google injunction)

---

### Non-US Users
**Allowed:**
- Display text informing users that subscriptions are managed on the website (e.g. *"Manage your subscription at cosmonaut-ai.com"* or *"Subscribe at cosmonaut-ai.com"*)
- Link to non-transactional pages: account management, help center, privacy policy — **as long as those pages don't lead to a purchase flow**
- Any communication *outside* the app (email, push notifications) can include direct links and pricing

**Not allowed:**
- Direct links to checkout, pricing, or any page that leads to a purchase
- Any language that actively encourages purchasing outside the app (e.g. "Get a better deal on our website")
- Any in-app purchase UI whatsoever — even a single "Subscribe" button that goes anywhere

---

### Determining User Location
Google's policy applies based on **where the user is served**, not their account region. Acceptable signals for determining US vs. non-US:

- **Device locale / region setting** (`Locale.getDefault()`) — fast and easy, but spoofable
- **Google Play's own billing region** — most authoritative; accessible via the Play Billing Library's `BillingClient`
- **IP geolocation** — reasonable fallback, but not bulletproof (VPNs, etc.)
- **Time zone** — weakest signal, use only as a supplement

**Recommended approach:** Use the Play Billing Library's region/country code as the primary signal, with IP geolocation as a fallback. If the signal is ambiguous or unavailable, **default to non-US behavior** (text only, no links to transactional pages). This is the safe default.

Do **not** rely solely on device locale — a US user traveling abroad or a non-US user with a US locale setting could be misclassified in either direction.

---

### The One Rule That Governs Everything
If the app has **any** in-app purchase flow anywhere — even for a single SKU, even hidden behind a flag — the consumption-only exemption is void for non-US users and the full Google Play Billing requirement kicks in. Keep the app 100% purchase-free.