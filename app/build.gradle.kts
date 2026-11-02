import java.util.Properties

plugins {
    id("com.android.application")
}

val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.coldcat.browser"
    compileSdk = 37
    // GeckoView 159 is built against API 37.2 (Android SDK minor versions).
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.coldcat.browser"
        minSdk = 26
        targetSdk = 37
        versionCode = 5
        versionName = "0.1.4"
    }

    androidResources {
        // Ship only the locales the app is translated into.
        localeFilters += listOf("en", "ru")
    }

    splits {
        // GeckoView ships ~170 MB of native code per ABI. Split the APK so a
        // phone downloads only the architecture it runs.
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    signingConfigs {
        if (keystoreProperties.getProperty("storeFile") != null) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        jniLibs {
            // GeckoView native code dominates the APK. Compress it to keep the
            // GitHub release download near ~100 MB instead of ~200 MB.
            useLegacyPackaging = true
        }
    }
}

// GeckoView 159 is compiled with Kotlin 2.4, while AGP's built-in Kotlin
// compiler is 2.2.x and rejects newer metadata versions. The metadata check
// only guards against forward-incompatible declarations; the stdlib 2.4.20 is
// shipped in the APK, so skipping the check is safe here.
kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xskip-metadata-version-check")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.12.0")

    implementation("org.mozilla.geckoview:geckoview:159.0.20261022150525")

    testImplementation("junit:junit:4.13.2")
}
