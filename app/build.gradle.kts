plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.navigation.safe.args)
    id("kotlin-parcelize")
}

android {
    namespace = "com.example.purrytify"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.purrytify"
        minSdk = 29
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Use Compose BOM to manage versions automatically
//    implementation(platform("androidx.compose:compose-bom:2024.03.00"))
    val compose_version = "1.6.3"
    val material3_version = "1.2.0"

    // Compose dependencies (No need to specify versions)
    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.foundation:foundation:$compose_version")
    implementation("androidx.compose.runtime:runtime:$compose_version")
    implementation("androidx.compose.material3:material3:$material3_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation ("com.squareup.picasso:picasso:2.71828")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    // Activity & Coil
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("io.coil-kt:coil-compose:2.5.0")

    // AndroidX Core & Material
    implementation("androidx.core:core-ktx:1.13.1")
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // ConstraintLayout & Lifecycle Components
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Navigation Components
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    //Recycler View
    implementation ("com.github.bumptech.glide:glide:4.15.1")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")

    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Coroutines (simplifies the process of making network requests and handling Responses
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
}
