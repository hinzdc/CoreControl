import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val fallbackGlobalKeystoreFile = file("/data/data/com.termux/files/home/.android/sargaverse-release.properties")
val keystoreProperties = Properties()

if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
} else if (fallbackGlobalKeystoreFile.exists()) {
    keystoreProperties.load(FileInputStream(fallbackGlobalKeystoreFile))
}

// 1. Check environment variables, 2. Check keystore.properties
val releaseStoreFilePath = System.getenv("KEYSTORE_PATH")
    ?: System.getenv("RELEASE_KEYSTORE_PATH")
    ?: keystoreProperties.getProperty("storeFile")

val releaseStorePassword = System.getenv("KEYSTORE_PASSWORD")
    ?: System.getenv("RELEASE_KEYSTORE_PASSWORD")
    ?: keystoreProperties.getProperty("storePassword")

val releaseKeyAlias = System.getenv("KEY_ALIAS")
    ?: System.getenv("RELEASE_KEY_ALIAS")
    ?: keystoreProperties.getProperty("keyAlias")

val releaseKeyPassword = System.getenv("KEY_PASSWORD")
    ?: System.getenv("RELEASE_KEY_PASSWORD")
    ?: keystoreProperties.getProperty("keyPassword")

val isReleaseSigningConfigured = !releaseStoreFilePath.isNullOrBlank() &&
    !releaseStorePassword.isNullOrBlank() &&
    !releaseKeyAlias.isNullOrBlank() &&
    !releaseKeyPassword.isNullOrBlank() &&
    file(releaseStoreFilePath).exists()

if (!isReleaseSigningConfigured) {
    logger.warn("[CoreControl] Release signing credentials not configured or keystore file missing. Building unsigned release APK.")
} else {
    logger.lifecycle("[CoreControl] SARGAVERSE Release signing configured with: $releaseStoreFilePath")
}

android {
    namespace = "com.sargaverse.corecontrol"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sargaverse.corecontrol"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (isReleaseSigningConfigured) {
            create("release") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (isReleaseSigningConfigured) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
            "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    debugImplementation(libs.androidx.ui.tooling)
}

tasks.register("distributeApks") {
    group = "distribution"
    description = "Copies built debug and release APKs into dist/ with version-date formatted names."

    doLast {
        val distDir = rootProject.file("dist")
        if (!distDir.exists()) {
            distDir.mkdirs()
        }

        val dateFormat = SimpleDateFormat("yyyy-MM-dd")
        val dateString = dateFormat.format(Date())
        val versionName = android.defaultConfig.versionName ?: "1.0.0"

        // 1. Process Debug APK
        val debugApk = file("build/outputs/apk/debug/app-debug.apk")
        if (debugApk.exists()) {
            val destDebugName = "CoreControl-v${versionName}-debug-${dateString}.apk"
            val destDebugFile = File(distDir, destDebugName)
            debugApk.copyTo(destDebugFile, overwrite = true)
            logger.lifecycle("[CoreControl] Distribution: Created ${destDebugFile.absolutePath} (${destDebugFile.length() / 1024 / 1024} MB)")
        } else {
            logger.warn("[CoreControl] Debug APK not found at ${debugApk.absolutePath}")
        }

        // 2. Process Release APK (signed 'app-release.apk' or fallback 'app-release-unsigned.apk')
        val signedReleaseApk = file("build/outputs/apk/release/app-release.apk")
        val unsignedReleaseApk = file("build/outputs/apk/release/app-release-unsigned.apk")
        val releaseApk = if (signedReleaseApk.exists()) signedReleaseApk else unsignedReleaseApk

        if (releaseApk.exists()) {
            val destReleaseName = "CoreControl-v${versionName}-release-${dateString}.apk"
            val destReleaseFile = File(distDir, destReleaseName)
            releaseApk.copyTo(destReleaseFile, overwrite = true)
            logger.lifecycle("[CoreControl] Distribution: Created ${destReleaseFile.absolutePath} (${destReleaseFile.length() / 1024 / 1024} MB)")
        } else {
            logger.warn("[CoreControl] Release APK not found in build/outputs/apk/release/")
        }
    }
}
