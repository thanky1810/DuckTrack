plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.kapt")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.ducktrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.ducktrack"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {

    // --- Image loading (Coil) ---
    // Giữ bản 2.6.0, xóa bản 2.5.0 bị trùng
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- Lifecycle & ViewModel ---
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // --- Compose BOM & UI ---
    implementation(platform(libs.androidx.compose.bom))
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation:1.7.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.compose.foundation)

    // Splash screen
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Material Design (Classic XML view support - nếu cần)
    implementation("com.google.android.material:material:1.12.0")

    // --- Room ---
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.foundation.layout)
    kapt(libs.androidx.room.compiler)

    // --- DataStore ---
    // Giữ bản 1.1.1, xóa bản 1.0.0 bị trùng
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // --- WorkManager ---
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // --- Firebase & Auth ---
    // QUAN TRỌNG: Chỉ giữ lại 1 bản BOM mới nhất (34.6.0)
    implementation(platform("com.google.firebase:firebase-bom:34.6.0"))

    // Khi dùng BOM, KHÔNG CẦN điền version cho các thư viện con dưới đây:
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")

    // SỬA LỖI Ở ĐÂY: Dùng string trực tiếp, không dùng libs đang lỗi version
    implementation("com.google.firebase:firebase-storage")

    // Google Sign In & Credentials
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // --- Lottie Animation ---
    implementation(libs.lottie.compose)

    implementation("androidx.glance:glance-appwidget:1.1.0")
    implementation("androidx.glance:glance-material3:1.1.0")

    implementation("com.google.ai.client.generativeai:generativeai:0.9.0")    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}