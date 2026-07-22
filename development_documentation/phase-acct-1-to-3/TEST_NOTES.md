# Test Notes: Phase Acct-1–3 — Account Data Enrichment

## Scope
- Extended `account` table with 5 new columns: `account_number`, `account_holder_name`, `bank_institution_code`, `qr_image_path`, `qr_payload_text`.
- Created `account_pair_confirmation` table for tracking human-confirmed transfer pairs.
- Added `counterpartyAccountNumber` field to `StructuredDraft` with regex extraction in `RegexExtractors.kt`.
- Added `matchesAccountSuffix()` helper for masked-number matching.
- Added `EditAccountDetailsSheet` bottom sheet with fields for account number, holder name, bank code.
- Added QR code image capture via system photo picker, copy to app-private storage, and display on detail screen.
- Extended `Account.sq` queries, domain model, and `AccountRepository` for all new fields.

## Results
- `:core-ledger`, `:core-parsing`, `:feature-wallet` compile and build successfully.
- Schema migration `3.sqm` validated against SQLDelight code generation.
- Account detail screen renders correctly with overflow menu "Edit Details" option.
- Edit bottom sheet opens and persists account number, holder name, bank code.
- QR code can be selected from gallery, copied to `filesDir/qr_codes/`, and displayed as thumbnail + fullscreen dialog.
- All existing call sites (`WalletViewModel.addAccount`, `AccountDetailViewModel.updateAccountColor`) unaffected by new nullable fields (default to null).

## Excluded
- QR payload decode / EMV-QR parsing (Transfer-3, stretch goal). `qr_payload_text` column exists but is never populated.
- Camera capture (photo picker only). No CAMERA permission requested.
- Coil or any image-loading dependency. Uses `BitmapFactory.decodeFile()` + `asImageBitmap()` — same pattern as existing app icon rendering.
