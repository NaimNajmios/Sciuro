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
                implementation(project(":core-audit"))
                implementation(project(":core-obligations"))
                implementation(project(":core-debt"))
                implementation(project(":core-parsing"))
            }
        }
        val androidMain by getting {
            dependencies {
    implementation(project(":core-ui"))
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.lifecycle.runtime.ktx)
                implementation(libs.androidx.activity.compose)
                implementation(project.dependencies.platform(libs.androidx.compose.bom))
                implementation(libs.androidx.ui)
                implementation(libs.androidx.ui.graphics)
                implementation(libs.androidx.ui.tooling.preview)
                implementation(libs.androidx.material3)
                implementation(libs.koin.androidx.compose)
            }
        }
    }
}

android {
    namespace = "com.sciuro.feature.kanban"
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

