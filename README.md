# VIP Caller

**Version 2.0** · [Changelog](CHANGELOG.md)

**VIP Caller** is a smart Android utility app that helps users recognize important incoming calls by playing the caller's custom ringtone.

The app works without root and stores everything locally on the device.

---

## Overview

You build a **VIP list** inside the app, imported from your phonebook. When someone on that list calls, VIP Caller plays their ringtone so you know who it is without looking at the screen.

The system can run **24/7** or only during **days and hours you choose**, a master switch turns it off and on at any time, and the app itself can be locked behind a PIN or your fingerprint.

---

## Features

### VIP contact list
- Import contacts from the device phonebook with multi-select
- Search by name or number, with Arabic-aware matching — `احمد` finds `أحمد`, and Arabic-Indic digits (`٠١٢٣`) match numbers stored in Latin digits
- Results are ranked, so names beginning with your query come first
- Enable or disable each contact individually
- Delete one contact or clear the whole list
- Duplicate numbers are detected and skipped on import

### Scheduling and control
- Master switch to turn the whole system off and on instantly
- **Always on** mode (24/7), or a **time window** mode
- Per-day selection (Saturday → Friday)
- Overnight windows are supported (e.g. 22:00 → 06:00)
- A **Stop** action directly in the ongoing notification

### App lock
- Optional 4-digit PIN required to open the app, so the VIP list stays private if someone else picks up the phone
- Fingerprint unlock, with the PIN always available as a fallback (Android 9+)
- The PIN is never stored — only a salted PBKDF2 hash of it
- Failed attempts are rate limited with an escalating delay that survives restarting the app
- Optional hiding of app content from the recent-apps preview

### Alert behavior
- The system ringer is silenced while a VIP ringtone plays, so a VIP call rings once instead of overlapping with the system's own ringtone (requires Do Not Disturb access)
- Vibration alongside the ringtone (toggleable)
- Override silent mode by routing audio through the alarm stream (toggleable)
- Maximum ringtone duration (10–120 s) as a safety cap
- Ringtone priority: in-app choice → contact's system ringtone → device default
- Stops immediately when the call is answered, rejected, or ended

### Reliability
- Ongoing foreground service keeps the process alive on restrictive OEMs
- Restarts automatically after reboot and after app updates
- Built-in shortcuts to battery-optimization exemption and OEM auto-start settings

---

## Device Compatibility

- **Minimum:** Android 7.0 Nougat (API 24)
- **Target:** Android 16 (API 36)

### Honor X9d and MagicOS devices

MagicOS aggressively terminates background apps. The **Device compatibility** section inside the **الإعدادات** (Settings) tab opens the required screens directly. For stable operation on Honor X9d:

1. Grant the requested permissions on first launch.
2. Tap **استثناء من تحسين البطارية** and allow it.
3. Tap **التشغيل التلقائي / بدء التشغيل** and enable VIP Caller.
4. In system Battery settings, set VIP Caller to **No restrictions**.
5. Lock the app in the recent-apps list (swipe down on its card) so it is not cleared.

The same shortcuts cover Huawei (EMUI), Xiaomi (MIUI), Oppo/Realme, Vivo, and Samsung.

---

## How to Use

1. Install the app and open it once.
2. Allow the required permissions.
3. Go to **قائمة VIP** → **استيراد من جهات الاتصال** to pick contacts from your phonebook.
4. Open the **الإعدادات** tab to choose *always on* or a specific time window.
5. Optionally enable **قفل التطبيق** in the same tab to require a PIN or fingerprint.
6. Use the master switch at the top to turn the system off and on whenever you want.

---

## Required Permissions

| Permission | Purpose |
| --- | --- |
| `READ_PHONE_STATE` | Detect incoming calls |
| `READ_CALL_LOG` | Read the caller number (required on Android 10+) |
| `READ_CONTACTS` | Import contacts and read their custom ringtone |
| `POST_NOTIFICATIONS` | Show the ongoing service notification |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Keep monitoring alive in the background |
| `RECEIVE_BOOT_COMPLETED` | Resume after reboot |
| `WAKE_LOCK` | Keep the device awake while a VIP is ringing |
| `VIBRATE` | Vibration alert |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Request exemption from battery optimization |
| `ACCESS_NOTIFICATION_POLICY` | Silence the system ringer while a VIP ringtone plays |
| `USE_BIOMETRIC` | Fingerprint unlock for the app lock |

The app does not upload contacts, call data, or any personal information. All state lives in local `SharedPreferences`, and the app-lock PIN is stored only as a salted PBKDF2 hash.

---

## Project Structure

```
core/
  Models.kt          VipContact, DeviceContact, Schedule
  VipStore.kt        persistence + ScheduleEvaluator
  ContactsRepo.kt    phonebook reads
  RingtonePlayer.kt  playback, audio focus, vibration, ringer silencing
  PhoneUtils.kt      number normalisation, matching, RTL-safe display
  SearchUtils.kt     Arabic-aware contact search and ranking
  AppLock.kt         PIN hashing, verification, attempt throttling
  BiometricGate.kt   platform BiometricPrompt wrapper
  OemCompat.kt       Honor / Huawei / Xiaomi / Oppo settings shortcuts
ui/
  Components.kt      shared cards, avatars, palette
  screens/           VipList, ContactPicker, Schedule, Lock, About
CallReceiver.kt      PHONE_STATE handling
VipService.kt        foreground monitoring service
BootReceiver.kt      restart after boot / update
MainActivity.kt      navigation + LockGate
```

---

## Build

```bash
./gradlew assembleDebug      # APK at app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # schedule, search, number and app-lock tests
```

---

## Developer

Developed by **Hassan Bazarah**

GitHub: [HBaz92](https://github.com/HBaz92)

---

## Disclaimer

VIP Caller does not replace the system dialer. It plays the contact ringtone itself during an incoming call, and silences the system ringer for the duration when Do Not Disturb access is granted.

Some devices may still restrict background execution due to manufacturer battery policies; see the Device Compatibility section above.

---

## License

This project is open source.
License information will be added later.

---

## Changelog

See [CHANGELOG.md](CHANGELOG.md) for the full release history.
