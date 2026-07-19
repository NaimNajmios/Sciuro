# Sciuro — UAT Checklist

**116 test cases across 23 modules.** Companion checklist to `sciuro-uat-test-tracker.xlsx` — same 116 tests, Obsidian/markdown-native checkbox format instead of a spreadsheet.

## How to use this

- Check a box `- [x]` once a test **passes**. Leave it unchecked if not yet tested or failing.
- Failed tests: don't check the box — add a line under **Notes** describing what actually happened.
- **Phase** tags reference the v4 engineering plan's milestones (`A0–A6`, `B1–B7`, `C1–C3`, `D1–D3`) — a guide for sequencing, not a rigid contract.
- Priority: 🔴 Critical (must be correct before anything else is trustworthy) · 🟠 High (core module correctness) · 🟡 Medium (UX polish/secondary paths) · 🟢 Low (nice-to-have/deferred).
- Bank/e-wallet sources assumed throughout: CIMB, Maybank, BSN, Touch 'n Go eWallet, GrabPay, Boost, ShopeePay.

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
