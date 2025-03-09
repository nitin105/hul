package com.hul.utils

import android.content.Context;
import android.provider.Settings;


object DateTimeSettingsChecker {
    fun isAutomaticDateTimeEnabled(context: Context): Boolean {
        try {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AUTO_TIME
            ) === 1 // 1 means enabled
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false // Default to false if the setting is not found
        }
    }

    fun isAutomaticTimeZoneEnabled(context: Context): Boolean {
        try {
            return Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.AUTO_TIME_ZONE
            ) === 1 // 1 means enabled
        } catch (e: Settings.SettingNotFoundException) {
            e.printStackTrace()
            return false // Default to false if the setting is not found
        }
    }
}