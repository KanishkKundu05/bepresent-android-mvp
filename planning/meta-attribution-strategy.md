# Meta Attribution & Conversion Signal Strategy

## Overview

Dual-layer attribution architecture combining the **Meta SDK** (client-side) with the **Conversions API** (server-side) to maximize signal quality for Meta ad optimization. Meta deduplicates overlapping events via `event_id`, so running both layers is the recommended approach.

---

## Architecture

```
+─────────────────────────────────────────────────────────────────────────+
│                         ANDROID APP (Client)                            │
│                                                                         │
│  ┌──────────────┐    ┌──────────────────┐    ┌───────────────────────┐  │
│  │   Meta SDK    │    │   Auth0 Login     │    │   Stripe Checkout     │  │
│  │              │    │                  │    │                       │  │
│  │ • app_open   │    │ • email          │    │ • payment_intent      │  │
│  │ • GAID       │    │ • name           │    │ • subscription_id     │  │
│  │ • install    │    │ • user_id        │    │                       │  │
│  └──────┬───────┘    └────────┬─────────┘    └───────────┬───────────┘  │
│         │                     │                           │              │
│         │    Client Events    │    User Identity          │   Payment    │
│         │   (with event_id)   │    (GAID + PII)           │   Events    │
│         ▼                     ▼                           ▼              │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      Convex Backend Sync                         │   │
│  │            (stores GAID, hashed PII on user record)              │   │
│  └──────────────────────────────────┬───────────────────────────────┘   │
+─────────────────────────────────────┼───────────────────────────────────+
                                      │
                                      ▼
+─────────────────────────────────────────────────────────────────────────+
│                        CONVEX BACKEND (Server)                          │
│                                                                         │
│  ┌─────────────────┐     ┌──────────────────┐    ┌──────────────────┐  │
│  │  User Record     │     │  Stripe Webhooks  │    │  App Events      │  │
│  │                 │     │                  │    │  (from client)    │  │
│  │ • hashed_email  │     │ • checkout done  │    │                  │  │
│  │ • hashed_name   │     │ • trial started  │    │ • event_id       │  │
│  │ • gaid          │     │ • payment made   │    │ • event_name     │  │
│  │ • convex_id     │     │ • renewal        │    │ • timestamp      │  │
│  └────────┬────────┘     └────────┬─────────┘    └────────┬─────────┘  │
│           │                       │                        │            │
│           └───────────┬───────────┘────────────────────────┘            │
│                       ▼                                                 │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                    Conversions API Dispatcher                     │   │
│  │                                                                  │   │
│  │  POST graph.facebook.com/v19.0/{pixel_id}/events                 │   │
│  │                                                                  │   │
│  │  Payload:                                                        │   │
│  │  • event_name     (Purchase, StartTrial, Lead, etc.)             │   │
│  │  • event_id       (for dedup with client SDK)                    │   │
│  │  • event_time     (unix timestamp)                               │   │
│  │  • action_source  ("app")                                        │   │
│  │  • user_data      (em, fn, ln, madid, external_id)               │   │
│  │  • custom_data    (currency, value, content_name)                │   │
│  └──────────────────────────────────────────────────────────────────┘   │
+─────────────────────────────────────────────────────────────────────────+
                                      │
                                      ▼
                        ┌─────────────────────────┐
                        │      META ADS SYSTEM     │
                        │                         │
                        │  • Install attribution  │
                        │  • Conversion optimize  │
                        │  • Lookalike audiences   │
                        │  • ROAS reporting        │
                        └─────────────────────────┘
```

---

## Signal Funnel

Events sent to Meta at each stage of the user journey. Each event is fired **both** client-side (Meta SDK) and server-side (Conversions API) with a shared `event_id` for deduplication.

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                 │
│   Ad Click ──► Install ──► App Open ──► Onboarding Complete     │
│                  │              │               │                │
│              (automatic)   (automatic)     fb_mobile_complete    │
│              Meta SDK      Meta SDK       _registration          │
│                                                │                │
│                                                ▼                │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                    PAYWALL SCREEN                        │   │
│   │                                                         │   │
│   │   User sees pricing ──────────► InitiateCheckout        │   │
│   │   User taps Subscribe ────────► StartTrial / Purchase   │   │
│   │   User dismisses ────────────► (no event)               │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                │                │
│                                                ▼                │
│                                                                 │
│   ┌─────────────────────────────────────────────────────────┐   │
│   │                    POST-PAYWALL                          │   │
│   │                                                         │   │
│   │   Trial converts ────────────► Purchase (server-only)   │   │
│   │   Subscription renews ───────► Purchase (server-only)   │   │
│   │   Subscription cancels ──────► (custom: Churn)          │   │
│   └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## Event Definitions

### Tier 1 — Standard Events (Meta-optimizable)

These are the events Meta's ad system can directly optimize against.

| Event Name | Trigger | Source | Value | Priority |
|---|---|---|---|---|
| `Install` | First app open | Meta SDK (auto) | — | Baseline |
| `CompleteRegistration` | Onboarding finished | Client + Server | — | High |
| `InitiateCheckout` | Paywall viewed | Client + Server | — | High |
| `StartTrial` | Free trial begins | Client + Server | — | Critical |
| `Purchase` | First payment / renewal | Server (Stripe webhook) | `$amount` | Critical |

### Tier 2 — Custom Proxy Signals

Additional signals that improve Meta's understanding of user quality.

| Event Name | Trigger | Source | Purpose |
|---|---|---|---|
| `Lead` | Permissions granted (usage access) | Client + Server | Engagement quality signal |
| `SetIntention` | First app intention created | Client + Server | Activation signal |
| `StartSession` | First present session started | Client + Server | Deep engagement signal |
| `Day3Retention` | App opened on day 3+ | Server (scheduled) | Retention quality signal |
| `Churn` | Subscription cancelled | Server (Stripe webhook) | Negative signal for exclusion audiences |

---

## User Matching Parameters

Higher match rates = better attribution = better ad optimization. Send as many parameters as possible with each Conversions API call.

```
┌──────────────────────────────────────────────────────────────────┐
│                     MATCH QUALITY HIERARCHY                       │
│                                                                  │
│  ████████████████████████████████████████████  GAID (madid)      │
│  ██████████████████████████████████████       Hashed Email (em)  │
│  ████████████████████████████████             External ID         │
│  ██████████████████████████                   Hashed Name (fn/ln)│
│  ████████████████████                         IP Address (client) │
│  ████████████████                             User Agent          │
│                                                                  │
│  ◄─── Higher Match Rate                Lower Match Rate ───►    │
│                                                                  │
│  BEST: Send GAID + hashed email + external_id together           │
│  This gives Meta multiple signals to match across its user graph │
└──────────────────────────────────────────────────────────────────┘
```

| Parameter | Key | Source | Hashing |
|---|---|---|---|
| Email | `em` | Auth0 user profile | SHA-256, lowercase, trimmed |
| First Name | `fn` | Auth0 user profile | SHA-256, lowercase |
| Last Name | `ln` | Auth0 user profile | SHA-256, lowercase |
| Mobile Advertiser ID | `madid` | Android GAID (client) | Sent plaintext |
| External ID | `external_id` | Convex user ID | SHA-256 |
| Client IP | `client_ip_address` | Request headers | Sent plaintext |
| Client User Agent | `client_user_agent` | Request headers | Sent plaintext |

---

## Deduplication

Meta deduplicates events from both layers using `event_id` + `event_name`. Without this, events would be double-counted.

```
         Client (Meta SDK)                    Server (CAPI)
        ┌──────────────┐                   ┌──────────────┐
        │ StartTrial   │                   │ StartTrial   │
        │ id: abc-123  │───┐           ┌───│ id: abc-123  │
        │ t: 17:04:30  │   │           │   │ t: 17:04:31  │
        └──────────────┘   │           │   └──────────────┘
                           ▼           ▼
                    ┌─────────────────────┐
                    │   META DEDUP ENGINE  │
                    │                     │
                    │  Same event_name +  │
                    │  Same event_id      │
                    │  Within 48h window  │
                    │                     │
                    │  ──► Count as ONE   │
                    └─────────┬───────────┘
                              ▼
                    ┌─────────────────────┐
                    │  Single StartTrial  │
                    │  event recorded     │
                    │  (server preferred) │
                    └─────────────────────┘
```

### Implementation

```kotlin
// Client side — generate event_id, send to both Meta SDK and backend
val eventId = UUID.randomUUID().toString()

// 1. Log to Meta SDK
val params = Bundle().apply {
    putString("event_id", eventId)
}
logger.logEvent("StartTrial", params)

// 2. Send to Convex backend (which forwards to CAPI)
convexManager.mutation("events:trackConversion", mapOf(
    "event_name" to "StartTrial",
    "event_id" to eventId,
    "user_id" to userId
))
```

---

## Server-Side CAPI Payload

Example Conversions API request for a `Purchase` event:

```json
{
  "data": [
    {
      "event_name": "Purchase",
      "event_time": 1709654400,
      "event_id": "evt_abc123",
      "action_source": "app",
      "user_data": {
        "em": ["a1b2c3...sha256hash"],
        "fn": ["d4e5f6...sha256hash"],
        "ln": ["g7h8i9...sha256hash"],
        "madid": "cdda802e-fb9c-47ad-9866-0794d394c912",
        "external_id": ["j0k1l2...sha256hash"],
        "client_ip_address": "203.0.113.42",
        "client_user_agent": "Dalvik/2.1.0 (Linux; U; Android 14)"
      },
      "custom_data": {
        "currency": "USD",
        "value": 49.99,
        "content_name": "BePresent Annual",
        "content_category": "subscription"
      },
      "app_data": {
        "advertiser_tracking_enabled": true,
        "application_tracking_enabled": true,
        "extinfo": ["a2", "com.bepresent.android", "1.0.0", "100", "14.0", "Pixel 8", "en_US", "UTC-5", "", "", "1080", "2400", "2.75", "8", "128", ""]
      }
    }
  ],
  "access_token": "EAAxxxxxxx",
  "partner_agent": "convex"
}
```

---

## Data Flow By Event

```
Event               Client SDK    CAPI (Server)    Trigger
─────               ──────────    ─────────────    ───────

Install             ██████████                     Automatic (Meta SDK)
App Open            ██████████                     Automatic (Meta SDK)
CompleteRegistration██████████    ██████████████    Onboarding last step
Lead                ██████████    ██████████████    Usage permission granted
InitiateCheckout    ██████████    ██████████████    Paywall screen viewed
StartTrial          ██████████    ██████████████    Stripe trial created
Purchase                         ██████████████    Stripe webhook (payment_intent.succeeded)
Renewal                          ██████████████    Stripe webhook (invoice.paid, period > 1)
Churn                            ██████████████    Stripe webhook (customer.subscription.deleted)
Day3Retention                    ██████████████    Convex scheduled function
SetIntention        ██████████    ██████████████    First intention saved
StartSession        ██████████    ██████████████    First present session
```

---

## Why Both Layers

```
                Signal Reliability Comparison

  SDK Only          ███████░░░░░░░░░░░░░  ~40%
  (client)          Battery optimization kills background events
                    User can revoke ad tracking
                    App-level crashes lose events

  CAPI Only         ░░░░░░░░███████████░░  ~60%
  (server)          No GAID = lower match rate on cold installs
                    No automatic install/app_open attribution
                    Delayed — misses real-time optimization windows

  SDK + CAPI        ████████████████████  ~90%+
  (both)            GAID from client + PII from server = best match
                    Redundant delivery = no lost events
                    Real-time (SDK) + reliable (CAPI) coverage
                    Meta deduplicates automatically via event_id
```

---

## Implementation Phases

### Phase 1 — Meta SDK Integration (Client)
1. Add `com.facebook.android:facebook-android-sdk` dependency
2. Configure `FacebookSdk.sdkInitialize()` in `Application.onCreate()`
3. Add Facebook App ID to `AndroidManifest.xml`
4. Capture GAID and store on Convex user record
5. Log `CompleteRegistration`, `InitiateCheckout`, `StartTrial` with `event_id`

### Phase 2 — Conversions API (Server)
1. Create Meta System User + generate CAPI access token
2. Build Convex HTTP action for CAPI dispatch
3. Wire Stripe webhooks to fire CAPI events (`Purchase`, `Renewal`, `Churn`)
4. Forward client events server-side with user matching data
5. Implement SHA-256 hashing for PII fields

### Phase 3 — Optimization
1. Add `Day3Retention` scheduled function in Convex
2. Tune which events to optimize ad sets against (likely `StartTrial` initially, `Purchase` once volume allows)
3. Build exclusion audiences from `Churn` events
4. Monitor Event Match Quality score in Meta Events Manager (target >6.0)

---

## Key Metrics to Track

| Metric | Target | Where to Check |
|---|---|---|
| Event Match Quality | > 6.0 / 10 | Meta Events Manager |
| CAPI Coverage | > 95% of conversions | Meta Events Manager → Diagnostics |
| Dedup Rate | ~50% (means both layers fire) | Meta Events Manager → Overlap |
| Cost Per Trial | Decreasing over time | Meta Ads Manager |
| Trial-to-Paid Rate | Stable/improving | Stripe Dashboard + Convex |
