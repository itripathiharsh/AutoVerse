import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}
val releaseSigningConfigured =
    keystorePropertiesFile.isFile || System.getenv("AUTOVERSE_RELEASE_STORE_FILE") != null

android {
    namespace = "com.autoverse.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.autoverse.launcher"
        minSdk = 23
        targetSdk = 34
        versionCode = 27
        versionName = "0.25.0-theme-engine"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a")
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }



    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (releaseSigningConfigured) {
            create("release") {
                if (keystorePropertiesFile.isFile) {
                    storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                    storePassword = keystoreProperties.getProperty("storePassword")
                    keyAlias = keystoreProperties.getProperty("keyAlias")
                    keyPassword = keystoreProperties.getProperty("keyPassword")
                } else {
                    storeFile = rootProject.file(System.getenv("AUTOVERSE_RELEASE_STORE_FILE"))
                    storePassword = System.getenv("AUTOVERSE_RELEASE_STORE_PASSWORD")
                    keyAlias = System.getenv("AUTOVERSE_RELEASE_KEY_ALIAS")
                    keyPassword = System.getenv("AUTOVERSE_RELEASE_KEY_PASSWORD")
                }
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            signingConfig = if (releaseSigningConfigured) {
                signingConfigs.getByName("release")
            } else {
                logger.warn(
                    "AUTOVERSE: release signing is not configured (keystore.properties " +
                        "or AUTOVERSE_RELEASE_* environment variables are missing); " +
                        "signing release builds with the debug key. Do not distribute.",
                )
                signingConfigs.getByName("debug")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            // Compress native libraries: libvosk.so stores ~17 MB uncompressed
            // (offline voice, Tier 2). Compression trades install-time
            // extraction for roughly half the APK size.
            useLegacyPackaging = true
        }
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // JNA ships desktop/other-platform natives as resources; only the
            // Android ABIs we build are relevant (offline voice, Tier 2).
            excludes += "com/sun/jna/aix-ppc/**"
            excludes += "com/sun/jna/aix-ppc64/**"
            excludes += "com/sun/jna/darwin-x86-64/**"
            excludes += "com/sun/jna/darwin-aarch64/**"
            excludes += "com/sun/jna/win32-x86/**"
            excludes += "com/sun/jna/win32-x86-64/**"
            excludes += "com/sun/jna/win32-aarch64/**"
            excludes += "com/sun/jna/freebsd-x86/**"
            excludes += "com/sun/jna/freebsd-x86-64/**"
            excludes += "com/sun/jna/openbsd-x86/**"
            excludes += "com/sun/jna/openbsd-x86-64/**"
            excludes += "com/sun/jna/linux-ppc/**"
            excludes += "com/sun/jna/linux-ppc64/**"
            excludes += "com/sun/jna/linux-ppc64le/**"
            excludes += "com/sun/jna/linux-mips64el/**"
            excludes += "com/sun/jna/linux-mipsel/**"
            excludes += "com/sun/jna/linux-s390x/**"
            excludes += "com/sun/jna/linux-riscv64/**"
            excludes += "com/sun/jna/linux-loongarch64/**"
            excludes += "com/sun/jna/sunos-x86/**"
            excludes += "com/sun/jna/sunos-x86-64/**"
            excludes += "com/sun/jna/sunos-sparc/**"
            excludes += "com/sun/jna/sunos-sparcv9/**"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.vosk.android) {
        exclude(module = "jna")
    }
    implementation(libs.jna.aar)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
