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
                implementation(libs.koin.core)
                implementation(project(":core-ingestion"))
                implementation(project(":core-parsing"))
                implementation(project(":core-ledger"))
                implementation(project(":core-transfer"))
                implementation(project(":core-audit"))
                implementation(project(":core-obligations"))
                implementation(project(":core-budget"))
                implementation(project(":core-debt"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
    }
}

android {
    namespace = "com.sciuro.core.classifier"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}
