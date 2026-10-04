# BTR

An Android app for self-discipline. BTR locks distracting apps (social media, novel readers, etc.) during time windows you choose, and will grow into a full daily-habits companion.

## Modules

| # | Module | Status |
|---|--------|--------|
| 1 | **App Lock**: pick apps, set customisable lock schedules, show a block screen when a locked app is opened | In progress |
| 2 | **Habit Tracker**: daily habits, streaks, reminders | Planned |
| 3 | **Integration**: link habits and locks (e.g. finish a habit to earn unlock time) | Planned |

See [docs/roadmap.md](docs/roadmap.md) for details.

## Tech stack

- Kotlin + Jetpack Compose
- Room (local storage)
- `AccessibilityService` for detecting and blocking apps
- Single `:app` module for now (split into `:core` / `:feature-*` modules planned)

## What's in the app

- **Welcome screen**: shown on first launch with the BTR poster: *Blood, Tears & Respect: "The first two things you give, the last thing you get."*
- **Home**: a status banner, a "Go dark" card that locks every app at once, and an expandable card per app with its own lock duration (apps can be removed here, but not while locked)
- **Apps**: pick which installed apps go on the block list (only shows apps not yet added)
- **Rules**: one-off lock sessions and recurring schedules (days + time window, including overnight)
- **History**: log of emergency unlocks
- **Block screen**: covers a locked app. An emergency unlock needs a reason and an exercise challenge

## Getting started

Requires Android Studio (latest stable) and an Android device or emulator (Android 8.0+).
