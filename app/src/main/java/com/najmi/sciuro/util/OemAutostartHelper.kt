package com.najmi.sciuro.util

import android.content.ComponentName
import android.content.Intent
import android.os.Build

object OemAutostartHelper {
    fun getAutostartIntent(): Intent? {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> Intent().apply {
                component = ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            }
            "oppo", "realme" -> Intent().apply {
                component = ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.floatwindow.FloatWindowListActivity"
                )
            }
            "vivo" -> Intent().apply {
                component = ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            }
            "huawei", "honor" -> Intent().apply {
                component = ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                )
            }
            else -> null
        }
    }

    fun getGuideSteps(): List<String> {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi" -> listOf(
                "Open Settings > Apps > Manage apps",
                "Find Sciuro",
                "Enable Autostart",
                "Go to Battery > Sciuro > No restrictions"
            )
            "oppo", "realme" -> listOf(
                "Open Settings > Apps > App Management",
                "Find Sciuro",
                "Enable Allow Auto Startup",
                "Go to Battery > Power Saver > Sciuro > No restrictions"
            )
            "vivo" -> listOf(
                "Open Settings > More settings > Applications",
                "Find Sciuro",
                "Enable Autostart",
                "Go to Settings > Battery > High background power consumption",
                "Set Sciuro to 'Allow'"
            )
            "huawei", "honor" -> listOf(
                "Open Settings > Apps > Apps",
                "Find Sciuro",
                "Tap Launch > Manage manually",
                "Enable Auto-launch, Secondary launch, and Run in background"
            )
            else -> listOf(
                "Open your device Settings",
                "Find Sciuro in the Apps list",
                "Ensure background activity is allowed",
                "Disable battery optimization for Sciuro"
            )
        }
    }

    fun isKnownAggressiveOem(): Boolean {
        return when (Build.MANUFACTURER.lowercase()) {
            "xiaomi", "oppo", "realme", "vivo", "huawei", "honor" -> true
            else -> false
        }
    }
}
