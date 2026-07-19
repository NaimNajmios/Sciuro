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
                api(libs.koin.core)
                api(project(":core-ledger"))
                api(project(":core-audit"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.sqldelight.coroutines)
            }
        }
    }
}

android {
    namespace = "com.sciuro.core.budget"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}
