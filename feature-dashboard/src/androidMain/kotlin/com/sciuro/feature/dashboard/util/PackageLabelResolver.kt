package com.sciuro.feature.dashboard.util

import android.content.Context

class PackageLabelResolver(private val context: Context) {

    fun label(sourceType: String, packageOrAddress: String): String {
        if (sourceType == "NOTIFICATION") {
            return try {
                val pm = context.packageManager
                pm.getApplicationLabel(pm.getApplicationInfo(packageOrAddress, 0)).toString()
            } catch (e: Exception) {
                packageOrAddress
            }
        }
        return packageOrAddress
    }
}
