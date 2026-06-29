# MINI POS

A native Android app for small shopkeepers to run sell / buy / expense / due bookkeeping and
inventory for one or more shops. Single user, on-device, no accounts.

## Tech stack
- **Kotlin · Jetpack Compose (Material 3) · Room (SQLite)**, MVVM.
- **Offline-first:** 100% on-device, no network/cloud/sign-in/analytics/ads.
- **Multi-shop:** every shop's data is fully separate (each shop-owned row carries `shopId`).
- **Currency:** BDT (symbol `৳`, editable per shop). Money stored as **Long paisa**; dates as **Long millis**.

## Source of truth (read these)
- **BUILD_PLAN.md** — WHAT to build: full spec + the build phases.
- **CONVENTIONS.md** — HOW to build it: package layout, color tokens, MVVM + Room rules.
- **PROGRESS.md** — WHERE we are: current position, task checklist, session log. Keep it updated after each task.

## Rule
**Always read BUILD_PLAN.md and PROGRESS.md before starting work.** Follow CONVENTIONS.md exactly.

## Build
`JAVA_HOME="C:/Program Files/Android/Android Studio/jbr"` then `./gradlew :app:assembleDebug`.
(AGP 9 uses built-in Kotlin — no `kotlin-android` plugin.)
