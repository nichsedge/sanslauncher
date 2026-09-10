# Proguard / R8 configuration for Sans Launcher

# Keep Kotlin metadata and coroutines
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-dontwarn java.lang.invoke.**

# Keep DataStore Preferences
-keepclassmembers class * extends androidx.datastore.preferences.core.Preferences { *; }

# Keep App Models and Data Classes
-keep class com.sans.launcher.AppModel { *; }
-keep class com.sans.launcher.TimeState { *; }
-keep class com.sans.launcher.HomeUiState { *; }
-keep class com.sans.launcher.CalendarEventModel { *; }
-keep class com.sans.launcher.PartitionedApps { *; }
