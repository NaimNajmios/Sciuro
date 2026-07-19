# ADR 004: Ingestion Pipeline & Staging

## Context
Sciuro needs to ingest transactions from varied sources (Notifications today, potentially SMS/Email tomorrow). We need an abstraction that treats all incoming data equally before handing it off to the parsers.

## Decision
1. **Source Agnostic**: We created the `RawEvent` model and `IngestionSource` interface. Parsers will consume `RawEvent`s and don't need to care if they came from an Android notification or a manual entry.
2. **Notification Pipeline**: The `SciuroNotificationService` (running as an Android `NotificationListenerService`) intercepts notifications.
3. **Strict Filtering**: To minimize load and protect privacy, the service immediately discards any notification not originating from a predefined set of financial packages defined in `IngestionConfig`.
4. **Staging Buffer**: Events are pushed into a `MutableSharedFlow` in the `NotificationSourceAdapter`, which acts as an in-memory queue. If the app is killed, the Android Notification history might be lost unless persisted (currently kept in memory for simplicity; persistence can be added if needed, but notification processing usually happens in real-time).

## Consequences
- Expanding to SMS parsing later only requires building an `SmsSourceAdapter` that emits `RawEvent`s into the same pipeline.
