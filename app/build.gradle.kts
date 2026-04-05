import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.way"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.way"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) {
                f.inputStream().use { load(it) }
            }
        }

        fun readSecret(key: String): String {
            val fromLocal = localProps.getProperty(key)?.trim().orEmpty()
            val fromProject = project.findProperty(key)?.toString()?.trim().orEmpty()
            val fromEnv = System.getenv(key)?.trim().orEmpty()
            return fromLocal.ifEmpty { fromProject.ifEmpty { fromEnv } }
        }

        val placesApiKey = readSecret("GOOGLE_PLACES_API_KEY")
        manifestPlaceholders["GOOGLE_PLACES_API_KEY"] = placesApiKey
        buildConfigField("String", "PLACES_API_KEY", "\"$placesApiKey\"")

        val geoapifyKey = readSecret("GEOAPIFY_API_KEY")
        buildConfigField("String", "GEOAPIFY_API_KEY", "\"$geoapifyKey\"")
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

// Hilt KSP configuration
ksp {
    arg("dagger.hilt.android.internal.disableAndroidSuperclassValidation", "true")
    arg("dagger.fastInit", "ENABLED")
}

dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.lifecycle.runtime)

    // Fragment
    implementation(libs.androidx.fragment)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:34.9.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.3.0")

    // Location Services (GPS)
    implementation(libs.play.services.location)

    // OkHttp (for Geoapify REST API calls)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Coroutines Play Services (for .await() on Firebase Tasks)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.10.2")

    // Gson (local walk session caching)
    implementation("com.google.code.gson:gson:2.11.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}