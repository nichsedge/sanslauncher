# Sans Launcher 🧠⚡

> A distraction-free, high-performance Android launcher engineered to scaffold executive function, combat time blindness, and reduce compulsive phone use for ADHD minds.

[![Release](https://img.shields.io/github/v/release/nichsedge/sanslauncher?style=flat-square)](https://github.com/nichsedge/sanslauncher/releases)
[![License](https://img.shields.io/badge/License-MIT-blue.svg?style=flat-square)](LICENSE)
[![Android](https://img.shields.io/badge/Android-13%2B-green.svg?style=flat-square)](https://developer.android.com)

---

## ✨ Features

### 🧠 ADHD & Time Scaffolding
- **"Next Up Only" Calendar Anchor**: Minimalist single-event preview under the clock (e.g. `14:00 • Team Sync in 20m`) to prevent cognitive overload while stopping time blindness.
- **Ambient Time & Date Anchor**: Clean digital clock and date grounded at the top.
- **Tactile Haptic Feedback**: Crisp, subtle vibrational feedback on clicks and long presses.

### ⚡ Navigation & Gestures
- **Circle to Search & Google Lens Integration**: Directly triggers Google's contextual search (`IVoiceInteractionManagerService`) from the search bar shortcut, Quick Settings tile, or system digital assistant invocation (`AssistActivity`), bypassing HyperOS third-party launcher restrictions.
- **Swipe Down for Notifications**: Pull down anywhere on the home screen to expand Android's notification shade.
- **Auto-Open Keyboard Option**: Automatically focus the search bar and pop up the keyboard when returning to the home screen.
- **Sans Finance Quick Expense Shortcut**: Integrated money icon in the search bar for immediate one-tap expense entry and search-query note passing to Sans Finance.
- **Long-Press Header for Settings**: Press and hold the Clock header to access launcher preferences.

### 🔍 Smart Intent-Driven Search
- **Instant Multi-Tier Search**: Exact match, prefix, acronym discovery (e.g. `yt` -> YouTube, `gc` -> Google Chrome), and fuzzy matching.
- **Immediate Auto-Launch**: As soon as a query matches a single app, it launches immediately without waiting for full typing.
- **Custom App Aliases**: Rename apps to conscious mnemonics (e.g. rename social media to *"Mindless Feed"*).
- **Multi-Account & Work Profile Badging**: Unobtrusive work profile badging (briefcase indicator) and dual/cloned app support, allowing work and personal apps to be searched, pinned to favorites, or hidden independently.

### 🎨 Themes & Privacy
- **Pure AMOLED True Black, Clean Light, and System Default**: Dark theme uses 100% pure black (`#000000`) for zero OLED power leakage.
- **Font Styling**: Modern Sans-Serif vs Clean Monospace.
- **100% Offline & Private**: Zero telemetry, zero analytics, zero ads.
- **AOT Baseline Profiles**: Ahead-of-Time ART compilation profile included for instantaneous cold startup.

---

## 📥 Installation

Download the latest `SansLauncher.apk` directly from [GitHub Releases](https://github.com/nichsedge/sanslauncher/releases) and install it on your device (Android 13+ / API 33+).

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
