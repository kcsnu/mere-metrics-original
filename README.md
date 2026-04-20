# Mere Metrics: Weight Tracker (Original)

This is the original version of Mere Metrics: Weight Tracker before the capstone enhancements. I built it in a mobile architecture and programming course as a Java Android app for tracking daily weight entries against a goal. The enhanced version is at [mere-metrics-enhanced](https://github.com/kcsnu/mere-metrics-enhanced), and the full portfolio is at [kcsnu.github.io](https://kcsnu.github.io/).

## Requirements

- A recent version of Android Studio that supports Android Gradle Plugin 9.0 and compileSdk 36
- Android SDK 36 installed through the Android Studio SDK Manager
- Java 11-compatible JDK (bundled with Android Studio)
- An emulator running Android 14 (API 34) or higher, or a physical device on Android 14 or higher

## How to load and run

1. Clone the repository:

```bash
   git clone https://github.com/kcsnu/mere-metrics-original.git
```

2. Open the project folder in Android Studio.
3. Let Gradle sync finish. The project uses Gradle 9.1.0, which the wrapper downloads automatically.
4. Select an emulator or connected device from the device picker.
5. Press Run.

## Features

- Account creation and login with PBKDF2-hashed passwords
- Goal weight setting
- Daily weight entry tracking with create, read, update, and delete
- Weight history displayed in a manually generated table
- Optional SMS notification when the goal is reached (requires SMS runtime permission)

## Notes

- SMS permission is requested at runtime. Granting it enables the notification feature. Denying it leaves the rest of the app fully functional.
- Data is stored locally in SQLite through a `SQLiteOpenHelper` implementation. There is no network or backend component.
- I kept the original package name so the enhanced version stays consistent with it.

## Project structure

This version uses a single flat package containing the main activity, fragments, utility classes, and the SQLite helper. The enhanced version reorganizes this code into layered packages.

## Related

- Enhanced version: [mere-metrics-enhanced](https://github.com/kcsnu/mere-metrics-enhanced)
- Portfolio site: [kcsnu.github.io](https://kcsnu.github.io/)
