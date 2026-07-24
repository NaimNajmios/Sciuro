plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
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
                api(project(":core-ledger"))
                api(project(":core-audit"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.sqldelight.coroutines)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.sqldelight.jdbc.driver)
                implementation(libs.sqlite.jdbc)
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
