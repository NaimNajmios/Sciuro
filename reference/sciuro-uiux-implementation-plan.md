# Sciuro — UI/UX Implementation Plan
## Design System, Iconography, Motion/Lottie, States, Modals & Cross-Module Interaction Choreography

> Companion to the v4 engineering plan. This document covers the full visual and interaction layer: design tokens, composable library, icon system, Lottie/motion inventory, every empty/loading/error state, the modal system, navigation, and — the part most plans skip — exactly how modules hand off to each other so the app feels like one coherent product, not a set of screens.

---

## 1. Design Philosophy & Visual Direction

### 1.1 Continuity with your existing aesthetic, adapted for finance

Your portfolio's dark neomorphism direction — near-black `#0D0D10`, chartreuse `#C8FF00` accent, DM Serif Display paired with IBM Plex Mono — is a strong, distinctive starting point, and reusing it gives Sciuro visual continuity with your other work. Two deliberate adaptations for a finance app specifically:

- **Restrained neomorphism, not full neomorphism.** Full soft-UI (dual embossed shadows on every element) is known to reduce affordance clarity — users can't always tell what's tappable. Sciuro reserves the neomorphic treatment for a small set of "hero" surfaces (the runway card, account cards, the Kanban board background) and keeps interactive elements (buttons, list rows, chips) flatter with a clear pressed-state so tap targets stay unambiguous. This is a conscious refinement of your aesthetic for a functional-first surface, not an abandonment of it.
- **Chartreuse does double duty.** Rather than adding a separate "positive/income" green and diluting the palette, `#C8FF00` is both the brand accent *and* the positive-money color — growth, income, "on track" all read as chartreuse. This keeps the palette lean and makes the accent color feel meaningful rather than decorative, the same way Cash App's brand green or Robinhood's brand tones double as their positive-state colors.

### 1.2 The mascot idea: leaning into the name

"Sciuro" traces back to *Sciuridae* — the squirrel family. That's a genuinely useful hook for the "lively" quality you're asking for, and it's exactly the kind of thing mainstream apps use to make empty/loading/celebration moments feel designed rather than default (Duolingo's owl, Headspace's blob character, Robinhood's confetti, Mint's old parachute icon). Proposal: a simple, geometric squirrel mark — not a mascot that talks or has a personality arc, just a recurring visual motif — used specifically for:

- Empty states (a squirrel with an empty paw / resting pose)
- Loading states (a squirrel gathering/stashing acorns — literally "your data is being gathered")
- Celebration moments (a squirrel with a small pile of acorns for genuinely meaningful milestones — debt paid off, savings goal hit)

This is a proposal, not a given — flag if you'd rather keep it purely abstract/geometric with no character at all. The rest of this plan assumes the squirrel motif is in, since it's referenced in the states tables below, but every instance can be swapped for a non-mascot alternative without restructuring anything else.

### 1.3 Core principles (carried from the v3/v4 UX sections, restated for this doc)

1. **Glanceable before detailed** — every screen has one thing to look at first.
2. **Motion with purpose, not decoration** — every animation either communicates a state change or provides feedback for an action; nothing moves just to look busy.
3. **Restrained delight** — celebration moments are reserved for milestones that matter (debt payoff, first month completed), not fired on every transaction. Overusing delight cheapens it — this is a deliberate departure from apps that confetti everything.
4. **Silence by default, loud when it matters** — consistent with the v3 notification philosophy, extended to in-app motion: routine confirmations are quiet (a small toast), genuine milestones are loud (Lottie + haptic).

---

## 2. Design Tokens

### 2.1 Color roles

| Token | Value | Usage |
|---|---|---|
| `surface.base` | `#0D0D10` | App background |
| `surface.elevated` | `#17171B` | Cards, sheets |
| `surface.neomorphic.light` / `.dark` | shadow pair off `surface.elevated` | Hero cards only (runway, account cards, Kanban background) |
| `accent.primary` (chartreuse) | `#C8FF00` | CTAs, positive/income amounts, brand marks, "on track" runway state |
| `text.primary` | `#F5F5F0` | Headlines, primary amounts |
| `text.secondary` | `#8C8C94` | Labels, timestamps, metadata |
| `signal.transfer` | `#7C9CBF` (cool neutral blue-gray) | Transfer-tagged transactions, "this moved, not spent" |
| `signal.warning` | `#E8B84B` (amber) | Due-soon bills, approaching budget threshold, runway mid-state |
| `signal.danger` | `#E3543D` | Overdue, shortage, BNPL stacking risk, runway low-state |
| `signal.neutral-expense` | `#D8D8D2` | Ordinary expense amounts — deliberately *not* red; not every purchase is a warning |

**Runway temperature** (from the v2 UX section, formalized here as a token gradient): `accent.primary` → `signal.warning` → `signal.danger`, interpolated by safe-to-spend ratio, applied only to the one home-screen number — this stays the single place color carries financial judgment, everywhere else color is purely categorical.

### 2.2 Typography

| Role | Font | Usage |
|---|---|---|
| Display | DM Serif Display | Runway number, screen titles, celebration moment headlines |
| Numeric/data | IBM Plex Mono, tabular figures | All transaction amounts, balances, dates in lists — monospace keeps decimals aligned down a list, a deliberate fintech convention (Revolut/N26 use tabular numerals for the same reason) |
| Body | IBM Plex Sans (or system default) | Everything else — descriptions, labels, settings copy |

### 2.3 Spacing, radius, elevation

- Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 / 48 (standard 4pt-based scale, matches Compose defaults)
- Radius: 12dp for cards, 20dp for bottom sheets (top corners), 999dp (full) for pills/chips/FAB
- Elevation: two-tier only — `flat` (list rows, chips) and `raised` (cards, sheets, dialogs) — avoids the "elevation soup" that makes some Material apps feel visually noisy

### 2.4 Motion tokens

| Token | Duration | Easing | Usage |
|---|---|---|---|
| `motion.micro` | 100–150ms | standard decelerate | Button press, chip toggle |
| `motion.transition` | 250–300ms | emphasized | Screen transitions, bottom sheet enter/exit |
| `motion.card-move` | 350ms | spring (medium bounce) | Kanban card auto-move, list reorder |
| `motion.celebration` | 600–900ms | spring (playful) | Confetti, mascot celebration Lottie |
| `motion.count` | 400–600ms | ease-out | Number count-up/down (runway changes, investment valuation refresh) |

---

## 3. Iconography System

### 3.1 Technical approach

Compose Multiplatform has no first-party icon font. Two workable paths:

1. **Convert Lucide's SVGs to Compose `ImageVector`** — Lucide is MIT-licensed, you already use it in your React work (`lucide-react`), and there are established SVG→ImageVector conversion tools (Android Studio's built-in Vector Asset importer, or the `svg-to-compose` community tool) that batch-convert an icon set. This keeps a consistent icon language across your web portfolio and Sciuro.
2. **Material Symbols (Rounded variant)** as a fallback for anything Lucide doesn't cover well — Lucide is strong on general UI icons but thinner on some finance-specific glyphs (e.g., very specific bank/wallet iconography).

Recommendation: Lucide-first, Material Symbols Rounded for gaps. Both render as vectors, both support tinting, both work fine at the small sizes a transaction list needs.

### 3.2 Category → icon/color mapping (spend taxonomy from v2)

| Category | Icon (Lucide name) | Tint |
|---|---|---|
| Food & Dining | `utensils` | neutral-expense |
| Transport | `car` | neutral-expense |
| Shopping | `shopping-bag` | neutral-expense |
| Entertainment | `clapperboard` | neutral-expense |
| Health | `heart-pulse` | neutral-expense |
| Personal Care | `sparkles` | neutral-expense |
| Education | `book-open` | neutral-expense |
| Social/Giving | `hand-heart` | neutral-expense |
| Travel | `plane` | neutral-expense |
| Bills — Housing | `home` | neutral-expense |
| Bills — Telco | `smartphone` | neutral-expense |
| Bills — Insurance | `shield` | neutral-expense |
| Subscriptions | `refresh-cw` (small "recurring" badge overlay) | neutral-expense |
| Income (any) | `arrow-down-left` | accent.primary |
| Transfer (any) | `arrow-left-right` | signal.transfer |
| Debt repayment | `landmark` | neutral-expense, with a small progress-ring overlay showing payoff % |

### 3.3 Channel → icon mapping (Malaysian payment rails from v3)

| Channel | Icon | Notes |
|---|---|---|
| DuitNow QR | `qr-code` | Most common — should render crisply even at 16dp in dense lists |
| DuitNow Transfer | `send` | |
| FPX | `globe` | |
| Card | `credit-card` | |
| ATM Withdrawal | `banknote` | Paired with the Cash wallet's icon on the destination side |
| Cash Deposit | `wallet` | |
| E-wallet reload | provider's own glyph where recognizable, else `wallet` | |
| Standing Instruction | `repeat` | |

### 3.4 Bank/e-wallet marks

Small brand marks (CIMB/Maybank/BSN/TNG/GrabPay/Boost/ShopeePay) used purely for personal recognition in a personal-use app — fine as-is. If this ever moves beyond personal/sideloaded use, these need to be revisited for trademark reasons; flagged here so it isn't forgotten later, not a blocker now.

---

## 4. Lottie & Motion Library

### 4.1 Technical library

Airbnb's `lottie-compose` is Android-only and won't work for the Windows-desktop target on your roadmap. For true Compose Multiplatform: **Compottie** (`io.github.alexzhirkevich:compottie`) is the right choice — it's an actively maintained, MIT-licensed, pure-Kotlin Lottie renderer built specifically for Compose Multiplatform (Android/iOS/JVM/desktop/Web), currently at the 2.x line with its own rendering engine (no platform delegates). `Kottie` is a viable alternative from the same ecosystem if Compottie's API doesn't fit — worth a quick spike with both before committing, but Compottie is the more mature, more widely-referenced option as of mid-2026.

### 4.2 Lottie vs. native Compose animation — when to use which

| Use Lottie when | Use native Compose animation (`animateFloatAsState`, `AnimatedVisibility`, spring specs) when |
|---|---|
| Character/mascot moments (empty states, celebrations) | Simple property changes (fade, scale, slide) |
| Complex multi-shape choreography that would be painful to hand-code | Anything tied to a live data value (count-up numbers, progress bars) |
| One-off "hero" moments (onboarding, splash) | Kanban card movement, list item add/remove |

This split matters for performance — Lottie files, even lightweight ones, cost more CPU/battery than native animations, so reserving them for genuinely special moments (not every button press) keeps the app snappy, which is itself a "state of the art" signal mainstream apps get right.

### 4.3 Lottie inventory

| Moment | Trigger | Asset direction | Source strategy |
|---|---|---|---|
| Splash | App cold launch | Squirrel mark assembling/animating in, ~1.2s, skippable | Custom — simple enough to commission or build in a free tool (e.g. a basic After Effects/Bodymovin export, or a simpler tool like Rive exported to Lottie-compatible JSON if preferred) |
| First-launch onboarding | Onboarding flow | Squirrel walking through 3 short explainer beats (permissions, accounts, minimal-input philosophy) | Custom, or LottieFiles marketplace placeholder ("onboarding character walk") swapped later |
| Data sync / initial load | App open while parsing backlog notifications | Squirrel gathering acorns loop | Custom or marketplace ("loading gather" search terms) |
| Empty state (per screen) | See Section 6 table | Squirrel resting / idle pose, screen-specific | Reuse one base rig with 3–4 pose variants rather than commissioning per-screen animations — cheaper and more consistent |
| Celebration — debt paid off | `Debt.outstandingBalance` reaches zero | Squirrel with acorn pile + light confetti, ~1.5s, haptic on trigger | Custom — this is a genuine "flagship moment," worth the extra polish |
| Celebration — savings/budget goal met | Monthly budget closed under target, or investment milestone | Smaller confetti burst, no full mascot animation (reserve the full mascot moment for debt payoff specifically, so it doesn't get diluted) | Native Compose particle-style animation, not full Lottie — cheaper and sufficient |
| Pull-to-refresh | Manual refresh gesture on Home/Wallet | Small squirrel peek, replaces default spinner | Custom, small/lightweight file |
| Error / connection issue | LLM fallback call fails, or a rare hard error | Squirrel looking confused, neutral (not alarming) tone | Marketplace placeholder acceptable long-term — low-visibility moment |

### 4.4 Reduced-motion fallback

Every Lottie moment has a static fallback (the mascot's resting frame as a plain image) that's used automatically when the OS-level reduced-motion accessibility setting is on. Confetti/celebration effects degrade to a simple color flash + haptic instead of particles. This is a real accessibility requirement, not optional polish — covered again in Section 13.

---

## 5. Core Composable Library (Ported from Sprint)

Built once in `core-ui` shared module, reused everywhere — this is what keeps "state of the art" achievable for a solo dev instead of hand-building every screen from scratch. We have directly ported the proven components from the Sprint app:

| Composable | Purpose | Handles |
|---|---|---|
| `HeroPanel` + `WaveChart` | The pure black top section with bezier curve | Trend visualization, range switching via `PillToggle` |
| `SheetList` | -24dp overlapping bottom-sheet container | Nested scrolling, drag handle |
| `TransactionCard` | Adapted from Sprint's `SessionCard` | Merchant name, timestamp, amount (Plex Mono) + **8dp Category Dot** |
| `BudgetProgressRow`| Adapted from Sprint's `ContextBreakdownRow` | Animated `LinearProgressIndicator` (1000ms tween) for spend vs limit |
| `ActionCard` / `HealthCard` | Adapted from Sprint Settings | Supabase sync status, background service monitoring |
| `SessionInspectorSheet`| Adapted from Sprint's Session Modal | AI confidence metrics (Green/Orange/Red), dropdowns, save/ignore |
| `SciuroCard` | Base card with the restrained-neomorphic treatment | Elevation variants, press state |
| `AmountText` | Currency-formatted text with automatic sign-based coloring | Locale-aware RM formatting, tabular figures |
| `StatusPill` | Small rounded label (e.g. "Due in 3 days") | Color-coded by `signal.*` tokens |
| `StatusKanbanBoard` | Generic status-driven board (Sprint `KanbanColumn`) | Column definitions, card auto-move animation |
| `SegmentedControl` | Time-range pickers / View mode switches | `PillToggle` integration |
| `SkeletonBlock` | Loading placeholder (shimmer) | Configurable shape for composing skeletons |
| `EmptyStateView` | Standardized empty-state layout | Takes a config object so every screen's empty state is visually consistent |

---

## 6. Empty States (comprehensive, per screen)

| Screen | Trigger | Visual | Message direction | Primary CTA |
|---|---|---|---|---|
| Home Dashboard | First launch, zero transactions | Squirrel, empty paw, calm pose | "Nothing gathered yet — once your bank notifications start coming in, this is where they'll show up." | "Grant notification access" (if not yet granted) |
| Review Inbox | Nothing pending | Squirrel, satisfied/resting pose | "All caught up. Nothing needs your attention right now." | None — an empty inbox needs no action, that's the point |
| Bills & Debt Kanban | No recurring obligations detected yet | Squirrel, idle | "Sciuro hasn't spotted any recurring bills yet — give it a few weeks of transactions and it'll start picking up patterns." | None (passive — reinforces "zero setup," no button to manually add prematurely) |
| Wallet (Cash) | No cash activity recorded | Squirrel with an empty acorn basket | "No cash tracked yet. Withdraw from an ATM and it'll show up here automatically." | "Log a cash amount manually" (secondary, small) |
| Wallet (E-Wallets) | No e-wallet linked/detected | Same base illustration, wallet-specific copy | "Once you use TNG, GrabPay, Boost, or ShopeePay, transactions will appear here." | None |
| Investment/Gold | No investment account set up | Squirrel next to a single acorn (seed metaphor) | "Track your Maybank Gold Savings here once you add it." | "Add investment account" |
| Category Drilldown | No spend in category this month | Neutral illustration, no mascot needed (low-stakes moment) | "Nothing in \[Category] this month." | None |
| Debt Overview | All debts paid off (celebratory empty state, not neutral) | Squirrel with acorn pile — this reuses the celebration asset, not a separate "sad empty" style | "Debt-free. Nothing owed." | None — this is a moment to sit with, not act on |
| Search / filtered results | No matches | Minimal, no mascot (utility moment) | "No transactions match that filter." | "Clear filters" |
| Budgets | No budgets set | Squirrel, idle | "No budgets yet — set a monthly limit for any category to start tracking against it." | "Set your first budget" |

Design rule embedded in this table: the mascot appears for *emotionally relevant* empty states (nothing to review, debt-free, no cash tracked) and steps back for *purely utilitarian* ones (no search results, no category spend) — using it everywhere would dilute it.

---

## 7. Loading States

### 7.1 Skeleton strategy

Every list-based screen (Home's recent-activity strip, Category Drilldown, Debt Overview, Kanban board) shows a skeleton (shimmer blocks matching the eventual content's shape) on cold load rather than a spinner — this is the mainstream pattern (Instagram, LinkedIn, most modern fintech apps) because it communicates *what's coming* rather than just *that something is happening*, and it feels faster even at identical load times.

- `SkeletonBlock` composed into per-screen skeleton layouts (e.g., three shimmer rows shaped like transaction cards for the Home activity strip).
- Skeletons time out to the real empty state if load genuinely returns nothing — never leave a skeleton on screen indefinitely.

### 7.2 Optimistic UI

Given the "minimal interaction" philosophy, every tap-confirm action (Review Inbox swipe, category recategorize, mark bill settled) updates the UI **instantly**, before the database write completes — the card moves, the count decrements, the moment feels immediate. The actual write happens in the background; if it fails (rare), a toast surfaces ("Couldn't save that change — retrying") and the UI silently reconciles. This single decision does more for "feels state-of-the-art" than almost anything else in this document — waiting for a spinner on a one-tap confirm would undercut the entire minimal-interaction premise.

### 7.3 Background sync indicator

Notification parsing happens continuously in the background. Rather than a blocking loader, a small persistent affordance (a subtle dot or "synced Xm ago" microcopy near the Home header) shows freshness without demanding attention — consistent with "silence by default" from the v3 UX principles. Tapping it triggers a manual refresh with the pull-to-refresh Lottie moment from Section 4.3.

---

## 8. Popup / Modal System

### 8.1 Decision matrix

| Pattern | Use when | Examples |
|---|---|---|
| **Bottom sheet** | Viewing/editing detail without leaving context; multi-step but lightweight flows | Transaction detail, Recount Cash wizard, new-recurring-obligation confirmation, unmatched-transfer resolution |
| **Dialog (centered)** | Destructive or high-consequence confirmations that deserve a harder interrupt | Delete a debt entry, clear cash adjustment log, disable audit trail (if ever exposed as a setting) |
| **Toast/Snackbar** | Lightweight, undo-able confirmations of an action already taken | "Categorized as Food & Dining · Undo," "Transfer matched · Undo," bill marked settled |
| **Inline card (no modal)** | Low-friction proposals that don't need full interruption | "Looks like a new recurring bill — Maxis, ~RM68/month" appearing as a dismissible card at the top of the Kanban board rather than a popup blocking the screen |

### 8.2 Specific modal inventory

- **Transaction Detail** (bottom sheet) — full parsed detail, category (editable), channel icon, linked TransferLink if applicable, and a link into its `AuditLog` history ("categorized as Food & Dining, auto, 94% confidence → recategorized as Transport, you, 2 days later").
- **Recount Cash** (bottom sheet, 3-step wizard) — enter actual amount → variance shown live as you type → optional remark field → confirm. Matches the v3 spec exactly, now with the visual/motion detail: the variance number animates in with `motion.count` once an amount is entered, color-coded amber/danger if the shortage is large, neutral if trivial.
- **New Recurring Obligation proposal** (inline card, not a blocking sheet) — "Looks like a new bill" with a one-tap Confirm/Dismiss, consistent with the zero-setup philosophy; only escalates to a bottom sheet if the person taps to edit details.
- **Unmatched Transfer resolution** — presented as a `SwipeableRow` action in Review Inbox ("This is me" / "Someone else") rather than a popup, since it's a binary choice best resolved with the same swipe gesture used elsewhere in that screen — consistency over novelty.
- **Budget threshold breach** — a `StatusPill` color shift on the relevant category card plus an optional system notification (only if the person has opted into alerts) — never a blocking in-app popup, since budget overages are informational, not urgent.

---

## 9. Navigation Architecture

- **Bottom navigation, 4 destinations** (the mainstream pattern for finance apps — Monzo, Revolut, Cash App all converge on this): **Home** · **Kanban** (Bills & Debt) · **Wallet** · **More** (Budgets, Investment, Settings, Review Inbox badge count).
- Review Inbox is *not* a fifth tab — it's accessed via a badge/entry point from Home and More, since an empty inbox (the common state) shouldn't occupy permanent nav real estate.
- **FAB (floating action button)**, present on Home and Wallet only — expands on tap into 2–3 quick actions: "Log cash spend," "Add informal debt," "Add manual transaction." This is the one deliberately manual entry point in an otherwise passive app, kept small and out of the way rather than centered/prominent, so it doesn't visually compete with the passive-capture philosophy.
- **Shared-element transitions** between Home and detail screens — tapping the runway number expands into its breakdown (which obligations were subtracted) rather than a hard navigation cut; tapping a bill in the "next 3 due" strip transitions into the Kanban board pre-scrolled and highlighted to that card. These transitions are what make module hand-offs feel connected rather than like separate apps stapled together — covered in detail next.

---

## 10. Cross-Module Interaction Choreography

This is the part most implementation plans skip, and it's what actually determines whether Sciuro feels like one coherent product. Each row is a real user moment, traced across modules.

| Flow | Trigger | Path | Motion/feedback |
|---|---|---|---|
| **Live capture while app is open** | A bank/e-wallet notification arrives while Sciuro is foregrounded | Notification → parser → classifier → confirmed transaction | A small toast rises from the bottom, non-blocking: "RM12.50 · Grab · Transport." Auto-dismisses in ~3s, tappable to open Transaction Detail. Does *not* interrupt whatever screen the person is on. |
| **Runway drilldown** | Tap the Home runway number | Home → expanded breakdown | The number itself grows and the card expands downward (shared-element, not a new screen push) to list the RecurringObligations subtracted from it. Collapses back on tap-away. |
| **Bill due → Kanban** | Tap a bill in the "next 3 due" strip on Home | Home → Kanban, pre-navigated | Standard push transition, but the Kanban board opens already scrolled to and briefly highlighting (a soft pulse) the relevant card — avoids the "now go find it yourself" gap. |
| **Bill paid → card settles** | A matching notification confirms payment | Kanban board (if open) or silently if not | Card animates from "Due Soon" to "Settled" column using `motion.card-move` (spring), with a brief checkmark micro-animation on the card itself. If the Kanban screen isn't open, this just happens silently in data — no need to force the person to watch it. |
| **Review Inbox resolve** | Swipe-confirm a pending item | Inbox row → gone, counters update | Card flies off in the swipe direction (`motion.micro`/`transition` blend), the inbox badge count on the bottom nav and More screen rolls over with a quick digit-transition animation rather than an instant jump-cut. |
| **Cross-account transfer confirmed** | Bank↔bank or bank↔cash match resolves | Home (if visible) | Both affected account balance rows on Home pulse softly and update simultaneously — visually reinforcing "this moved, it didn't vanish," directly supporting the mental model the whole Transfer system (Section 2.7 of the v3 plan) is built around. |
| **Investment valuation refresh** | Gold price API call returns a new price (app open / periodic refresh) | Investment tile on Home, Investment screen | The value number count-transitions (`motion.count`) to the new figure rather than snapping instantly, with a tiny up/down arrow that briefly flashes the relevant signal color — same pattern stock-tracking apps use, applied here at a deliberately calmer pace since this is a savings account, not a trading screen. |
| **Debt fully paid off** | `Debt.outstandingBalance` hits zero | Debt Overview | The flagship celebration moment (Section 4.3): mascot Lottie + confetti + haptic, `motion.celebration` timing. This is the single most "loud" moment in the entire app, deliberately — it's rare and it's worth marking. |
| **Cash recount closes a gap** | Recount flow completed with a nonzero variance | Wallet screen | The Cash balance figure updates with `motion.count`, and the new CashAdjustment entry slides into the Adjustments log below with a brief highlight fade — visible enough to confirm it was recorded, quiet enough not to feel like an error state. |

---

## 11. Screen-by-Screen Specification

| Screen | Purpose | Key composables | Empty state | Loading state | Primary interactions | Motion notes |
|---|---|---|---|---|---|---|
| **Splash** | Cold-launch brand moment | Lottie (squirrel assemble) | — | — | Auto-advance (skippable after ~0.5s) | Section 4.3 |
| **Onboarding** | Explain philosophy, request notification access | `SciuroButton`, Lottie walk sequence | — | — | 3-beat swipe-through, permission request with clear rationale before the OS prompt | `motion.transition` between beats |
| **Home Dashboard** | The 90%-of-opens glanceable answer | `RunwayCounter`, `SciuroCard` (account rows), horizontal bill strip, FAB | Section 6 | Skeleton on cold load (Section 7.1) | Tap runway → drilldown; tap bill → Kanban; tap account → that account's transaction list | Section 10 flows |
| **Transaction Detail** | Full record of one transaction | `SciuroBottomSheet`, `AmountText`, `IconBadge`, audit-history link | N/A (always has data) | Instant (already loaded) | Edit category, view audit trail, mark as transfer manually if mis-triaged | `motion.transition` sheet enter |
| **Category Drilldown** | Spend analysis for one category | `SegmentedControl` (time range), bar/line chart, transaction list | Section 6 | Skeleton chart + list rows | Change time range, tap a transaction → Transaction Detail | Chart redraw animates on range change |
| **Bills & Debt Kanban** | Status board for recurring obligations | `StatusKanbanBoard`, `StatusPill` | Section 6 | Skeleton cards per column | Tap card → detail sheet; manual drag only for informal-debt cards | `motion.card-move` on auto-settle |
| **Debt Overview** | All debts + payoff trajectory | `ProgressRing`, list of `SciuroCard` per debt | Section 6 (celebratory when debt-free) | Skeleton cards | Tap a debt → payoff trajectory detail; BNPL stacking flag as a `StatusPill` at top if 3+ active | Celebration moment (Section 4.3) on full payoff |
| **Wallet** | Cash + e-wallet balances | `AmountText` (hero), quick-action buttons, recent log | Section 6 (separate cash/e-wallet variants) | Skeleton | "Log cash spend," "Recount cash" (Section 8.2), tap an e-wallet → its own transaction list | `motion.count` on balance changes |
| **Investment/Gold** | Weight-based ledger + computed valuation | `RunwayCounter`-style hero number (reused pattern), buy/sell history list, small value-over-time chart | Section 6 | Skeleton | Tap → buy/sell history, manual "Add transaction" for GIA entries without a notification | `motion.count` on price refresh; disclaimer text always visible (bank spread caveat from v4 Section 9) |
| **Budgets** | Category limits vs. actual | `ProgressRing` per category, `StatusPill` for threshold state | Section 6 | Skeleton rows | Set/edit limit, toggle rollover | Ring fill animates on data change |
| **Review Inbox** | Low-confidence items needing a human | `SwipeableRow`, optional `StatusKanbanBoard` alt-view (Section 7 of v4) | Section 6 (an empty inbox is a feature) | Skeleton rows | Swipe confirm/dismiss, bulk re-categorize by merchant | Card fly-off + counter roll (Section 10) |
| **Settings/More** | Security, backup, LLM opt-in, notification allowlist, About | Standard list rows, toggles | N/A | N/A | Manage which apps are allowlisted for capture, trigger encrypted export, toggle LLM-assisted parsing (Section 6 of v4) | Minimal motion — this screen should feel calm/utilitarian |

---

## 12. Accessibility & Reduced-Motion Guardrails

- **Color is never the only signal** — every `signal.*` color pairing (transfer/warning/danger) is always accompanied by an icon or shape change, not color alone, so the app remains legible for color-blind users.
- **Dynamic type support** — typography scale respects system font-size settings; the DM Serif Display headline numbers are the one place a max-scale cap is reasonable (to avoid the runway number wrapping awkwardly), everywhere else scales freely.
- **Reduced motion** — every Lottie moment has a static fallback (Section 4.4); spring-based card movements degrade to simple cross-fades; celebration confetti degrades to a color flash + haptic only.
- **Haptics map** — light tap for one-tap confirmations, medium for celebration moments, a distinct warning buzz reserved for shortage/anomaly detection — consistent and sparing, not haptic-on-everything.
- **Dark mode as primary**, matching the aesthetic direction — a light-mode palette should exist for parity but is not the primary design target for v1.

---

## 13. Where This Slots Into the v4 Phase Plan

This document is primarily **Milestone C (Experience Layer)** from the v4 engineering plan, but a few pieces need to start earlier to avoid rework:

| This document's work | v4 phase it belongs to |
|---|---|
| Design tokens (Section 2), core composable shells (Section 5) | Should start during **A0 (Engineering Foundations)** — build the token file and empty composable signatures early so every subsequent phase builds against them instead of ad-hoc styling |
| Icon system (Section 3), Compottie integration spike (Section 4.1) | Early in **Milestone A**, as a short technical spike — confirms the Lottie library choice before any screen depends on it |
| Empty/loading states, modal system (Sections 6–8) | Built alongside each module in **Milestone B** as those modules land — a screen shouldn't be considered "done" in B without its empty/loading state, rather than retrofitting all of them in one pass later |
| Full screen builds, Kanban visualization, navigation, motion choreography (Sections 9–11) | **Milestone C**, as originally scoped |
| Celebration moments, mascot Lottie polish | Can slip to a post-MVP polish pass without blocking anything — flagged as the safest thing to de-scope if Milestone C runs long |

---

## 14. Immediate Next Steps

1. Build the design token file (Section 2) as actual Kotlin objects/Compose theme definitions — this is cheap, fast, and unblocks consistent styling from the very first screen.
2. Run the Compottie technical spike (one splash-screen Lottie, rendered on both Android and a quick desktop target check) before committing further — confirms the library choice holds up on your actual multiplatform setup.
3. Decide on the squirrel mascot direction (Section 1.2) — in or out — since several downstream sections (empty states, celebrations) reference it directly; easy to swap now, more rework once composables are built around it.
