# Mere Metrics: Weight Tracker (Original)

This is the original version of *Mere Metrics: Weight Tracker* before I made the capstone improvements. I built it in a mobile architecture and programming course as a Java Android app for tracking daily weight entries against a goal. The enhanced version is at [mere-metrics-enhanced](https://github.com/kcsnu/mere-metrics-enhanced), and the full portfolio is at [kcsnu.github.io](https://kcsnu.github.io/).

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
3. Let Gradle finish syncing. The project uses Gradle 9.1.0, which the wrapper downloads automatically.
4. Select an emulator or connected device from the device picker.
5. Press Run.

## Features

- Account creation and login with PBKDF2-hashed passwords
- Goal weight setting
- Daily weight entry tracking with create, read, update, and delete
- Weight history displayed in a manually generated table
- Optional SMS notification when the goal is reached (requires SMS runtime permission)

## Notes

- SMS permission is requested at runtime. If you allow it, notifications work. If not, everything else still works fine.
- Data is stored locally in SQLite through a `SQLiteOpenHelper` implementation. There is no network or backend component.
- I kept the original package name so it matches the enhanced version.

## Project structure

Everything is in a single flat package, including the main activity, fragments, utilities, and the SQLite helper. In the enhanced version, I reorganized this into separate layers.

## Related

- Enhanced version: [mere-metrics-enhanced](https://github.com/kcsnu/mere-metrics-enhanced)
- Portfolio site: [kcsnu.github.io](https://kcsnu.github.io/)
