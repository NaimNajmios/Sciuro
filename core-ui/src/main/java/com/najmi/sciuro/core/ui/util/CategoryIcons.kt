package com.najmi.sciuro.core.ui.util

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalance
import androidx.compose.material.icons.outlined.CardGiftcard
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalGroceryStore
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector

fun mapCategoryIcon(categoryId: String?): ImageVector? {
    return when (categoryId) {
        "cat_dining", "cat_exp_1" -> Icons.Outlined.Restaurant
        "cat_groceries", "cat_exp_6" -> Icons.Outlined.LocalGroceryStore
        "cat_transport", "cat_exp_2" -> Icons.Outlined.DirectionsCar
        "cat_utilities", "cat_exp_3" -> Icons.Outlined.Home
        "cat_exp_4" -> Icons.Outlined.ShoppingCart
        "cat_exp_5" -> Icons.Outlined.Description
        "cat_exp_7" -> Icons.Outlined.LocalHospital
        "cat_exp_8" -> Icons.Outlined.School
        "cat_exp_9", "cat_inc_6" -> Icons.Outlined.MoreHoriz
        "cat_inc_1" -> Icons.Outlined.AccountBalance
        "cat_inc_2" -> Icons.Outlined.Computer
        "cat_inc_3" -> Icons.Outlined.CardGiftcard
        "cat_inc_4" -> Icons.AutoMirrored.Outlined.TrendingUp
        "cat_inc_5" -> Icons.Outlined.Refresh
        "cat_transfer" -> Icons.Outlined.SwapHoriz
        else -> null
    }
}
