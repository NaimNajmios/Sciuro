$files = @(
    "core-ui\src\main\java\com\najmi\sciuro\core\ui\components\TransactionDetailSheet.kt",
    "core-ui\src\main\java\com\najmi\sciuro\core\ui\components\TransactionCard.kt",
    "core-ui\src\main\java\com\najmi\sciuro\core\ui\components\AdjustmentCard.kt",
    "core-ui\src\main\java\com\najmi\sciuro\core\ui\components\AdjustmentBottomSheet.kt",
    "feature-wallet\src\androidMain\kotlin\com\sciuro\feature\wallet\ui\WalletScreen.kt",
    "feature-kanban\src\androidMain\kotlin\com\sciuro\feature\kanban\ui\KanbanScreen.kt",
    "feature-dashboard\src\androidMain\kotlin\com\sciuro\feature\dashboard\ui\DashboardScreen.kt",
    "feature-budgets\src\androidMain\kotlin\com\sciuro\feature\budgets\ui\BudgetsScreen.kt",
    "feature-settings\src\androidMain\kotlin\com\sciuro\feature\settings\ui\DeveloperSettingsScreen.kt"
)

foreach ($f in $files) {
    if (Test-Path $f) {
        $content = Get-Content $f -Raw
        
        # Determine which imports to add
        $needsIncome = $content -match "Color\(0xFF4CAF50\)"
        $needsDanger = $content -match "Color\(0xFFE53935\)" -or $content -match "Color\(0xFFFF5252\)" -or $content -match "Color\.Red"
        $needsWarning = $content -match "Color\(0xFFE8B84B\)" -or $content -match "Color\(0xFFFFB74D\)"
        
        # Replace colors
        $content = $content -replace 'Color\(0xFF4CAF50\)', 'com.najmi.sciuro.core.ui.theme.SignalIncome'
        $content = $content -replace 'Color\(0xFFE53935\)', 'com.najmi.sciuro.core.ui.theme.SignalDanger'
        $content = $content -replace 'Color\(0xFFFF5252\)', 'com.najmi.sciuro.core.ui.theme.SignalDanger'
        $content = $content -replace 'Color\.Red', 'com.najmi.sciuro.core.ui.theme.SignalDanger'
        $content = $content -replace 'Color\(0xFFE8B84B\)', 'com.najmi.sciuro.core.ui.theme.SignalWarning'
        $content = $content -replace 'Color\(0xFFFFB74D\)', 'com.najmi.sciuro.core.ui.theme.SignalWarning'

        # Remove local val definitions that are now redundant
        $content = $content -replace 'private val InflowGreen = com\.najmi\.sciuro\.core\.ui\.theme\.SignalIncome\r?\n?', ''
        $content = $content -replace 'private val OutflowRed = com\.najmi\.sciuro\.core\.ui\.theme\.SignalDanger\r?\n?', ''
        $content = $content -replace 'private val AdjustmentAmber = com\.najmi\.sciuro\.core\.ui\.theme\.SignalWarning\r?\n?', ''
        
        # Also fix references to the removed val definitions
        $content = $content -replace 'InflowGreen', 'com.najmi.sciuro.core.ui.theme.SignalIncome'
        $content = $content -replace 'OutflowRed', 'com.najmi.sciuro.core.ui.theme.SignalDanger'
        $content = $content -replace 'AdjustmentAmber', 'com.najmi.sciuro.core.ui.theme.SignalWarning'
        
        Set-Content -Path $f -Value $content
        Write-Host "Updated $f"
    }
}
