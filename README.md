# BTR

An Android app for self-discipline. BTR locks distracting apps (social media, novel readers, etc.) during time windows you choose, and will grow into a full daily-habits companion.

## Modules

| # | Module | Status |
|---|--------|--------|
| 1 | **App Lock**: pick apps, set customisable lock schedules, show a block screen when a locked app is opened | Planned |
| 2 | **Habit Tracker**: daily habits, streaks, reminders | Planned |
| 3 | **Integration**: link habits and locks (e.g. finish a habit to earn unlock time) | Planned |

See [docs/roadmap.md](docs/roadmap.md) for details.

## Tech stack

- Kotlin + Jetpack Compose
- Room (local storage)
- `UsageStatsManager` + `AccessibilityService` for detecting and blocking apps
- `WorkManager` / `AlarmManager` for schedules
- Multi-module Gradle: `:app`, `:core`, `:feature-applock`, `:feature-habits`

## Getting started

Requires Android Studio (latest stable) and an Android device or emulator (Android 8.0+).
