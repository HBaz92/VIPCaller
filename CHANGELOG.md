# Changelog

All notable changes to VIP Caller are documented here.

---

## [2.0] — 2026-09-12

A major release. VIP Caller now manages an explicit list of important contacts, runs on a schedule you control, and can be locked behind a PIN or your fingerprint.

### Added

**VIP contact list**
- Import contacts directly from the device phonebook, with multi-select
- Search by name or number, with Arabic-aware matching — `احمد` finds `أحمد`, `فاطمه` finds `فاطمة`, and Arabic-Indic digits (`٠١٢٣`) match numbers stored in Latin digits
- Results are ranked: names beginning with your query appear before names that merely contain it
- Enable, disable, or remove each contact individually; contacts already on the list are marked during import
- Everything is stored locally on the device

**Scheduling and control**
- Master switch to turn monitoring on and off at any time
- **Always on** mode, or a **time window** with specific days and hours
- Overnight windows are supported (for example 22:00 → 06:00)
- A **Stop** action inside the ongoing notification

**App lock**
- Optional 4-digit PIN required to open the app, so the VIP list stays private if someone else picks up the phone
- Fingerprint unlock, with the PIN always available as a fallback (Android 9+)
- The PIN itself is never stored — only a salted PBKDF2 hash of it
- Failed attempts are rate limited with an escalating delay, and the delay survives restarting the app
- Optional hiding of app content from the recent-apps preview

**Alert behavior**
- The system ringer is silenced while a VIP ringtone plays, so a VIP call rings once instead of overlapping with the system's own ringtone (requires Do Not Disturb access, which the app offers to grant)
- Vibration alongside the ringtone
- Silent-mode override, routing audio through the alarm stream
- Configurable maximum ringtone duration (10–120 seconds)
- Ringtone priority: the contact's system ringtone, falling back to the device default

**Background reliability**
- Ongoing foreground service keeps call monitoring alive on restrictive devices
- Automatic restart after reboot and after app updates
- Device compatibility section with direct shortcuts to battery-optimization exemption and manufacturer auto-start settings, covering Honor (MagicOS), Huawei (EMUI), Xiaomi, Oppo/Realme, Vivo, and Samsung

**Interface**
- Rebuilt as a three-tab Arabic interface: VIP list, settings, and about
- Edge-to-edge layout with proper handling of the status bar, navigation bar, and keyboard
- Phone numbers are displayed in their correct order within the right-to-left layout

### Changed

- Caller matching now ignores country codes and separators, so `+966512345678` and `0512345678` are recognised as the same contact
- Incoming-call detection handles dual-SIM devices, which broadcast call state once per SIM
- Ringtone playback now requests and releases audio focus, and stops reliably when a call is answered, rejected, or ended
- The app targets Android 16 (API 36) and still supports Android 7.0 (API 24) and newer

### Permissions

New in this release: `POST_NOTIFICATIONS`, `VIBRATE`, `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED`, `FOREGROUND_SERVICE_SPECIAL_USE`, `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, and `USE_BIOMETRIC`.

No contact data, call data, or personal information leaves the device.

---

## [1.0]

Initial release.

- Detects incoming calls and plays the caller's custom contact ringtone
- Stops the ringtone when the call is answered, rejected, or ended
- Material 3 interface with Arabic and English tabs
- Works without root
