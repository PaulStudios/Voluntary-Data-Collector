plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    kotlin("android") version "1.9.0" // Replace with your Kotlin version if necessary
    kotlin("plugin.serialization") version "2.0.0"
}
buildscript {
    repositories {
        google()
        mavenCentral()
    }
}
android {
    namespace = "org.paulstudios.datasurvey"
    compileSdk = 34

    defaultConfig {
        applicationId = "org.paulstudios.datasurvey"
        minSdk = 29
        targetSdk = 34
        versionCode = 13
        versionName = "0.7.6"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        viewBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom.v20230100))

    // Core Compose dependencies
    implementation(libs.ui)
    implementation(libs.ui.tooling.preview)
    implementation(libs.androidx.compose.material3.material3)
    implementation(libs.androidx.room.runtime.android)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
    implementation(libs.androidx.ui.android)
    implementation(libs.androidx.runtime.livedata)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)
    testImplementation(libs.junit.junit)
    androidTestImplementation(libs.junit.junit)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    // Accompanist navigation animation
    implementation(libs.accompanist.navigation.animation.v0311alpha)

    // Other dependencies
    implementation(libs.androidx.core.ktx.v1101)
    implementation(libs.androidx.lifecycle.runtime.ktx.v261)
    implementation(libs.androidx.activity.compose.v172)
    implementation(libs.androidx.appcompat.v161)
    implementation(libs.material.v190)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.androidx.navigation.fragment.ktx.v253)
    implementation(libs.androidx.navigation.ui.ktx.v253)
    implementation(libs.androidx.navigation.dynamic.features.fragment.v253)
    androidTestImplementation(libs.androidx.navigation.testing.v253)
    implementation(libs.androidx.navigation.compose.v253)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.androidx.work.runtime.ktx.v281)
    implementation(libs.play.services.location.v2101)
    implementation(libs.kotlinx.coroutines.android.v172)
    implementation(libs.play.services.auth)


    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.play.services.location.v1800)

    implementation(libs.androidx.lifecycle.runtime.ktx.v231)
    implementation(libs.androidx.activity.compose.v131)

    implementation(libs.annotations.v2300)

    implementation(libs.kotlinx.serialization.json)
}

configurations {
    all {
        exclude(group = "com.intellij", module = "annotations")
    }
}