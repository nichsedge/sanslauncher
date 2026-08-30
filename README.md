# Sans Launcher 🧠⚡

> A distraction-free, high-performance Android launcher engineered to scaffold executive function, combat time blindness, and reduce compulsive phone use for ADHD minds.

[![Release](https://img.shields.io/github/v/release/nichsedge/sanslauncher?style=flat-square)](https://github.com/nichsedge/sanslauncher/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-green.svg?style=flat-square)](https://developer.android.com)

---

## ✨ Features

### 🧠 ADHD & Time Scaffolding
- **Instant Brain Dump (Swipe Left)**: Swipe left anywhere on the home screen to open a raw text scratchpad with instant todo-checkbox support. Auto-saves locally on every stroke.
- **"Next Up Only" Calendar Anchor**: Minimalist single-event preview under the clock (e.g. `14:00 • Team Sync in 20m`) to prevent cognitive overload while stopping time blindness.
- **Ambient Time & Date Anchor**: Clean digital clock and date grounded at the top.
- **Visual Day Progress Meter**: Real-time progress bar showing `% of today elapsed`.
- **Tactile Haptic Feedback**: Crisp, subtle vibrational feedback on clicks and long presses.

### ⚡ Navigation & Gestures
- **Swipe Down for Notifications**: Pull down anywhere on the home screen to expand Android's notification shade.
- **Auto-Open Keyboard Option**: Automatically focus the search bar and pop up the keyboard when returning to the home screen.
- **Long-Press Header for Settings**: Press and hold the Clock / Day progress header to access launcher preferences.

### 🔍 Smart Intent-Driven Search
- **Instant Multi-Tier Search**: Exact match, prefix, acronym discovery (e.g. `yt` -> YouTube, `gc` -> Google Chrome), and fuzzy matching.
- **Immediate Auto-Launch**: As soon as a query matches a single app, it launches immediately without waiting for full typing.
- **Custom App Aliases**: Rename apps to conscious mnemonics (e.g. rename social media to *"Mindless Feed"*).

### 🎨 Themes & Privacy
- **AMOLED True Black, Minimal Dark, Clean Light, and System Default**.
- **Font Styling**: Modern Sans-Serif vs Clean Monospace.
- **100% Offline & Private**: Zero telemetry, zero analytics, zero ads.

---

## 🚀 Building & Testing

```bash
# Run unit tests
./gradlew test

# Build debug APK
./gradlew assembleDebug

# Build optimized release APK
./gradlew assembleRelease
```
