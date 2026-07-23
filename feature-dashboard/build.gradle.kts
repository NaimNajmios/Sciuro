plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    
    sourceSets {
        val commonMain by getting {
            dependencies {
    implementation(project(":core-ui"))
                implementation(libs.koin.core)
                implementation(project(":core-ledger"))
                implementation(project(":core-budget"))
                implementation(project(":core-debt"))
                implementation(project(":core-obligations"))
                implementation(project(":core-investment"))
                implementation(project(":core-ingestion"))
                implementation(project(":core-transfer"))
                implementation(project(":core-parsing"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(project(":core-ui"))
                implementation(project(":core-debt"))
                implementation(project(":core-obligations"))
                implementation(project(":core-investment"))
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.graphics)
                implementation(libs.androidx.ui.tooling.preview)
                implementation(libs.androidx.material3)
                implementation(libs.androidx.material.icons.extended)
                implementation(libs.koin.androidx.compose)
            }
        }
    }
}

android {
    namespace = "com.sciuro.feature.dashboard"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
}

