// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.detekt) apply false
}

allprojects {
    val proj = this
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        buildUponDefaultConfig = true
        allRules = false
        config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    }

    plugins.withId("org.jetbrains.kotlin.multiplatform") {
        configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            source = proj.files(
                "${proj.projectDir}/src/commonMain/kotlin",
                "${proj.projectDir}/src/androidMain/kotlin",
                "${proj.projectDir}/src/jvmMain/kotlin"
            )
        }
    }
}