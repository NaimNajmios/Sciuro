pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Sciuro"
include(":app")
include(":core-ingestion")
include(":core-parsing")
include(":core-classifier")
include(":core-ledger")
include(":core-audit")
include(":core-obligations")
include(":core-transfer")
include(":core-debt")
include(":core-investment")
include(":core-budget")
include(":core-ui")
include(":feature-dashboard")
include(":feature-kanban")
include(":feature-wallet")
include(":feature-budgets")
include(":feature-debt")
include(":feature-settings")