# FFA � Flash For Alert

Native Android utility that triggers sharp camera-flash alerts for notifications from user-selected apps, controlled through a Quick Settings tile.

## Features
- Quick Settings tile (`FFA`) to activate/deactivate flash alerts.
- Notification detection via `NotificationListenerService` (no polling, no AccessibilityService).
- Per-app selection with real installed launcher apps, search, and **Turn On All**.
- Default notification signature: **OFF 120ms ? ON 80ms ? OFF 120ms ? ON 80ms ? OFF** (2 pulses).
- Notification presets (iPhone Sharp default, Fast, Balanced, Soft, Custom) and manual ON/OFF ms inputs.
- Separate call flash timing controls (call-category notification detection).
- Torch brightness/dim control on supported Android 13+ hardware (`turnOnTorchWithStrengthLevel`); graceful fallback on unsupported devices.
- Hard-coded 40ms vibration toggle (Settings only, no duration config).
- Hard-coded always enabled: flash while locked, unlocked, and in silent mode.
- DataStore persistence for settings and selected apps.

## Build
Requirements:
- Android SDK installed (`local.properties` points to SDK).
- JDK 17.
- Gradle wrapper included.

```powershell
.\gradlew.bat :app:assembleDebug
```

Debug APK output:
`app/build/outputs/apk/debug/app-debug.apk`

## Setup / Use
1. Install APK on an Android device with camera flash.
2. Launch FFA and grant **Notification access** when prompted.
3. Add **FFA** tile to Quick Settings (Edit tiles / pencil).
4. Select apps in FFA.
5. Tap FFA tile to activate. Selected app notifications trigger the flash pattern.

## Permissions
- `BIND_NOTIFICATION_LISTENER_SERVICE` (service): notification callbacks.
- `BIND_QUICK_SETTINGS_TILE` (service): Quick Settings tile.
- `VIBRATE`: optional 40ms vibration on trigger.
- `QUERY_ALL_PACKAGES`: complete launcher app list for per-app selection; no app data collected/transmitted.

## Notes
- Physical device verification not performed in this build environment.
- Call detection uses call-category notifications; behavior can vary by dialer/OS.
- Torch dimming depends on hardware support; unsupported devices use standard torch ON/OFF.
- OEM background/camera restrictions may affect behavior.