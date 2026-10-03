# Wata

A minimal water-drinking reminder. Kotlin Multiplatform + Compose Multiplatform, Android target.

- Log a sip (100 ml), glass (250 ml) or bottle (500 ml); undo the last one.
- Reminders fire one interval after your last drink, only within active hours, and pause once the daily goal is reached.
- The notification's **Drank a glass** action logs 250 ml without opening the app.

## Structure

- `composeApp/src/commonMain` — all UI and logic (`data/ReminderPlanner.kt` holds the scheduling rules).
- `composeApp/src/androidMain` — platform layer: SharedPreferences store, exact alarms, notifications, boot/time-change receivers.
- `composeApp/src/commonTest` — planner and repository tests.

## Build & install

```bash
./gradlew :composeApp:testDebugUnitTest
```

```bash
./gradlew :composeApp:installRelease
```

The release build is minified and signed with the debug key so it can be sideloaded directly.
