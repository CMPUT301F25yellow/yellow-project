plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.yellow"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.yellow"
        minSdk = 24
        targetSdk = 34
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

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    // ---------- Firebase (using BoM) ----------
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-storage")

    // ---------- Google Play Services ----------
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-auth:21.3.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ---------- ML Kit ----------
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // ---------- Glide ----------
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ---------- QR / ZXing ----------
    implementation("com.google.zxing:core:3.5.3")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")

    // ---------- AndroidX / Material via version catalog ----------
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.play.services.location)

    implementation("androidx.activity:activity-ktx:1.9.1")

    // ---------- CameraX (explicit versions) ----------
    val cameraxVersion = "1.3.4"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")
    implementation("androidx.camera:camera-extensions:$cameraxVersion")

    // ---------- Date filtering (Material) ----------
    implementation("com.google.android.material:material:1.11.0")

    // ============================================================
    //                       TESTING
    // ============================================================

    // ---------- Unit tests ----------
    testImplementation(libs.junit)
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.annotation:annotation:1.8.0")
    testImplementation("org.mockito:mockito-core:5.11.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("androidx.test.ext:junit:1.1.5")

    // ---------- Android instrumented tests (Espresso UI + Fragment tests) ----------
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")

    // Fragment testing (FragmentScenario)
    debugImplementation("androidx.fragment:fragment-testing:1.6.2")
}