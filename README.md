# PlaneCatcher

A plane-collecting hobby app for Android. Real aircraft flying within 10 miles show up on a radar. Answer a multiple-choice question about a plane to catch it, then build a collection ranked by rarity. Plane data comes from [airplanes.live](https://airplanes.live) and photos from [planespotters.net](https://www.planespotters.net).

CSCN 375 project by Nathaniel Cash and Robert Vander Pyl.

## Features

| Feature | Where |
| --- | --- |
| Live planes within 10 mi (query 9 nm, filter on device with Haversine), poll every 30 s, back off 60 s on errors or HTTP 429 | `domain/RadarTracker.kt`, `core/radar/Radar.kt` |
| Radar view with sweep, tier-coloured blips and a nearby list | `ui/home/` |
| Pop-up with photo, photographer credit, type, callsign and tier; silhouette when there's no photo | `ui/home/PlanePopup.kt` |
| Quiz gate: 4 options, curated questions for popular types, generated fallback (model, maker, engines, seats, airline, registration country, altitude) | `core/quiz/` |
| Wrong answer locks that plane for 1 hour; the retry gets a different question | `core/rules/QuizLock.kt`, `domain/CatchManager.kt` |
| Collection grid with tier filter, sort by date or rarity, totals and points; detail screen | `ui/collection/` |
| Tiers (Common 1, Rare 5, Epic 20, Legendary 100) from `assets/tiers.json`, military at least Epic | `core/tier/TierTable.kt` |
| "Planes nearby" alerts, once per plane, with a minimum-tier filter | `notify/` |
| Optional background radar (foreground service, no background-location permission) | `service/RadarService.kt` |
| Location Jump: once per 24 h, up to 3 h, ends on the first catch; cooldown uses a tamper-resistant clock | `core/rules/LocationJump.kt`, `data/time/TrustedClock.kt` |
| Onboarding asks for location, then notifications | `ui/onboarding/` |
| Mock-location detection blocks catches (except during a jump) | `data/location/LocationSource.kt` |

The UI is dark and high-contrast, and the collection is stored only on the device, so it works offline. Both choices come from the user interviews: people use the app outdoors, and they wanted it to work without a connection.

## Project layout

```
core/   Pure Kotlin game logic: rules, tiers, quiz generator, radar maths. No Android, fully unit-tested.
app/    Android app: Kotlin, Jetpack Compose, Material 3, MVVM, Hilt, Room, DataStore, Retrofit, Coil.
```

## Building

Requirements: Android Studio (Ladybug or newer) or the Android SDK with platform 35, and JDK 17+.

1. Open the project folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on a device or emulator (Android 8.0+, API 26).

From the command line:

```bash
./gradlew :core:test          # game-logic unit tests
./gradlew :app:assembleDebug  # builds app/build/outputs/apk/debug/app-debug.apk
```

On an emulator, set a location near an airport (Extended controls → Location) so planes appear. Location-spoofing apps are detected and block catching; if that happens on your test device, use a Location Jump instead.

## Rules in one place

All tunable numbers live in `core/src/main/kotlin/com/planecatcher/core/rules/GameRules.kt`: catch range, quiz lock, jump duration and cooldown, and poll intervals. To change tiers without a code change, edit `app/src/main/assets/tiers.json`.

## Notes and limits

- airplanes.live is free for non-commercial use only, with no uptime guarantee and about 1 request per second. The app spaces requests at least 2 s apart. A public release would need a licensed data source or a small proxy server.
- Without a backend, the jump cooldown uses server `Date` headers plus `elapsedRealtime`, so changing the phone clock doesn't reset it. A reboot while offline falls back to the phone clock, but never earlier than the last trusted time.
- The feature is called "Location Jump" everywhere and doesn't use `VpnService`, in line with Google Play's VPN policy.
- Not built yet (outline phases 5 and 6): sounds, cloud sync and accounts, leaderboards, missions.
