plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.kanayama.sudokuassistant"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kanayama.sudokuassistant"
        minSdk = 24
        targetSdk = 35
        versionCode = 12
        versionName = "1.7.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.13")
}
