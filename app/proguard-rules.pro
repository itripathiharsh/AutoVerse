# AutoVerse R8 keep rules.
#
# Minification and resource shrinking are enabled for release builds. The app
# code itself needs no keep rules: every reflection- or JNI-heavy dependency
# ships consumer rules that R8 merges automatically (MapLibre Native, OkHttp,
# Compose/AndroidX). Add rules only when an R8 run reports a missing class or
# a device smoke test finds a break; avoid broad keeps that disable shrinking.

# MapLibre Native: the AAR's consumer rules cover the JNI bridge; these only
# silence OkHttp's optional TLS-provider probes.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Shizuku user service is instantiated reflectively by the Shizuku server;
# the AIDL stub must survive too.
-keep class com.autoverse.launcher.core.mapdisplay.** { *; }
