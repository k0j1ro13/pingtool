# Koin
-keep class org.koin.** { *; }
-keepnames class org.koin.** { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.android.** { *; }

# Compose
-keep class androidx.compose.** { *; }

# ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Data classes del dominio
-keep class com.pingmonitor.domain.** { *; }
-keep class com.pingmonitor.data.** { *; }

# Evitar que se eliminen clases referenciadas por reflexión
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
