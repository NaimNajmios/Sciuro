# Sciuro — UAT Checklist

**116 test cases across 23 modules.** Companion checklist to `sciuro-uat-test-tracker.xlsx`. Each module includes: the **user flow** with the specific UI element involved at every step, how that module's **UX intertwines with other modules** (which Domain Event fires and what else reacts), and a **developer test flow** for testing without depending on real bank activity.

## How to use this

- Check a box `- [x]` once a test **passes**. Leave it unchecked if not yet tested or failing.
- Failed tests: don't check the box — add a line under **Notes** describing what actually happened.
- **Phase** tags reference the v4 engineering plan's milestones (`A0–A6`, `B1–B7`, `C1–C3`, `D1–D3`) — a guide for sequencing, not a rigid contract.
- Priority: 🔴 Critical (must be correct before anything else is trustworthy) · 🟠 High (core module correctness) · 🟡 Medium (UX polish/secondary paths) · 🟢 Low (nice-to-have/deferred).
- Bank/e-wallet sources assumed throughout: CIMB, Maybank, BSN, Touch 'n Go eWallet, GrabPay, Boost, ShopeePay.
- UI element names (`RunwayCounter`/`HeroPanel`-pattern, `SheetList`, `PillToggle`, `StatusKanbanBoard`, `SwipeableRow`, `SciuroBottomSheet`, `SciuroCard`, `IconBadge`, `StatusPill`, `ProgressRing`, `EmptyStateView`, `SkeletonBlock`) reference the shared composable library from the UI/UX plan.

## Developer Test Infrastructure (Recommended)

Three debug-only tools get referenced repeatedly below — worth building once, early, rather than re-solving per module. None of these ship in a release build.

1. **Notification Simulator** — a hidden screen that lets you paste raw notification text and pick a source package (CIMB/Maybank/BSN/TNG/GrabPay/Boost/ShopeePay), then injects it directly into the real intake pipeline.
2. **Debug Data Seeder** — directly writes account balances, debt states, obligation statuses, or budget totals into the database, so a test can start from a specific scenario instantly instead of accumulating real data.
3. **Domain Event Log** — lists every DomainEvent as it fires, in order, with its payload. This is what makes the Cross-Module Cascade tests actually verifiable in one place instead of checking five screens by hand.

---

## Table of Contents

- [Onboarding](#onboarding) (4)
- [Notification Capture](#notification-capture) (6)
- [Parsing](#parsing) (8)
- [Triage & Categorization](#triage-&-categorization) (6)
- [Transfer Detection](#transfer-detection) (6)
- [Physical Cash Wallet](#physical-cash-wallet) (5)
- [E-Wallet Accounts](#e-wallet-accounts) (5)
- [Recurring Obligations](#recurring-obligations) (6)
- [Debt Ledger](#debt-ledger) (7)
- [Balance & Reconciliation](#balance-&-reconciliation) (4)
- [Investment / Gold](#investment--gold) (5)
- [Budgeting](#budgeting) (5)
- [Audit Log](#audit-log) (5)
- [Cross-Module Cascades](#cross-module-cascades) (6)
- [Net Position](#net-position) (3)
- [Review Inbox](#review-inbox) (5)
- [Kanban Board](#kanban-board) (5)
- [Home Dashboard](#home-dashboard) (5)
- [Navigation & UI Shell](#navigation-&-ui-shell) (4)
- [Security](#security) (5)
- [Empty/Loading States](#emptyloading-states) (5)
- [Settings](#settings) (4)
- [Desktop Companion](#desktop-companion) (2)

---

## Onboarding

**User flow (UI elements named at each step):**
1. Welcome screen (splash Lottie mark, monochrome) explains the minimal-interaction philosophy in 2-3 swipeable beats
2. Permission step: a plain-language rationale line renders above a `SciuroButton` ("Grant Access") — the OS permission dialog only appears after this, never before
3. Tap `SciuroButton` → OS notification-listener permission dialog → grant or deny
4. On grant, app transitions (`motion.transition`) into Home; on deny, a persistent but non-blocking banner (same visual weight as the Review Inbox "action needed" banner) offers a path back to Settings
5. Home renders its `EmptyStateView` (zero-transaction squirrel pose) since no notifications have been captured yet

**How this intertwines with other modules:**
- Granting access here is what unlocks every other module — nothing downstream (Parsing, Triage, Kanban, Home) has anything to react to until the first real notification lands
- No Domain Event fires from onboarding itself; it's purely a permission-state gate in front of the rest of the pipeline

**Developer test flow:**
1. Add a debug-only "Reset onboarding" toggle (Settings → Developer) to re-run the flow repeatedly without reinstalling
2. Test the decline branch by revoking notification-listener access via Settings → Notification access, or the equivalent adb notification-listener command, then relaunch

**Test cases:**

- [ ] **ONB-01 — Welcome & philosophy explainer displays** 🟡 `Medium · Milestone C`
    - *Feature:* First-run experience
    - *Precondition:* Fresh install, first launch
    - *Steps:* Install app and open for the first time
    - *Expected:* Welcome screens explain the minimal-interaction philosophy in plain language before any setup is requested
    - *Notes:* 

- [ ] **ONB-02 — Notification access request shows rationale first** 🔴 `Critical · Milestone C`
    - *Feature:* Permission rationale
    - *Precondition:* Onboarding in progress
    - *Steps:* Reach the notification-access step
    - *Expected:* A one-sentence explanation appears before the system permission dialog, not after
    - *Notes:* 

- [ ] **ONB-03 — Declining notification access is handled gracefully** 🔴 `Critical · Milestone C`
    - *Feature:* Permission decline handling
    - *Precondition:* On the permission step
    - *Steps:* Deny the notification access request
    - *Expected:* App does not crash; a clear path to grant access later from Settings is shown, no dead end
    - *Notes:* 

- [ ] **ONB-04 — Onboarding ends on the correct Home empty state** 🟡 `Medium · Milestone C`
    - *Feature:* Completion state
    - *Precondition:* Onboarding just completed, zero data
    - *Steps:* Complete all onboarding steps
    - *Expected:* Home screen shows the zero-transaction empty state, not an error or blank screen
    - *Notes:* 

---

## Notification Capture

**User flow:** None by design — this module is intentionally passive/background. The user takes no direct action to trigger it; it's only ever observed indirectly through its effects on other screens.

**How this intertwines with other modules:**
- A captured notification (while the app is foregrounded) surfaces as a small, non-blocking toast — `SnackbarHost`-style, auto-dismissing (~3s), tappable into Transaction Detail — this is the one visible UI moment this module produces on its own
- Every capture is the root trigger for the entire downstream chain: Parsing → Triage → Categorization/Transfer-matching → Recurring/Debt updates → Balance recalculation → Kanban/Budget/Runway/Net Position → AuditLog. Nothing else in the app moves without this firing first

**Developer test flow:**
1. Build a debug-only Notification Simulator screen: paste raw notification text, pick a source package (CIMB/Maybank/BSN/TNG/etc.), inject it directly into the same intake pipeline the real NotificationListenerService uses — this becomes the single most-used tool for the rest of development
2. For lower-level pipeline checks, adb shell cmd notification post can post a real system notification from a test package, though it won't carry a bank app's exact extras

**Test cases:**

- [ ] **NOT-01 — CIMB Clicks notification is captured** 🔴 `Critical · A2`
    - *Feature:* Bank capture — CIMB
    - *Precondition:* Notification access granted, CIMB Clicks installed
    - *Steps:* Trigger a real CIMB transaction notification
    - *Expected:* Notification is captured into the raw staging buffer within a few seconds
    - *Notes:* 

- [ ] **NOT-02 — Maybank2u notification is captured** 🔴 `Critical · A2`
    - *Feature:* Bank capture — Maybank
    - *Precondition:* Notification access granted, Maybank2u installed
    - *Steps:* Trigger a real Maybank2u transaction notification
    - *Expected:* Notification is captured into the raw staging buffer
    - *Notes:* 

- [ ] **NOT-03 — BSN notification is captured** 🔴 `Critical · A2`
    - *Feature:* Bank capture — BSN
    - *Precondition:* Notification access granted, BSN app installed
    - *Steps:* Trigger a real BSN transaction notification
    - *Expected:* Notification is captured into the raw staging buffer
    - *Notes:* 

- [ ] **NOT-04 — Non-allowlisted app notifications are ignored** 🔴 `Critical · A2`
    - *Feature:* Allowlist enforcement
    - *Precondition:* Any non-finance app installed (e.g. a messaging app)
    - *Steps:* Receive a notification from a non-allowlisted app
    - *Expected:* Notification is not captured, stored, or processed in any way
    - *Notes:* 

- [ ] **NOT-05 — Capture works while app is backgrounded or killed** 🔴 `Critical · A2`
    - *Feature:* Background reliability
    - *Precondition:* App backgrounded or force-closed
    - *Steps:* Trigger a bank notification while the app isn't in the foreground
    - *Expected:* Notification is still captured and later reflected in the transaction list once opened
    - *Notes:* 

- [ ] **NOT-06 — New finance app install triggers a suggestion** 🟢 `Low · B (automation)`
    - *Feature:* Proactive app detection
    - *Precondition:* A new bank/e-wallet app is installed on the device
    - *Steps:* Install e.g. Boost after Sciuro is already set up
    - *Expected:* A one-tap suggestion appears offering to track the newly installed app
    - *Notes:* 

---

## Parsing

**User flow:** None by design — this module is intentionally passive/background. The user takes no direct action to trigger it; it's only ever observed indirectly through its effects on other screens.

**How this intertwines with other modules:**
- Fully invisible to the user by design — its only observable trace is the extractionMethod (REGEX vs LLM_FALLBACK) shown quietly inside Transaction Detail's audit history, for anyone who goes looking
- A failed/low-confidence extraction is what populates the Review Inbox `SwipeableRow` list — that's the one UI consequence a parsing shortfall has

**Developer test flow:**
1. Maintain a test/resources/notification_fixtures/ folder of anonymized real notification samples per bank, channel, and language — run parser unit tests against these on every change
2. Use the Notification Simulator to verify a new fixture end-to-end before committing it, so the unit test and the real pipeline agree

**Test cases:**

- [ ] **PAR-01 — CIMB DuitNow QR payment parses correctly** 🔴 `Critical · A3`
    - *Feature:* CIMB — DuitNow QR
    - *Precondition:* A real CIMB DuitNow QR notification exists
    - *Steps:* Let the notification be parsed
    - *Expected:* Amount, merchant, and channel (DuitNow QR) are all correctly extracted
    - *Notes:* 

- [ ] **PAR-02 — Maybank FPX payment parses correctly** 🔴 `Critical · A3`
    - *Feature:* Maybank — FPX
    - *Precondition:* A real Maybank FPX notification exists
    - *Steps:* Let the notification be parsed
    - *Expected:* Amount, merchant, and channel (FPX) are correctly extracted
    - *Notes:* 

- [ ] **PAR-03 — BSN transaction parses correctly** 🔴 `Critical · A3`
    - *Feature:* BSN — standard transaction
    - *Precondition:* A real BSN notification exists
    - *Steps:* Let the notification be parsed
    - *Expected:* Amount and direction are correctly extracted at minimum; channel where determinable
    - *Notes:* 

- [ ] **PAR-04 — ATM withdrawal is correctly channel-tagged** 🔴 `Critical · A3`
    - *Feature:* ATM withdrawal channel
    - *Precondition:* A real ATM withdrawal notification exists
    - *Steps:* Let the notification be parsed
    - *Expected:* Channel is tagged ATM_WITHDRAWAL, distinct from a normal card spend
    - *Notes:* 

- [ ] **PAR-05 — BM-language notification parses as accurately as English** 🟠 `High · A3`
    - *Feature:* Bahasa Malaysia variant
    - *Precondition:* Device/app locale set to Bahasa Malaysia for one bank
    - *Steps:* Trigger a transaction with the BM notification variant
    - *Expected:* Amount, direction, and channel extract correctly, matching English-variant accuracy
    - *Notes:* 

- [ ] **PAR-06 — Unparseable format still books with partial data** 🔴 `Critical · A3/A4`
    - *Feature:* Graceful degradation
    - *Precondition:* A notification format the parser doesn't recognize (simulate if needed)
    - *Steps:* Let an unrecognized-format notification arrive
    - *Expected:* Transaction still books with at least the amount extracted and routes to Review Inbox rather than being dropped or crashing
    - *Notes:* 

- [ ] **PAR-07 — LLM extraction engages when regex fails** 🟠 `High · A4`
    - *Feature:* LLM fallback trigger
    - *Precondition:* LLM-assisted parsing enabled in Settings; an unrecognized format arrives
    - *Steps:* Let the fallback path run
    - *Expected:* LLM extraction attempt occurs and produces a structured draft, visible in the transaction's extraction method
    - *Notes:* 

- [ ] **PAR-08 — Avail Bal figure is captured where present** 🟠 `High · A3`
    - *Feature:* Available balance extraction
    - *Precondition:* A notification containing an available-balance figure
    - *Steps:* Let it be parsed
    - *Expected:* The balance figure is correctly extracted and available to the reconciliation engine
    - *Notes:* 

---

## Triage & Categorization

**User flow (UI elements named at each step):**
1. Low-confidence case: item appears as a `SwipeableRow` in Review Inbox, with the proposed `IconBadge` + category name already filled in — swipe right to confirm, left to dismiss/recategorize
2. High-confidence case: no interaction at all — the transaction simply appears already-categorized in whichever list surfaces it (Category Drilldown, Home's recent-activity strip)
3. Manual correction path: open the transaction from any list → `SciuroBottomSheet` (Transaction Detail) → tap the category `IconBadge` → pick a new one from a `PillToggle`-style category picker → save

**How this intertwines with other modules:**
- Confirming in Review Inbox fires `TransactionCategorized` + `MerchantRuleLearned` together — the item's `SwipeableRow` flies off-screen, the Review Inbox nav badge count rolls down by one (digit-transition, not a jump-cut), and every *future* transaction from that same merchant now skips Review Inbox entirely
- A manual recategorization from Transaction Detail fires `TransactionRecategorized`, writing an AuditLog before/after pair, and updates that transaction's category everywhere it's rendered (Category Drilldown, Budgets progress ring) without needing to reload those screens separately

**Developer test flow:**
1. Unit test the triage/classifier function directly against mocked merchant + amount + channel input — no UI needed
2. Use the Notification Simulator for full-pipeline verification once the unit-level logic passes

**Test cases:**

- [ ] **CAT-01 — Genuine purchase is triaged as SPEND** 🔴 `Critical · A6`
    - *Feature:* Spend triage
    - *Precondition:* A normal merchant purchase notification
    - *Steps:* Let it be triaged
    - *Expected:* Transaction is tagged legType = SPEND, not TRANSFER or INCOME
    - *Notes:* 

- [ ] **CAT-02 — Genuine income is triaged as INCOME** 🔴 `Critical · A6`
    - *Feature:* Income triage
    - *Precondition:* A salary/allowance credit notification
    - *Steps:* Let it be triaged
    - *Expected:* Transaction is tagged legType = INCOME
    - *Notes:* 

- [ ] **CAT-03 — Self-transfer is triaged as TRANSFER, not spend** 🔴 `Critical · A6/B2`
    - *Feature:* Transfer triage
    - *Precondition:* A DuitNow transfer to your own second account
    - *Steps:* Let it be triaged
    - *Expected:* Transaction is tagged legType = TRANSFER and excluded from spend categorization
    - *Notes:* 

- [ ] **CAT-04 — First-time merchant routes to Review at low confidence** 🟠 `High · A6`
    - *Feature:* New merchant routing
    - *Precondition:* A transaction from a merchant never seen before
    - *Steps:* Let it be categorized
    - *Expected:* Transaction appears in Review Inbox rather than being silently auto-categorized incorrectly
    - *Notes:* 

- [ ] **CAT-05 — Repeat merchant auto-categorizes without review** 🟠 `High · A6`
    - *Feature:* Merchant memory
    - *Precondition:* A merchant confirmed once already in Review Inbox
    - *Steps:* A second transaction from the same merchant arrives
    - *Expected:* It is auto-categorized correctly without appearing in Review Inbox again
    - *Notes:* 

- [ ] **CAT-06 — Editing a category from Transaction Detail works** 🔴 `Critical · A6`
    - *Feature:* Manual recategorization
    - *Precondition:* Any confirmed transaction
    - *Steps:* Open Transaction Detail and change its category
    - *Expected:* Category updates immediately and an AuditLog entry records the change
    - *Notes:* 

---

## Transfer Detection

**User flow (UI elements named at each step):**
1. Clean auto-match: no interaction — both legs quietly resolve into a TransferLink, visible only if you open either transaction's detail sheet and see the linked-transfer indicator
2. Unmatched case: item appears in Review Inbox as a `SwipeableRow` with two actions instead of the usual categorize choice — "This is me" / "Someone else", rendered as two `SciuroButton`s beneath the transaction summary

**How this intertwines with other modules:**
- Confirming "This is me" fires `TransferMatched` + `RecipientRuleLearned` — both affected account `SciuroCard` rows on Home pulse softly and simultaneously (reinforcing "this moved, it didn't vanish"), both legs are retroactively excluded from that month's Category Drilldown and Budgets totals, and future transfers to the same recipient auto-match without asking again
- Confirming "Someone else" instead fires normal `TransactionCategorized` — it becomes an ordinary spend/income transaction with no special treatment from that point on

**Developer test flow:**
1. Feed a paired source-debit + destination-credit notification via the Notification Simulator with an adjustable time gap, to test the matching window's boundary directly
2. Unit test the matching engine against synthetic TransferLink candidates, including deliberately ambiguous ones (same amount, different people)

**Test cases:**

- [ ] **TRF-01 — Self-transfer between two banks auto-matches** 🔴 `Critical · B2`
    - *Feature:* Bank-to-bank matching
    - *Precondition:* Transfer sent between two of your own linked accounts
    - *Steps:* Let both notifications arrive
    - *Expected:* A single TransferLink is created; neither leg appears as spend or income
    - *Notes:* 

- [ ] **TRF-02 — ATM withdrawal auto-credits the Cash wallet** 🔴 `Critical · B2`
    - *Feature:* ATM auto-credit
    - *Precondition:* An ATM withdrawal notification
    - *Steps:* Let it be processed
    - *Expected:* Cash wallet balance increases by the withdrawn amount with no manual confirmation needed
    - *Notes:* 

- [ ] **TRF-03 — Cash deposit prompts a one-tap confirm and debits Cash** 🟠 `High · B2`
    - *Feature:* Cash deposit confirm
    - *Precondition:* A cash deposit notification
    - *Steps:* Let it be processed, then respond to the prompt
    - *Expected:* Cash wallet balance decreases by the deposited amount once confirmed
    - *Notes:* 

- [ ] **TRF-04 — Unrecognized DuitNow Transfer recipient routes to Review** 🔴 `Critical · B2`
    - *Feature:* Unmatched transfer routing
    - *Precondition:* A transfer to a new, unrecognized recipient
    - *Steps:* Let it be processed
    - *Expected:* It appears in Review Inbox with a 'this is me / someone else' choice rather than being auto-classified
    - *Notes:* 

- [ ] **TRF-05 — Confirming a recipient once teaches the rule** 🟠 `High · B2`
    - *Feature:* Recipient learning
    - *Precondition:* A recipient confirmed as 'me' in Review Inbox previously
    - *Steps:* A second transfer to the same recipient arrives
    - *Expected:* It auto-matches without prompting again
    - *Notes:* 

- [ ] **TRF-06 — Matched transfers never appear in budget totals** 🔴 `Critical · B2/B7`
    - *Feature:* Budget exclusion
    - *Precondition:* A confirmed TransferLink exists this month
    - *Steps:* Check Category Drilldown and Budgets screens
    - *Expected:* Neither leg of the transfer contributes to any category spend total
    - *Notes:* 

---

## Physical Cash Wallet

**User flow (UI elements named at each step):**
1. Tap the centered FAB → it expands into 2-3 quick actions (icon-labeled `SciuroButton`s) → tap "Log cash spend" → a minimal inline field (amount + optional category `IconBadge`) → confirm — no full-screen form
2. Tap "Recount cash" from the Wallet screen → `SciuroBottomSheet` opens as a 3-step wizard: enter actual amount (`AmountText` input) → the variance renders live beneath it, color-coded via the same `signal.warning`/`signal.danger` tokens used elsewhere → optional remark field → confirm

**How this intertwines with other modules:**
- A logged spend or a completed recount fires `CashDebited`/`CashRecounted`, which count-animates (`motion.count`) the Cash balance on the Wallet screen's hero figure, the Home account-summary row, and the Net Position tile all at once
- Critically, a recount variance explicitly does *not* touch any `StatusPill`/`ProgressRing` on Budgets or Category Drilldown — the CashAdjustment lives only in the Wallet screen's own Adjustments log, by design, so a lost RM20 note never quietly inflates a spend category

**Developer test flow:**
1. Seed CashAccount balance directly via a debug data seeder to set up specific drift scenarios instantly, instead of performing real ATM withdrawals repeatedly
2. Unit test the CashAdjustment variance/remark logic directly

**Test cases:**

- [ ] **CSH-01 — Cash balance starts correctly at first use** 🟡 `Medium · B2`
    - *Feature:* Balance initialization
    - *Precondition:* Fresh install, no cash activity yet
    - *Steps:* Open the Wallet screen
    - *Expected:* Balance shows zero with the correct empty state, not an error
    - *Notes:* 

- [ ] **CSH-02 — Logging a cash spend updates the balance** 🟡 `Medium · B2`
    - *Feature:* Manual spend logging
    - *Precondition:* Cash balance is nonzero
    - *Steps:* Use the 'Log cash spend' quick action with an amount
    - *Expected:* Cash balance decreases by that amount immediately
    - *Notes:* 

- [ ] **CSH-03 — Recount shows correct computed-vs-actual variance** 🟠 `High · B2`
    - *Feature:* Recount flow
    - *Precondition:* Cash balance has some value from prior activity
    - *Steps:* Run Recount Cash and enter a different actual amount
    - *Expected:* The variance is shown accurately and live as the amount is entered
    - *Notes:* 

- [ ] **CSH-04 — Recount variance logs as an Adjustment with remark, not a spend category** 🔴 `Critical · B2`
    - *Feature:* Adjustment logging
    - *Precondition:* A recount reveals a shortage or surplus
    - *Steps:* Complete the recount with a remark
    - *Expected:* A CashAdjustment entry appears in the Adjustments log; no spend category total changes
    - *Notes:* 

- [ ] **CSH-05 — Cash balance stays accurate across a mix of ATM/deposit/spend activity** 🟠 `High · B2`
    - *Feature:* Multi-transaction accuracy
    - *Precondition:* Several ATM withdrawals, one deposit, and a couple of logged spends have occurred
    - *Steps:* Compare computed balance to actual wallet cash
    - *Expected:* Balance matches within expected drift, and any gap is explainable via the Adjustments log
    - *Notes:* 

---

## E-Wallet Accounts

**User flow (UI elements named at each step):**
1. Wallet screen → e-wallet section renders each provider as its own `SciuroCard` with a provider `IconBadge` → tap a card to open its transaction list (a `SheetList`-pattern beneath a small hero balance)

**How this intertwines with other modules:**
- A bank-to-e-wallet reload fires `TransferMatched` on both legs, the same way a bank-to-bank transfer does — both the source bank's `SciuroCard` on Home and the destination e-wallet's `SciuroCard` on Wallet update together
- An e-wallet spend flows through the exact same Triage/Categorization pipeline as a bank spend — if low-confidence, it lands in the same Review Inbox `SwipeableRow` list, no separate e-wallet-specific review queue

**Developer test flow:**
1. Notification Simulator with the TNG/GrabPay/Boost/ShopeePay package identity selected

**Test cases:**

- [ ] **EWL-01 — Touch 'n Go eWallet notification is captured and parsed** 🔴 `Critical · A2/A3`
    - *Feature:* TNG capture
    - *Precondition:* TNG eWallet installed and allowlisted
    - *Steps:* Trigger a TNG transaction
    - *Expected:* Transaction is captured, parsed, and appears against the TNG e-wallet account
    - *Notes:* 

- [ ] **EWL-02 — GrabPay, Boost, and ShopeePay notifications are captured** 🟠 `High · A2/A3`
    - *Feature:* Other wallets capture
    - *Precondition:* Each app installed and allowlisted
    - *Steps:* Trigger a transaction on each
    - *Expected:* Each is captured and parsed correctly, same as TNG
    - *Notes:* 

- [ ] **EWL-03 — Bank-to-e-wallet reload creates a complete TransferLink** 🟠 `High · B2`
    - *Feature:* Reload transfer linking
    - *Precondition:* A reload from a bank account to an e-wallet
    - *Steps:* Let both notifications arrive
    - *Expected:* A TransferLink connects the bank debit and the e-wallet credit; neither shows as spend
    - *Notes:* 

- [ ] **EWL-04 — E-wallet spend categorizes like any normal transaction** 🟠 `High · A6`
    - *Feature:* E-wallet spend categorization
    - *Precondition:* An e-wallet purchase (e.g. GrabPay ride)
    - *Steps:* Let it be processed
    - *Expected:* It's categorized under the correct taxonomy category, same pipeline as bank spend
    - *Notes:* 

- [ ] **EWL-05 — E-wallet cashback/rewards book as passive income** 🟡 `Medium · A6`
    - *Feature:* Cashback as income
    - *Precondition:* A cashback credit notification from an e-wallet
    - *Steps:* Let it be processed
    - *Expected:* It's tagged as Income (Passive/other), not as a transfer or spend
    - *Notes:* 

---

## Recurring Obligations

**User flow (UI elements named at each step):**
1. A detected pattern surfaces as an inline `SciuroCard` proposal — not a blocking sheet — appearing atop the Kanban board with Confirm/Dismiss `SciuroButton`s directly on the card
2. Tap Confirm → the card animates into becoming a permanent entry on the `StatusKanbanBoard`, landing in Upcoming or Due Soon depending on its next expected date
3. Editing: tap any existing Kanban card → `SciuroBottomSheet` detail view → adjust amount/date fields → save

**How this intertwines with other modules:**
- Confirming fires `RecurringObligationConfirmed`, which does three things at once: the card appears on the Kanban board, Home's "next 3 bills due" horizontal strip re-evaluates whether this new obligation now belongs in it, and the Runway hero figure's forward projection silently recalculates to account for it — all from one tap, no separate step for each
- A matching transaction arriving later fires `ObligationCycleSettled`, animating (`animateItem()`/`motion.card-move`) the card from Due Soon straight into Settled with no drag required

**Developer test flow:**
1. Seed 2-3 synthetic matching transactions via the Notification Simulator, spaced at the expected cadence, to trigger detection without waiting through real months
2. Unit test the pattern-matching threshold (occurrence count, amount tolerance) directly against a list of synthetic transactions

**Test cases:**

- [ ] **OBL-01 — New recurring bill is proposed after repeated occurrences** 🟠 `High · B1`
    - *Feature:* Auto-detection
    - *Precondition:* The same merchant/amount/cadence has occurred 2-3 times (e.g. mobile plan)
    - *Steps:* Let the pattern repeat the required number of times
    - *Expected:* A 'looks like a new bill' proposal appears
    - *Notes:* 

- [ ] **OBL-02 — Confirming the proposal creates a persistent obligation** 🟠 `High · B1`
    - *Feature:* One-tap confirm
    - *Precondition:* A recurring obligation proposal is showing
    - *Steps:* Tap confirm
    - *Expected:* RecurringObligation is created and appears on the Kanban board
    - *Notes:* 

- [ ] **OBL-03 — A matching new transaction settles the obligation cycle** 🔴 `Critical · B1`
    - *Feature:* Cycle settlement
    - *Precondition:* An active RecurringObligation exists
    - *Steps:* The matching bill transaction arrives next cycle
    - *Expected:* status moves to PAID_THIS_CYCLE automatically, no manual action needed
    - *Notes:* 

- [ ] **OBL-04 — Settlement moves the Kanban card correctly** 🔴 `Critical · B1/C1`
    - *Feature:* Kanban linkage
    - *Precondition:* An obligation just settled (OBL-03)
    - *Steps:* Open the Kanban board
    - *Expected:* The card has moved from Due Soon/Upcoming into Settled
    - *Notes:* 

- [ ] **OBL-05 — A single price change is flagged, a repeated one auto-updates** 🟡 `Medium · B1`
    - *Feature:* Amount drift handling
    - *Precondition:* A subscription's amount changes for one cycle, then repeats at the new amount
    - *Steps:* Observe behavior across two cycles
    - *Expected:* First occurrence flags for review; after repeating, expectedAmount updates automatically
    - *Notes:* 

- [ ] **OBL-06 — Editing an obligation's amount or date works correctly** 🟡 `Medium · B1`
    - *Feature:* Manual editing
    - *Precondition:* An existing RecurringObligation
    - *Steps:* Edit its expected amount or day
    - *Expected:* Changes save and are reflected on the Kanban card and future detection
    - *Notes:* 

---

## Debt Ledger

**User flow (UI elements named at each step):**
1. FAB → "Add informal debt" → a deliberately minimal `SciuroBottomSheet` with exactly 3 fields (amount, counterparty, direction) → save
2. Debt Overview screen renders each debt as a `SciuroCard` with a `ProgressRing` showing payoff progress → tap any card for its full trajectory detail
3. BNPL risk state renders as a `StatusPill` at the top of the screen, not a popup, when 3+ plans are active

**How this intertwines with other modules:**
- An installment transaction settling a linked obligation fires both `ObligationCycleSettled` and `DebtBalanceUpdated` in the same instant — the Kanban card settles *and* the `ProgressRing` on Debt Overview advances together, from one notification
- When the final installment lands, `DebtFullyPaidOff` fires: the monochrome-squirrel celebration Lottie plays (the one deliberately "loud" moment in the whole app), a shape-morph confirm animation runs (the one place Sciuro uses that effect), and the debt archives itself out of the active Debt Overview list without any manual dismissal

**Developer test flow:**
1. Seed a Debt with a near-zero balance directly via the debug data seeder to test the payoff-celebration trigger without waiting through a real repayment schedule
2. Seed 3+ active BNPL entries to verify the risk flag without opening that many real BNPL plans

**Test cases:**

- [ ] **DBT-01 — PTPTN entry tracks balance, repayment, and due date correctly** 🟠 `High · B5`
    - *Feature:* PTPTN setup
    - *Precondition:* A PTPTN debt entry is created (manually anchored)
    - *Steps:* View the Debt Overview
    - *Expected:* Balance, monthly repayment, and due date all display accurately
    - *Notes:* 

- [ ] **DBT-02 — A loan installment notification reduces the linked debt balance** 🔴 `Critical · B5`
    - *Feature:* Installment linkage
    - *Precondition:* A Debt is linked to a RecurringObligation
    - *Steps:* The installment transaction arrives
    - *Expected:* Debt.outstandingBalance decreases by the installment amount automatically
    - *Notes:* 

- [ ] **DBT-03 — Credit card balance and due date track correctly** 🟠 `High · B5`
    - *Feature:* Credit card tracking
    - *Precondition:* A credit card Debt entry exists
    - *Steps:* Make a purchase and a payment via card
    - *Expected:* Revolving balance and statement due date update accordingly
    - *Notes:* 

- [ ] **DBT-04 — Adding an informal debt is fast and minimal** 🟡 `Medium · B5`
    - *Feature:* Informal debt quick-add
    - *Precondition:* On the Debt Ledger screen
    - *Steps:* Add an informal debt via the 3-field form (amount, counterparty, direction)
    - *Expected:* Entry saves correctly and appears in the debt list with no extra required fields
    - *Notes:* 

- [ ] **DBT-05 — 3+ active BNPL plans trigger a risk flag** 🟡 `Medium · B5`
    - *Feature:* BNPL risk flag
    - *Precondition:* Three or more active BNPL installment plans exist
    - *Steps:* View Debt Overview
    - *Expected:* A visible risk indicator (StatusPill) appears, not a blocking alert
    - *Notes:* 

- [ ] **DBT-06 — A debt reaching zero balance triggers the celebration moment** 🟡 `Medium · B5/C`
    - *Feature:* Payoff celebration
    - *Precondition:* A debt's final installment is about to be paid
    - *Steps:* Let the final installment post
    - *Expected:* outstandingBalance hits zero and the debt-payoff celebration (Lottie/haptic) fires once
    - *Notes:* 

- [ ] **DBT-07 — A fully paid debt moves out of the active list** 🟢 `Low · B5`
    - *Feature:* Archive on payoff
    - *Precondition:* A debt was just paid off (DBT-06)
    - *Steps:* Return to Debt Overview
    - *Expected:* The paid-off debt no longer appears in the active list, and is bound to any 'paid off' history view
    - *Notes:* 

---

## Balance & Reconciliation

**User flow (UI elements named at each step):**
1. No dedicated screen — balances render as `AmountText` inside each account's `SciuroCard` row on Home, updating live as transactions confirm
2. Pull-to-refresh gesture on Home triggers a manual reconciliation check, replacing the default spinner with the small squirrel-peek Lottie moment

**How this intertwines with other modules:**
- A drift detection doesn't interrupt with a popup — it surfaces as a subtle indicator on the affected account's `SciuroCard` (consistent with "silence by default"), and resolves itself silently once a later Avail-Bal-bearing notification confirms the correct figure
- Every balance change anywhere (bank, cash, e-wallet, investment) is one of the inputs the Net Position rollup listens for, so a reconciliation event ripples there too, even though it has no screen of its own

**Developer test flow:**
1. Feed a notification sequence via the Simulator with one deliberately skipped, then a later Avail-Bal-bearing notification, to verify driftFlag triggers correctly
2. Unit test the reconciliation math directly against known balance sequences

**Test cases:**

- [ ] **BAL-01 — Running balance matches the real bank app** 🔴 `Critical · B3`
    - *Feature:* Per-account accuracy
    - *Precondition:* Normal transaction activity on one account over several days
    - *Steps:* Compare Sciuro's shown balance to the actual bank app
    - *Expected:* Balances match within a trivial margin
    - *Notes:* 

- [ ] **BAL-02 — A missed notification triggers a drift flag** 🟠 `High · B3`
    - *Feature:* Drift detection
    - *Precondition:* Simulate a gap (e.g. a transaction the app might have missed)
    - *Steps:* Let a subsequent Avail Bal figure arrive
    - *Expected:* A driftFlag is raised rather than silently showing a wrong balance
    - *Notes:* 

- [ ] **BAL-03 — Cash balance reconciles correctly after a recount** 🟠 `High · B2/B3`
    - *Feature:* Cash reconciliation
    - *Precondition:* A recount was just completed
    - *Steps:* Check the Wallet balance
    - *Expected:* It matches the entered actual amount exactly, with the adjustment logged
    - *Notes:* 

- [ ] **BAL-04 — Balances update immediately after a matched transfer** 🔴 `Critical · B2/B3`
    - *Feature:* Transfer-driven balance update
    - *Precondition:* A transfer was just matched (TRF-01)
    - *Steps:* Check both accounts on Home
    - *Expected:* Both balances reflect the movement correctly and simultaneously
    - *Notes:* 

---

## Investment / Gold

**User flow (UI elements named at each step):**
1. First-time setup: More → Investment → `SciuroButton` "Add investment account" → enter provider + starting weight via a short `SciuroBottomSheet` form
2. Ongoing entry: "Add transaction" → Buy/Sell toggle (`PillToggle`) → weight + price fields → save
3. The Investment tile itself mirrors the Home hero-figure pattern (`HeroPanel`-style: big weight/value figure, small chart underneath) both on its own screen and as a secondary tile on Home

**How this intertwines with other modules:**
- A price refresh fires `InvestmentPriceRefreshed`, which count-transitions (`motion.count`) the displayed value on *both* the Investment screen's hero figure and the Net Position rollup simultaneously — two renderings of the same recalculation, not two separate updates
- A manual buy/sell entry fires `InvestmentTransactionRecorded`, which only ever changes the stored weight (unitBalance) — the MYR value shown is always freshly computed from that weight and the latest price, never stored as ground truth

**Developer test flow:**
1. Add a debug toggle to force a specific fake gold price response, to test valuation math and the count-up/down animation without depending on the real market moving

**Test cases:**

- [ ] **INV-01 — Gold Investment Account is created with correct starting weight** 🟠 `High · B6`
    - *Feature:* Account creation
    - *Precondition:* Setting up the Investment module for the first time
    - *Steps:* Add the Maybank GIA account with an initial weight
    - *Expected:* unitBalance reflects the entered starting weight in grams
    - *Notes:* 

- [ ] **INV-02 — Manual buy/sell correctly adjusts the weight ledger** 🟠 `High · B6`
    - *Feature:* Buy/sell entry
    - *Precondition:* An existing InvestmentAccount
    - *Steps:* Log a buy of X grams, then a sell of Y grams
    - *Expected:* unitBalance updates correctly after each entry (never the MYR value directly)
    - *Notes:* 

- [ ] **INV-03 — Displayed value equals weight × current price** 🟠 `High · B6`
    - *Feature:* Valuation calculation
    - *Precondition:* A known unitBalance and a known fetched gold price
    - *Steps:* Open the Investment screen
    - *Expected:* Displayed MYR value matches the manual calculation
    - *Notes:* 

- [ ] **INV-04 — Value updates smoothly when the price refreshes** 🟡 `Medium · B6/C`
    - *Feature:* Price refresh behavior
    - *Precondition:* The Investment screen is open
    - *Steps:* Trigger a price refresh (reopen app or manual refresh)
    - *Expected:* The value count-animates to the new figure rather than jump-cutting
    - *Notes:* 

- [ ] **INV-05 — The bank-spread caveat is visible** 🟡 `Medium · B6`
    - *Feature:* Valuation disclaimer
    - *Precondition:* Any time the Investment screen is viewed
    - *Steps:* Open the Investment screen
    - *Expected:* A disclaimer noting the estimate may differ from Maybank's actual GIA rate is visible
    - *Notes:* 

---

## Budgeting

**User flow (UI elements named at each step):**
1. More → Budgets → tap a category `SciuroCard` → set/edit limit via an inline field → save
2. Ongoing state renders as a `ProgressRing` + `StatusPill` per category, both here and mirrored on that category's Category Drilldown screen

**How this intertwines with other modules:**
- Crossing 80% or 100% fires `BudgetThresholdCrossed`, which shifts the `StatusPill` color on *both* the Budgets screen and the Category Drilldown screen at once — one event, two renderings, never out of sync — plus an optional system notification only if the person has opted in
- Budget totals are computed only from transactions with legType = SPEND/INCOME; anything tagged TRANSFER by the Transfer Detection module is structurally excluded before it ever reaches this calculation, not filtered out after the fact

**Developer test flow:**
1. Seed category spend totals directly via the debug data seeder to jump straight to the 80%/100% threshold states, instead of generating dozens of real transactions

**Test cases:**

- [ ] **BUD-01 — Setting a category budget limit works correctly** 🟠 `High · B7`
    - *Feature:* Manual limit setting
    - *Precondition:* On the Budgets screen
    - *Steps:* Set a monthly limit for a category
    - *Expected:* Limit saves and progress tracks against it going forward
    - *Notes:* 

- [ ] **BUD-02 — A suggested limit appears once enough history exists** 🟡 `Medium · B7`
    - *Feature:* Auto-suggested limits
    - *Precondition:* 2-3 months of category spend history exist
    - *Steps:* Open Budgets for an unset category
    - *Expected:* A suggested limit based on historical average appears, one-tap to accept
    - *Notes:* 

- [ ] **BUD-03 — Crossing 80% of a budget triggers the correct state** 🟠 `High · B7`
    - *Feature:* 80% threshold
    - *Precondition:* A category is at or near 80% of its limit
    - *Steps:* Cross the threshold with a new transaction
    - *Expected:* The StatusPill/color state updates on both Budgets and Category Drilldown
    - *Notes:* 

- [ ] **BUD-04 — Crossing 100% of a budget triggers the correct state** 🟠 `High · B7`
    - *Feature:* 100% threshold
    - *Precondition:* A category is at or over its limit
    - *Steps:* Cross the threshold
    - *Expected:* The state escalates correctly (and an opt-in notification fires if enabled)
    - *Notes:* 

- [ ] **BUD-05 — Budget totals never include transfer-tagged transactions** 🔴 `Critical · B7`
    - *Feature:* Transfer exclusion spot-check
    - *Precondition:* A month with at least one self-transfer and normal spend
    - *Steps:* Review a category total against a manual sum of only true spend
    - *Expected:* The two match; the transfer amount is not included anywhere
    - *Notes:* 

---

## Audit Log

**User flow (UI elements named at each step):**
1. Open any Transaction/Debt/Obligation's `SciuroBottomSheet` detail view → tap "History" → a plain-language timeline renders ("categorized as Food & Dining, auto, 94% confidence → recategorized as Transport, you, 2 days later")

**How this intertwines with other modules:**
- This screen doesn't originate anything — it's the one place that renders the accumulated trace of every other module's Domain Events. Every cascade described elsewhere in this document (categorization, transfer matches, obligation settlements, debt updates, cash adjustments, budget changes, investment entries) writes here automatically, since AuditLog subscribes to the event bus as a universal listener rather than being called explicitly by each feature

**Developer test flow:**
1. Repository-level test: perform a mutation in a unit test and assert an AuditLog row was written with the correct before/after JSON — correctness doesn't need the UI, only the display formatting does

**Test cases:**

- [ ] **AUD-01 — Every new transaction produces an AuditLog entry** 🔴 `Critical · A1`
    - *Feature:* Creation logging
    - *Precondition:* Any new transaction is confirmed
    - *Steps:* Check its audit history
    - *Expected:* A CREATE entry exists with the correct source (SYSTEM_AUTO/LLM_INFERRED/USER_MANUAL)
    - *Notes:* 

- [ ] **AUD-02 — Recategorizing a transaction logs a correct before/after entry** 🔴 `Critical · A1`
    - *Feature:* Change logging
    - *Precondition:* A transaction is recategorized (CAT-06)
    - *Steps:* Check its audit history
    - *Expected:* An UPDATE entry shows the old and new category with a timestamp
    - *Notes:* 

- [ ] **AUD-03 — Audit history is viewable from Transaction Detail** 🟠 `High · A1/C`
    - *Feature:* Trail visibility
    - *Precondition:* Any transaction with at least one change
    - *Steps:* Open Transaction Detail and view history
    - *Expected:* Full change history displays in plain language, not raw JSON
    - *Notes:* 

- [ ] **AUD-04 — Audit entries cannot be edited or deleted after the fact** 🔴 `Critical · A1`
    - *Feature:* Immutability
    - *Precondition:* Any existing AuditLog entries
    - *Steps:* Attempt to alter history through normal app use
    - *Expected:* There is no path in the UI to edit or delete a past audit entry
    - *Notes:* 

- [ ] **AUD-05 — LLM-inferred actions are tagged with confidence** 🟠 `High · A1/A4`
    - *Feature:* Source & confidence tagging
    - *Precondition:* A transaction was categorized via LLM fallback
    - *Steps:* Check its audit entry
    - *Expected:* source = LLM_INFERRED with a populated confidence value
    - *Notes:* 

---

## Cross-Module Cascades

**User flow (UI elements named at each step):**
1. There's no dedicated screen for this module — its "UI" is the sum of every ripple effect described throughout this document. The clearest single example: confirming nothing at all (a bill notification just arrives) results in the Kanban card settling, the Budgets `ProgressRing` advancing, the Home Runway figure recalculating, and an AuditLog entry appearing — four screens updating from one event the user never directly touched
2. The one place a user *does* feel this directly is the live-capture toast (see Notification Capture) and the account-row pulse on Home (see Transfer Detection) — both are the visible tip of a cascade running underneath

**How this intertwines with other modules:**
- Every row in the cascade catalog (CAS-01 through CAS-06) is this module's real content — see those test cases directly for the specific event chains

**Developer test flow:**
1. Build a debug-only Domain Event Log screen listing every DomainEvent as it fires, in order, with its payload — trigger one root event via the Notification Simulator and watch the entire cascade fire in the log without manually checking five different screens by hand

**Test cases:**

- [ ] **CAS-01 — One bill payment updates Kanban, Budget, Runway, and Audit together** 🔴 `Critical · B1/B7/C2`
    - *Feature:* Bill payment cascade
    - *Precondition:* An active RecurringObligation with a matching transaction arriving
    - *Steps:* Let the matching transaction post
    - *Expected:* Kanban card settles, category budget increments, Home runway recalculates excluding it, and an audit entry is written — all without a second tap
    - *Notes:* 

- [ ] **CAS-02 — A loan installment updates both Debt and Kanban simultaneously** 🔴 `Critical · B1/B5`
    - *Feature:* Loan payment dual cascade
    - *Precondition:* A Debt linked to a RecurringObligation, installment due
    - *Steps:* Let the installment transaction post
    - *Expected:* Debt balance decreases and the Kanban card settles from the same single event
    - *Notes:* 

- [ ] **CAS-03 — ATM withdrawal cascades to Cash credit with zero manual steps** 🔴 `Critical · B2`
    - *Feature:* ATM cascade
    - *Precondition:* A card is used for an ATM withdrawal
    - *Steps:* Let the notification process
    - *Expected:* Cash wallet balance increases automatically; no confirmation prompt appears
    - *Notes:* 

- [ ] **CAS-04 — A recount shortage corrects Cash balance without touching spend categories** 🔴 `Critical · B2/B7`
    - *Feature:* Cash shortage isolation
    - *Precondition:* A recount reveals a shortage
    - *Steps:* Complete the recount with a remark
    - *Expected:* Cash balance corrects; no category budget or spend total changes as a result
    - *Notes:* 

- [ ] **CAS-05 — Confirming a new obligation immediately affects future runway math** 🟠 `High · B1/C2`
    - *Feature:* Forward runway projection
    - *Precondition:* A new recurring obligation is confirmed (OBL-02)
    - *Steps:* Check Home's runway calculation for a date before the obligation's next due date
    - *Expected:* The obligation is now included in the forward projection without any extra setup
    - *Notes:* 

- [ ] **CAS-06 — Recognized income pattern populates the runway's next-income date** 🟠 `High · B3/C2`
    - *Feature:* Income timing inference
    - *Precondition:* A recurring income pattern (e.g. monthly stipend) has occurred 2-3 times
    - *Steps:* Check what date the runway calculation uses as 'next income'
    - *Expected:* The date reflects the detected pattern automatically, with no manual setup required
    - *Notes:* 

---

## Net Position

**User flow (UI elements named at each step):**
1. Rendered as a smaller secondary figure beneath the Home Runway hero figure, or one tap into its own screen for the full per-account breakdown

**How this intertwines with other modules:**
- This figure has no independent trigger of its own — it's a subscriber to nearly every other module's events (`TransactionCategorized`, `TransferMatched`, `CashCredited`/`CashDebited`, `DebtBalanceUpdated`, `InvestmentPriceRefreshed`) and simply recomputes the sum fresh each time any of them fire, rather than being incrementally updated by each module individually

**Developer test flow:**
1. Seed known values across every account type via the debug data seeder, compute the expected total by hand, compare against the displayed figure

**Test cases:**

- [ ] **NET-01 — Net Position correctly sums all account types minus debt** 🟡 `Medium · B7`
    - *Feature:* Aggregation accuracy
    - *Precondition:* Bank, cash, e-wallet, investment, and debt data all present
    - *Steps:* Compare the displayed Net Position to a manual sum
    - *Expected:* Figures match exactly
    - *Notes:* 

- [ ] **NET-02 — Net Position updates immediately when any input changes** 🟡 `Medium · B7`
    - *Feature:* Live recalculation
    - *Precondition:* Net Position is currently displayed
    - *Steps:* Make any change (new transaction, price refresh, debt payment)
    - *Expected:* The figure updates without needing to leave and reopen the screen
    - *Notes:* 

- [ ] **NET-03 — Manually re-derived total matches what's shown, at any point in time** 🟢 `Low · B7`
    - *Feature:* Cross-check
    - *Precondition:* Any point with mixed account activity
    - *Steps:* Independently total every account/debt and compare
    - *Expected:* Sciuro's figure and the manual total agree
    - *Notes:* 

---

## Review Inbox

**User flow (UI elements named at each step):**
1. Never a bottom-nav tab — reached via a prominent banner (red `errorContainer`-tinted, matching Sprint's real "action needed" pattern) that only renders when the inbox is non-empty
2. Inside: a list of `SwipeableRow` items — swipe right to confirm the suggested action, left to dismiss/reject, or long-press a merchant name for a bulk re-categorize action across every pending item from that merchant

**How this intertwines with other modules:**
- Every resolution here is a dispatch point back into whichever module the item originated from — a categorization confirm feeds Triage & Categorization's `MerchantRuleLearned`, a transfer confirm feeds Transfer Detection's `RecipientRuleLearned` — and the nav banner's item count decrements with a digit-roll animation as each is cleared, disappearing entirely once the inbox is empty again

**Developer test flow:**
1. Temporarily lower the classification-confidence threshold via a debug config flag to force more items into the inbox, or seed several unrecognized-merchant transactions via the Simulator

**Test cases:**

- [ ] **REV-01 — Uncertain transactions appear in the Review Inbox** 🔴 `Critical · B4`
    - *Feature:* Low-confidence surfacing
    - *Precondition:* A transaction with low classification confidence exists
    - *Steps:* Open Review Inbox
    - *Expected:* The item is present with a suggested category shown
    - *Notes:* 

- [ ] **REV-02 — Swiping resolves an item correctly** 🟠 `High · B4`
    - *Feature:* Swipe confirm
    - *Precondition:* An item is pending in Review Inbox
    - *Steps:* Swipe to confirm the suggested (or a different) category
    - *Expected:* Item is categorized, removed from the inbox, and the merchant rule is learned
    - *Notes:* 

- [ ] **REV-03 — Bulk re-categorizing by merchant works across items** 🟡 `Medium · B4`
    - *Feature:* Bulk re-categorize
    - *Precondition:* Multiple pending items from the same merchant
    - *Steps:* Use bulk re-categorize for that merchant
    - *Expected:* All matching pending items resolve at once
    - *Notes:* 

- [ ] **REV-04 — Inbox shows the correct empty state when clear** 🟡 `Medium · B4`
    - *Feature:* Empty state
    - *Precondition:* No items pending
    - *Steps:* Open Review Inbox
    - *Expected:* A calm 'all caught up' empty state is shown, not a blank screen
    - *Notes:* 

- [ ] **REV-05 — A likely duplicate transaction is flagged, not silently merged or doubled** 🟠 `High · B4`
    - *Feature:* Duplicate handling
    - *Precondition:* Two very similar notifications arrive close together (simulating a pending-then-posted update)
    - *Steps:* Let both be processed
    - *Expected:* The duplicate is flagged for a decision rather than either double-counted or silently dropped
    - *Notes:* 

---

## Kanban Board

**User flow (UI elements named at each step):**
1. Kanban tab → `StatusKanbanBoard` renders three columns (Upcoming / Due Soon / Settled) → tap any card for its `SciuroBottomSheet` detail → for informal debt cards only, a manual "Mark settled" `SciuroButton` is available (bank-linked cards have no such button — they only ever move automatically)

**How this intertwines with other modules:**
- Every card's column membership is a pure rendering of its underlying RecurringObligation/Debt `status` field — this board owns no data of its own, so it can never drift out of sync with what Debt Ledger or the Budgets/Runway calculations believe is true
- A settlement event moves a card using `animateItem()`/`motion.card-move`, the same spring-based mechanic used for the Review Inbox's card removal, kept visually consistent across the app rather than being a bespoke Kanban-only animation

**Developer test flow:**
1. Seed obligations/debts directly at various status values via the debug data seeder to populate all three columns instantly for a visual QA pass, instead of waiting for real detection cycles

**Test cases:**

- [ ] **KAN-01 — Bills & Debt board shows the correct three columns** 🟠 `High · C1`
    - *Feature:* Column structure
    - *Precondition:* Any obligations/debts exist
    - *Steps:* Open the Kanban board
    - *Expected:* Upcoming, Due Soon, and Settled columns are all present and correctly populated
    - *Notes:* 

- [ ] **KAN-02 — Bank-linked cards move automatically, no drag needed** 🔴 `Critical · C1`
    - *Feature:* Auto-move on settlement
    - *Precondition:* An obligation just settled (OBL-04)
    - *Steps:* View the board
    - *Expected:* The card is in Settled without any manual drag
    - *Notes:* 

- [ ] **KAN-03 — Informal debt cards require a manual settle action** 🟡 `Medium · C1`
    - *Feature:* Manual move for informal debt
    - *Precondition:* An informal debt exists, marked as repaid in real life
    - *Steps:* Mark it settled manually
    - *Expected:* Card moves to Settled only after the manual tap, never automatically
    - *Notes:* 

- [ ] **KAN-04 — Overdue items are visually prioritized** 🟠 `High · C1`
    - *Feature:* Overdue surfacing
    - *Precondition:* An obligation has passed its due date unpaid
    - *Steps:* View the board
    - *Expected:* It surfaces at the top of Due Soon with a distinct visual treatment
    - *Notes:* 

- [ ] **KAN-05 — Board shows the correct state before any obligations exist** 🟡 `Medium · C1`
    - *Feature:* Empty state
    - *Precondition:* Fresh install, no detected obligations yet
    - *Steps:* Open the Kanban board
    - *Expected:* A passive empty state explains detection is still gathering data, with no premature 'add' prompt
    - *Notes:* 

---

## Home Dashboard

**User flow (UI elements named at each step):**
1. Landing screen on open: the Runway hero figure (`HeroPanel`-pattern) leads, with a `PillToggle` for time range if applicable, sitting above a `SheetList` of account `SciuroCard` rows (the sheet-over-hero pull-up layout)
2. Tap the Runway figure → `SharedTransitionLayout` expansion shows exactly which RecurringObligations were subtracted to reach that number
3. Tap a bill in the horizontal "next 3 due" strip → shared-element transition into Kanban, pre-scrolled and highlighting that card

**How this intertwines with other modules:**
- Home is the single screen that aggregates the most other modules at once: Runway depends on Balance & Reconciliation + Recurring Obligations + income-pattern detection; the account rows depend on every transaction/transfer event; the bills strip depends on Kanban's status data; Net Position (if shown here) depends on nearly everything else besides
- Because all of these are derived, not stored, Home never needs to be told to refresh by another module — it simply recomputes whenever its own event subscriptions fire

**Developer test flow:**
1. Seed a specific balance + obligation combination via the debug data seeder to deterministically hit each runway temperature state (healthy/warning/danger) for visual QA

**Test cases:**

- [ ] **HOM-01 — Safe-to-spend figure calculates correctly against known data** 🔴 `Critical · C2`
    - *Feature:* Runway calculation
    - *Precondition:* A known balance and known set of upcoming obligations
    - *Steps:* Check the runway number against a manual calculation
    - *Expected:* Figures match
    - *Notes:* 

- [ ] **HOM-02 — Runway color reflects financial health accurately** 🟡 `Medium · C2`
    - *Feature:* Temperature indicator
    - *Precondition:* Test at healthy, marginal, and low runway states
    - *Steps:* Observe the color at each state
    - *Expected:* Color moves calm to warning to danger appropriately and legibly
    - *Notes:* 

- [ ] **HOM-03 — Next 3 bills due are shown correctly** 🟠 `High · C2`
    - *Feature:* Upcoming bills strip
    - *Precondition:* Multiple obligations with different due dates exist
    - *Steps:* View the Home screen
    - *Expected:* The three soonest-due items are listed in correct order
    - *Notes:* 

- [ ] **HOM-04 — Tapping the runway number opens an accurate breakdown** 🟡 `Medium · C2`
    - *Feature:* Runway drilldown
    - *Precondition:* Runway figure is displayed
    - *Steps:* Tap the number
    - *Expected:* It expands to show exactly which obligations were subtracted
    - *Notes:* 

- [ ] **HOM-05 — Tapping a bill in the strip navigates correctly** 🟡 `Medium · C2`
    - *Feature:* Bill-to-Kanban handoff
    - *Precondition:* A bill is visible in the upcoming strip
    - *Steps:* Tap it
    - *Expected:* Kanban opens pre-scrolled and highlighting that specific card
    - *Notes:* 

---

## Navigation & UI Shell

**User flow (UI elements named at each step):**
1. Bottom nav: 4 fixed destinations (Home · Kanban · Wallet · More) plus one centered, elevated FAB as a 5th visual slot that is not itself a destination
2. Review Inbox is deliberately absent from this bar — see Review Inbox's own flow for how it's actually reached

**How this intertwines with other modules:**
- The FAB's expanded quick-action menu is a dispatch point into three other modules at once (Physical Cash Wallet, Debt Ledger, manual transaction entry) — it doesn't own any of that logic itself, it's purely a navigational shortcut

**Developer test flow:**
1. Compose UI/instrumentation tests asserting every destination is reachable and every back-stack pops correctly; manual pass specifically for the predictive-back gesture feel, since that's not meaningfully assertable in an automated test

**Test cases:**

- [ ] **NAV-01 — Bottom nav shows exactly 4 destinations plus a centered FAB** 🟡 `Medium · C1-C3`
    - *Feature:* Nav structure
    - *Precondition:* App is in normal use
    - *Steps:* Observe the bottom nav bar
    - *Expected:* Home, Kanban, Wallet, and More are present, with one centered elevated FAB, matching no more/fewer
    - *Notes:* 

- [ ] **NAV-02 — All quick-add actions function from the FAB** 🟠 `High · C1-C3`
    - *Feature:* FAB quick-add
    - *Precondition:* FAB is tapped
    - *Steps:* Select each quick-add option in turn (log cash, add informal debt, manual transaction)
    - *Expected:* Each opens the correct minimal-input flow and saves correctly
    - *Notes:* 

- [ ] **NAV-03 — Review Inbox never occupies a permanent nav slot** 🟡 `Medium · B4/C1`
    - *Feature:* Review Inbox is not a tab
    - *Precondition:* Review Inbox has pending items
    - *Steps:* Check the nav bar and the entry point used instead
    - *Expected:* It's absent from the tab bar; a banner/badge surfaces it only while non-empty
    - *Notes:* 

- [ ] **NAV-04 — Every core screen is reachable and returns cleanly** 🔴 `Critical · C1-C3`
    - *Feature:* No dead ends
    - *Precondition:* Normal app use
    - *Steps:* Navigate through every screen and back
    - *Expected:* No crashes, no dead-end screens, back navigation always works
    - *Notes:* 

---

## Security

**User flow (UI elements named at each step):**
1. Settings → toggle biometric/PIN lock (standard list row + switch) → close and reopen the app → system biometric prompt gates entry
2. Settings → Backup → `SciuroButton` "Export" → system share sheet with the resulting encrypted file

**How this intertwines with other modules:**
- Security sits underneath every other module rather than beside them — it doesn't react to Domain Events, it gates access to the surface all of them render on

**Developer test flow:**
1. adb pull the raw SQLite file and inspect it with a hex viewer / file command — it should not show a readable 'SQLite format 3' header if encryption is working
2. Route the device through a debugging proxy (e.g. mitmproxy) during a normal session to confirm no transaction text, amounts, or account identifiers appear in any outbound analytics/crash payload

**Test cases:**

- [ ] **SEC-01 — Local database is encrypted, not plaintext** 🔴 `Critical · D1`
    - *Feature:* Encryption at rest
    - *Precondition:* App has real data stored
    - *Steps:* Attempt to inspect the raw database file directly
    - *Expected:* Contents are not human-readable without the encryption key
    - *Notes:* 

- [ ] **SEC-02 — Biometric/PIN gate blocks unauthorized access** 🔴 `Critical · D1`
    - *Feature:* App gate
    - *Precondition:* App lock is enabled
    - *Steps:* Attempt to open the app without authenticating
    - *Expected:* Access is blocked until authentication succeeds
    - *Notes:* 

- [ ] **SEC-03 — Manual backup export produces a usable, protected file** 🟠 `High · D1`
    - *Feature:* Encrypted export
    - *Precondition:* Some data exists to export
    - *Steps:* Trigger the export/backup flow
    - *Expected:* A file is produced, and it's encrypted (not a plain readable export)
    - *Notes:* 

- [ ] **SEC-04 — No personal/financial data appears in logs if analytics are enabled** 🔴 `Critical · D1`
    - *Feature:* No PII leakage
    - *Precondition:* Any analytics/crash reporting is active
    - *Steps:* Trigger a typical session and inspect any logs sent externally
    - *Expected:* No transaction text, amounts, or account identifiers appear
    - *Notes:* 

- [ ] **SEC-05 — Raw notification text purge option works as configured** 🟡 `Medium · D1`
    - *Feature:* Raw text purge
    - *Precondition:* Purge-after-N-days is enabled in Settings
    - *Steps:* Wait past the configured window (or simulate it)
    - *Expected:* Raw notification text is removed while structured transaction data remains intact
    - *Notes:* 

---

## Empty/Loading States

**User flow (UI elements named at each step):**
1. Any screen visited before its underlying data exists renders `EmptyStateView` (tailored copy + monochrome squirrel pose per screen, see the earlier empty-state table)
2. A cold data load renders `SkeletonBlock`-composed shimmer shapes matching the eventual layout, before either real content or the empty state resolves

**How this intertwines with other modules:**
- These states are themselves reactions to the *absence* of events from other modules — an empty Kanban board, for instance, is simply what Kanban Board's `StatusKanbanBoard` renders when no RecurringObligation/Debt events have fired yet, not a separate code path to keep in sync

**Developer test flow:**
1. Add a debug menu toggle to force any screen into its empty state or a simulated slow-load state on demand, instead of wiping the database or throttling the network by hand each time

**Test cases:**

- [ ] **UIX-01 — Every major screen has an appropriate first-use empty state** 🟡 `Medium · B/C (per-module)`
    - *Feature:* Empty states coverage
    - *Precondition:* Fresh install or a screen with no data yet
    - *Steps:* Visit Home, Wallet, Investment, Kanban, Budgets, Debt Overview, Review Inbox in turn
    - *Expected:* Each shows a tailored, non-generic empty state rather than a blank or broken layout
    - *Notes:* 

- [ ] **UIX-02 — Cold loads show skeleton placeholders, not blank spinners** 🟡 `Medium · C1-C3`
    - *Feature:* Skeleton loading
    - *Precondition:* Cold app start with data to load
    - *Steps:* Open the app fresh
    - *Expected:* Shimmer/skeleton shapes matching the eventual layout appear briefly before real content
    - *Notes:* 

- [ ] **UIX-03 — One-tap confirms update instantly, not after a wait** 🟠 `High · B4/C1-C3`
    - *Feature:* Optimistic UI
    - *Precondition:* Any one-tap confirm action (Review Inbox, mark bill settled)
    - *Steps:* Perform the action
    - *Expected:* UI updates immediately; the background save happens invisibly after
    - *Notes:* 

- [ ] **UIX-04 — Background sync status is visible without being intrusive** 🟢 `Low · B3/C2`
    - *Feature:* Sync freshness indicator
    - *Precondition:* Normal background notification processing is occurring
    - *Steps:* Check the Home header area
    - *Expected:* A subtle 'synced Xm ago' indicator is present and accurate, not a blocking spinner
    - *Notes:* 

- [ ] **UIX-05 — System reduced-motion setting disables Lottie/confetti correctly** 🟡 `Medium · C/D1`
    - *Feature:* Reduced motion respect
    - *Precondition:* OS-level reduced motion is enabled
    - *Steps:* Trigger a celebration moment (e.g. debt payoff)
    - *Expected:* A static fallback appears instead of the full animation, no crash or broken layout
    - *Notes:* 

---

## Settings

**User flow (UI elements named at each step):**
1. Settings tab → standard list rows with switches/values (allowlist management, LLM opt-in, appearance, backup) — deliberately the calmest, least animated screen in the app, consistent with it being a utility surface rather than a glanceable one

**How this intertwines with other modules:**
- The LLM opt-in toggle is the one Settings switch with a direct cross-module effect: flipping it off immediately stops Parsing's LLM-fallback path and Triage's LLM-critic pass from making any network call, falling back to regex-only and lower-confidence-routes-to-Review-Inbox behavior instead

**Developer test flow:**
1. For the LLM opt-in toggle specifically: verify via a network proxy that no external call fires while it's off, and that fallback resumes correctly once re-enabled

**Test cases:**

- [ ] **SET-01 — Adding/removing tracked apps works correctly** 🟠 `High · A2/D1`
    - *Feature:* Allowlist management
    - *Precondition:* Settings > notification sources
    - *Steps:* Add or remove an app from the allowlist
    - *Expected:* Future notifications from that app are captured or ignored accordingly
    - *Notes:* 

- [ ] **SET-02 — Enabling/disabling LLM-assisted parsing works as expected** 🔴 `Critical · A4/D1`
    - *Feature:* LLM opt-in toggle
    - *Precondition:* Settings > AI parsing
    - *Steps:* Toggle it off, then trigger a parse-fallback scenario
    - *Expected:* No external call is made while off; fallback resumes correctly once re-enabled
    - *Notes:* 

- [ ] **SET-03 — Light/Dark/System selection applies across the whole app** 🟡 `Medium · D1`
    - *Feature:* Appearance setting
    - *Precondition:* Settings > Appearance
    - *Steps:* Switch between all three options
    - *Expected:* Every screen updates consistently, no screens left in the wrong theme
    - *Notes:* 

- [ ] **SET-04 — Manual export/backup completes and includes expected data** 🟠 `High · D1`
    - *Feature:* Backup trigger
    - *Precondition:* Settings > Backup
    - *Steps:* Trigger an export
    - *Expected:* A complete, correct file is produced covering all modules (not just transactions)
    - *Notes:* 

---

## Desktop Companion

**User flow (UI elements named at each step):**
1. Desktop app mirrors the phone's `HeroPanel`/`SheetList` pattern in a read-only layout — no FAB, no swipe actions, no bottom sheets, since nothing here is meant to be edited

**How this intertwines with other modules:**
- Purely a downstream renderer of whatever the phone's event-sourced state currently is at sync time — it originates no Domain Events of its own

**Developer test flow:**
1. Point a desktop debug build at a test export/sync file with known seeded values and compare every rendered figure against the expected values by hand

**Test cases:**

- [ ] **DSK-01 — Windows desktop viewer displays synced data correctly** 🟢 `Low · Deferred/Future`
    - *Feature:* Read-only sync view
    - *Precondition:* Desktop companion is set up and synced
    - *Steps:* Open the desktop app
    - *Expected:* Balances, obligations, and recent activity match the phone, view-only
    - *Notes:* 

- [ ] **DSK-02 — Desktop correctly falls back where Lottie doesn't render** 🟢 `Low · Deferred/Future`
    - *Feature:* Lottie fallback on desktop
    - *Precondition:* A moment that uses Lottie on Android (e.g. empty state)
    - *Steps:* View the equivalent screen on desktop
    - *Expected:* A static image appears in place of the animation, no error or blank space
    - *Notes:* 

---