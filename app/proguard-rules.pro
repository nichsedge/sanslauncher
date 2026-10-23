# Proguard / R8 configuration for Sans Launcher

# Keep Kotlin metadata, annotations, and signatures
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn java.lang.invoke.**

# Keep Compose Stability Annotations & Annotated Classes
-keep @androidx.compose.runtime.Immutable class * { *; }
-keep @androidx.compose.runtime.Stable class * { *; }

# Keep DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# Keep App Models and UI State Data Classes
-keep class com.sans.launcher.AppModel { *; }
-keep class com.sans.launcher.TimeState { *; }
-keep class com.sans.launcher.HomeUiState { *; }
-keep class com.sans.launcher.CalendarEventModel { *; }
-keep class com.sans.launcher.CalendarInstanceData { *; }
-keep class com.sans.launcher.PartitionedApps { *; }
-keep class com.sans.launcher.LauncherViewModel$SearchResult { *; }

# Keep LauncherIcons static singleton and vector holders
-keep class com.sans.launcher.LauncherIcons { *; }
