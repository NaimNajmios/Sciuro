package com.najmi.sciuro.navigation

sealed class SciuroRoute(val route: String) {
    data object Onboarding : SciuroRoute("onboarding")
    data object Dashboard : SciuroRoute("dashboard")
    data object Wallet : SciuroRoute("wallet")
    data object Kanban : SciuroRoute("kanban")
    data object Budgets : SciuroRoute("budgets")
    data object Settings : SciuroRoute("settings")
    data object DebtOverview : SciuroRoute("debt_overview")
    data object CategoryDrilldown : SciuroRoute("category_drilldown")
    data object CategorySettings : SciuroRoute("category_settings")
    data object LinkedAccounts : SciuroRoute("linked_accounts")

    data object AccountDetail : SciuroRoute("account_detail/{accountId}") {
        fun createRoute(accountId: String) = "account_detail/$accountId"
    }

    data object DeveloperSettings : SciuroRoute("developer_settings?initialTab={initialTab}") {
        fun createRoute(initialTab: Int) = "developer_settings?initialTab=$initialTab"
    }

    companion object {
        fun parentTabRoute(route: String): String? = when (route) {
            Dashboard.route, Wallet.route, Kanban.route, Budgets.route, Settings.route -> route
            AccountDetail.route -> Wallet.route
            CategoryDrilldown.route -> Budgets.route
            CategorySettings.route, LinkedAccounts.route, DeveloperSettings.route -> Settings.route
            else -> null
        }
    }
}
