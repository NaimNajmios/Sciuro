# ADR 001: Koin over Hilt for Dependency Injection

## Date
2026-07-19

## Context
We need a dependency injection framework for the Sciuro app. The existing habit has been to use Hilt, which is robust for Android-only development. However, the requirement for Sciuro is that it must be Kotlin Multiplatform (KMP) from day one, with a roadmap that includes a Windows desktop application.

## Decision
We will use Koin for dependency injection instead of Hilt.

## Rationale
- Hilt is strictly bound to Android and the Dagger ecosystem, making it incompatible with Kotlin Multiplatform (KMP).
- Koin is a lightweight, pragmatic dependency injection framework written purely in Kotlin that fully supports Kotlin Multiplatform.
- Choosing Koin upfront avoids the need for a costly migration later when extending the app to other platforms (e.g., Windows desktop).

## Consequences
- The development team will need to use Koin's DSL for module configuration rather than Dagger/Hilt annotations.
- Less compile-time safety compared to Hilt, but we gain the multiplatform capability which is a strict project requirement.
