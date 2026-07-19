# Sciuro End-to-End User Test Plan

This document serves as the master checklist for manually testing Sciuro's functionality as an end-user. It covers the complete lifecycle of the app from a clean install through advanced engine logic.

---

## 1. First-Time Setup & Onboarding
**Objective**: Verify the app initializes correctly and requests necessary system permissions.
- [ ] **Clean Install**: Install the app and verify the splash screen launches.
- [ ] **Empty States**: Navigate through the Dashboard, Kanban, Wallet, and Budget tabs to verify that the `EmptyStateView` components display correctly (e.g., "Nothing gathered yet", "No cash tracked yet").
- [ ] **Permissions**: Ensure the app prompts you to grant Notification Listener access (required for `NotificationSourceAdapter` to work).
- [ ] **Settings / Opt-ins**: Navigate to the settings/more screen and toggle the LLM-assisted parsing opt-in (Groq Llama 3 fallback).

## 2. Passive Data Capture (Ingestion & Parsing)
**Objective**: Verify that the app silently captures and parses notifications without user intervention.
- [ ] **Deterministic Parsing (Bank)**: Simulate or trigger a real notification from Maybank, CIMB, or BSN. Open Sciuro and verify the transaction appears instantly in the Ledger.
- [ ] **Deterministic Parsing (E-Wallet)**: Trigger a notification from TNG, GrabPay, or ShopeePay. Verify the transaction amount, merchant, and timestamp were parsed perfectly.
- [ ] **LLM Fallback**: Send a mock notification from a banking app with an unrecognizable format. Verify that the `LlmFallbackParser` kicks in, parses the data, and tags it with an AI Confidence score.

## 3. Manual Review Inbox & Triage
**Objective**: Verify that low-confidence AI parses require human validation via the Inbox.
- [ ] **Inbox Warning**: Trigger a transaction with an AI Confidence < 85%. Navigate to the Dashboard and verify the red "Review Inbox" warning card appears showing `1 items pending your review`.
- [ ] **Transaction Inspector**: Tap the pending transaction to launch the `TransactionInspectorSheet` (ModalBottomSheet).
- [ ] **Visual Signals**: Verify the AI Confidence displays correctly (Green for >85%, Amber for 50-84%, Red for <50%).
- [ ] **Categorization**: Use the dropdown inside the bottom sheet to change the category (e.g., from "General" to "Food & Dining").
- [ ] **Save & Ignore**: Tap "Save Changes" and verify the transaction is updated in the Ledger and removed from the Inbox. Trigger another one and tap "Ignore" to verify it is discarded.
- [ ] **Predictive Back**: While the bottom sheet is open, use the native Android back swipe gesture to verify it smoothly dismisses.

## 4. Dashboard & Wallet Visualizations
**Objective**: Verify UI reactivity and state rendering.
- [ ] **Hero Panel & WaveChart**: After adding transactions, verify the "Total Net Worth" updates dynamically in the Dashboard's `HeroPanel`. Check if the `WaveChart` bezier curve draws data points (if mock data is injected/available).
- [ ] **Wallet Switcher**: Navigate to the Wallet screen. Toggle the `PillToggle` between "Liquid Cash" and "Investments".
- [ ] **Typography**: Ensure all transaction amounts are formatted using the tabular `IBM Plex Mono` font to ensure numbers align cleanly.

## 5. Kanban Board (Bills & Debt)
**Objective**: Verify obligation tracking and board interaction.
- [ ] **Auto-Detection**: Make 2-3 similar payments to the same merchant across a simulated timeframe. Verify the `ObligationDetectionEngine` flags it as a recurring bill and adds it to the board.
- [ ] **Manual Task Status Update**: In the Kanban screen, move a bill from the "TODO" column to the "DONE" column. Verify that the UI updates instantly.

## 6. Budget Engine
**Objective**: Verify the 30-day rolling budget logic.
- [ ] **Create Budget**: Create a monthly budget for a specific category (e.g., RM500 for "Transport").
- [ ] **Track Expense**: Make a transaction that categorizes as "Transport".
- [ ] **Verify Breakdown**: Navigate to the Budgets screen and verify the `BudgetProgressRow` animates a progress bar showing the exact amount spent versus the limit.

## 7. Advanced: Transfer Reconciliation
**Objective**: Verify that moving money between your own accounts doesn't count as double-spending.
- [ ] **Simulate Transfer**: Trigger an outflow notification from Bank A (e.g., RM100 out of Maybank) and immediately trigger an inflow notification to Bank B (e.g., RM100 into TNG e-wallet).
- [ ] **Engine Verification**: Verify that the `TransferDetectionEngine` intercepts both transactions and links them as a single `TransferLink` instead of logging RM100 in Expenses and RM100 in Income.

## 8. Advanced: Database Security
**Objective**: Verify that sensitive ledger data isn't leaking to cloud backups.
- [ ] **Backup Test**: Run an ADB backup command (`adb backup -f mybackup.ab -apk com.najmi.sciuro`) or check Google Drive app backup sizes. Ensure the SQLDelight `.db` files are completely excluded from the backup payload.
