plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
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
                api(libs.sqldelight.coroutines)
                api(project(":core-audit"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(libs.sqldelight.android.driver)
                implementation(libs.sqlcipher)
                implementation(libs.androidx.security.crypto)
                implementation(libs.androidx.sqlite)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(libs.sqldelight.jdbc.driver)
            }
        }
    }
}

android {
    namespace = "com.sciuro.core.ledger"
    compileSdk = 34
    defaultConfig {
        minSdk = 24
    }
}

sqldelight {
    databases {
        create("SciuroDatabase") {
            packageName.set("com.sciuro.core.ledger.db")
        }
    }
}
