plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
}

kotlin {
    jvm()
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
                implementation(project(":core-ledger"))
                implementation(project(":core-audit"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.kotlinx.coroutines.core)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.sqldelight.jdbc.driver)
            }
        }
    }
}

android {
    namespace = "com.sciuro.core.transfer"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}
