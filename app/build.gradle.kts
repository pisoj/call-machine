plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.zutiradio.callmachine"
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        applicationId = "com.zutiradio.callmachine"
        minSdk = 29
        targetSdk = 37
        versionCode = 32
        versionName = "0.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    packaging {
        resources {
            // Prevent the Kotlin standard library from being included in the builds,
            // this project does not use Kotlin
            excludes += "kotlin/**"
        }
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {}
