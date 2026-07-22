# ADR-021: Account Enrichment — Number, Holder, Bank Code, QR

**Date:** 2026-07-22

**Status:** Accepted

## Context
Transfer detection relied solely on amount+time coincidence, producing false positives when unrelated transactions matched. Adding account identity (account number, holder name, bank code) to the `account` schema enables deterministic self-transfer matching. QR code storage was added for quick-access display during peer-to-peer payments.

## Decision
1. **Five new nullable columns** on `account`: `account_number`, `account_holder_name`, `bank_institution_code`, `qr_image_path`, `qr_payload_text`. All nullable — data entry is manual and one-time.
2. **`account_pair_confirmation` table**: Tracks which (account_a, account_b) pairs have been human-confirmed, enabling auto-linking on future heuristic matches.
3. **QR images stored in app-private storage** (`filesDir/qr_codes/`), not as URIs — survives file moves and gallery deletions. Copied from photo picker result via `ContentResolver`.
4. **No Coil/Glide dependency**: `BitmapFactory.decodeFile()` + `asImageBitmap()` is sufficient for a single static image.

## Consequences
- Schema migration `3.sqm` required alongside `CREATE TABLE` updates in `Account.sq`.
- `Account` domain model grows 5 fields, all defaulting to `null` — no existing call sites break.
- `AccountRepository.updateAccount()` becomes bulkier but remains a single atomic operation.
- QR images consume local storage proportional to the number of saved QR codes (typically 1–2 images, negligible).
