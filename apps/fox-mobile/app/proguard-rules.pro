# Proguard / R8 configuration for Fox Mobile

# Suppress harmless warnings from OkHttp3 and Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# OkHttp public suffix list (used by OkHttp cookie handling)
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Suppress harmless warnings for org.json (Android provides org.json in system framework)
-dontwarn org.json.**

# Preserve Fox Mobile application classes and members
-keep class com.fox.mobile.** { *; }
-keepclassmembers class com.fox.mobile.** { *; }

# Compose rules
-keep class androidx.compose.runtime.** { *; }
