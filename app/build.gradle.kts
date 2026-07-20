plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.najmi.sciuro"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.najmi.sciuro"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
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
    implementation(project(":core-ui"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.lottie.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.androidx.security.crypto)
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    
    // Feature Modules
    implementation(project(":feature-dashboard"))
    implementation(project(":feature-wallet"))
    implementation(project(":feature-kanban"))
    implementation(project(":feature-budgets"))
    
    // Core Modules
    implementation(project(":core-ingestion"))
    implementation(project(":core-parsing"))
    implementation(project(":core-llm"))
    implementation(project(":core-classifier"))
    implementation(project(":core-ledger"))
    implementation(project(":core-audit"))
    implementation(project(":core-budget"))
    implementation(project(":core-debt"))
    implementation(project(":core-investment"))
    implementation(project(":core-obligations"))
    implementation(project(":core-transfer"))
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
