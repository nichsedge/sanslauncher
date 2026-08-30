# Sans Launcher Developer & Agent Guidelines

## ⚡ Modern Standards & Strict "No Backward Compatibility"

* **Modern Kotlin & Compose Only**: Target latest Compose Material 3 standards, Kotlin coroutines/flows, and modern Android SDK APIs. Do not create deprecated wrappers or legacy shims.
* **Architecture**: Unidirectional Data Flow (UDF) with immutable UI state (`HomeUiState`). Business logic in Repositories (`AppRepository`, `SettingsRepository`, `CalendarRepository`).
* **Testing**: Write unit tests for business logic, search algorithms, time calculations, and repositories with JUnit.
* **Release Pipeline**: Production builds use R8 code shrinking and ProGuard minification via GitHub Actions (`.github/workflows/build-and-release.yml`).

## 🎯 Android Launcher & Search Invariants

* **Instant Auto-Launch**: When a search query reduces the visible matching apps to exactly 1 candidate, immediately launch the app without requiring full text typing or IME submit.
* **Custom Alias Exclusivity**: When an app is assigned a custom alias/label, search matching must be strictly against the custom alias. Never fall back to matching the original system label.
* **Strict Search Matching**: Prohibit loose scattered subsequence matching across letters. Use multi-tier scoring: exact match -> prefix -> word prefix -> acronym -> contiguous substring.
* **Edge-to-Edge Bar Tinting**: Ensure status bar and navigation bar icon contrast is dynamically driven by the app's active color scheme (not just system dark mode) to avoid invisible status bars on custom AMOLED themes.
* **Root Back Gesture Handling**: Absorb back gestures on the clean home view in `BackHandler` to prevent Android's predictive back window exit animations.

## 🛠️ Build & Verification Commands

```bash
# Run unit tests
./gradlew test

# Compile and package debug APK
./gradlew assembleDebug

# Compile optimized release APK with R8 minification
./gradlew assembleRelease
```
