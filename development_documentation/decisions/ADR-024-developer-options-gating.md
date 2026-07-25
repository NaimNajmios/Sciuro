# ADR-024: Developer Options Gating

## Status

Accepted

## Context

The Developer Options screen in Sciuro provides powerful tools for debugging the notification ingestion pipeline, including batch test runners, database clearing, and source package management. These tools are essential for development but dangerous for non-technical users — a single tap on "Clear Inbox" can permanently delete all unreviewed transactions, and modifying the notification source allowlist can break real-time transaction capture.

The team needed a way to keep these tools accessible for developers while preventing accidental access by regular users.

## Options Considered

### Option 1: No Gating (Status Quo)
- **Description:** Developer Options always visible in Settings
- **Pros:** Zero implementation effort, always accessible
- **Cons:** High risk of accidental data loss, confusing for non-technical users, cluttered Settings UI

### Option 2: Build Variant Gating
- **Description:** Developer Options only included in `debug` build variant
- **Pros:** Complete separation, impossible to access in production
- **Cons:** Can't test developer tools on production-like builds, requires maintaining two Settings screens

### Option 3: Hidden Activation Pattern (Android Developer Options Style)
- **Description:** Hidden behind a secret gesture (7-tap activation), persisted via SharedPreferences
- **Pros:** Familiar pattern (Android Settings), single codebase, persistent across restarts, discoverable for developers
- **Cons:** Still technically accessible to determined non-technical users

### Option 4: Biometric/PIN Gate
- **Description:** Require biometric or PIN authentication before accessing Developer Options
- **Pros:** Strong security, leverages existing BiometricGate
- **Cons:** Adds friction for developers during frequent testing, doesn't match Android conventions

## Decision

**Option 3: Hidden Activation Pattern**

The Developer Options section is hidden by default in the Settings screen and revealed via a hidden activation: tapping the "Developer Options" section header 7 times in quick succession. Once revealed, the setting persists via `EncryptedSharedPreferences` and remains visible across app restarts.

## Consequences

### Positive
- Non-technical users never see Developer Options by default
- Developers get a familiar, low-friction activation pattern (matches Android Settings)
- Single codebase — no build variant complexity
- Persistent setting — activate once, always available
- Discoverable for developers who know the pattern

### Negative
- Not a security boundary — determined non-technical users could theoretically discover the pattern
- Adds 3 new methods to `SettingsProvider` interface (interface pollution)
- Requires updating `EncryptedSettingsProvider` implementation

### Trade-offs Accepted
- Security vs. convenience: We accept that this is not a security boundary but a UX guardrail
- Interface bloat: We accept adding methods to `SettingsProvider` for this feature

## Implementation Notes

- `SettingsProvider.isDeveloperOptionsVisible()` / `setDeveloperOptionsVisible()` added to interface
- `EncryptedSettingsProvider` implements using `EncryptedSharedPreferences`
- `SettingsViewModel` exposes state and setter method
- `SettingsSectionHeader` updated to accept `Modifier` parameter for clickable behavior
- 7-tap counter resets after activation, shows Snackbar confirmation
