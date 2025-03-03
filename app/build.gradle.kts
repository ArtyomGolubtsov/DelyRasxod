plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.gms)
}

android {
    namespace = "com.example.bankapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.bankapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "23.21.11"

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Firebase
    implementation(libs.androidx.biometric)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)

    implementation ("com.github.bumptech.glide:glide:4.12.0")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.biometric.ktx)
    implementation(libs.androidx.ui.geometry.android)
    implementation(libs.androidx.lifecycle.viewmodel.android)
    implementation(libs.androidx.emoji)
    implementation(libs.androidx.emoji.bundled)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}