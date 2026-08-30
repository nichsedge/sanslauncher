# Sans Launcher Developer & Agent Guidelines

## ⚡ Modern Standards & Strict "No Backward Compatibility"

* **Modern Kotlin & Compose Only**: Target latest Compose Material 3 standards, Kotlin coroutines/flows, and modern Android SDK APIs. Do not create deprecated wrappers or legacy shims.
* **Architecture**: Unidirectional Data Flow (UDF) with immutable UI state (`HomeUiState`). Business logic in Repositories (`AppRepository`, `SettingsRepository`, `CalendarRepository`).
* **Testing**: Write unit tests for business logic, search algorithms, time calculations, and repositories with JUnit.
* **Release Pipeline**: Production builds use R8 code shrinking and ProGuard minification via GitHub Actions (`.github/workflows/build-and-release.yml`).

## 🛠️ Build & Verification Commands

```bash
# Run unit tests
./gradlew test

# Compile and package debug APK
./gradlew assembleDebug

# Compile optimized release APK with R8 minification
./gradlew assembleRelease
```
