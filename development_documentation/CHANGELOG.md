# Changelog

All notable changes to this project will be documented in this file.

## [Unreleased]
### Added
- Multi-step Onboarding setup flow capturing user's initial cash balance.
- Soft-deletion mechanics for `Account` and `Investment` via new `status` column.
- Undeletable constraint for the core system "Personal Wallet" utilizing a new `is_system` flag.
- Safe `archiveAccount` action in the Wallet UI for non-system accounts.
- Initial project scaffold and documentation structure setup.
