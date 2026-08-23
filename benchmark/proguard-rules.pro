# The benchmark target is minified to match the optimized app under test.
-dontwarn androidx.arch.core.**
-dontwarn androidx.profileinstaller.ProfileInstallReceiver
-dontwarn androidx.startup.Initializer
-dontwarn com.google.errorprone.annotations.**
-dontwarn android.hardware.fingerprint.**

# Instrumentation runners and benchmark rules are created from manifest/JUnit
# metadata, so their reflective entry points are not visible to R8's reachability
# analysis. Keep the runner and tracing bridge intact in the optimized test APK.
-keep class androidx.test.runner.** { *; }
-keep class androidx.benchmark.** { *; }
-keep class androidx.tracing.** { *; }
-keep class com.whip.benchmark.** { *; }
