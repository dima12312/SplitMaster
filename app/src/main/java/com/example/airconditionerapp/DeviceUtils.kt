// utils/DeviceUtils.kt
package com.example.airconditionerapp.utils

import android.content.Context
import android.provider.Settings
import java.security.MessageDigest

object DeviceUtils {

    /**
     * Получает уникальный идентификатор устройства
     */
    fun getDeviceId(context: Context): String {
        val androidId = getAndroidId(context)
        val deviceInfo = getDeviceInfo()

        // Создаем стабильный хэш из всех доступных данных устройства
        val combined = "$androidId|$deviceInfo"
        return hashString(combined)
    }

    private fun getAndroidId(context: Context): String {
        return try {
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ) ?: "unknown"
        } catch (e: Exception) {
            "unknown"
        }
    }

    private fun getDeviceInfo(): String {
        return buildString {
            append("BRAND:${android.os.Build.BRAND}")
            append("|MODEL:${android.os.Build.MODEL}")
            append("|PRODUCT:${android.os.Build.PRODUCT}")
            append("|DEVICE:${android.os.Build.DEVICE}")
            append("|BOARD:${android.os.Build.BOARD}")
            append("|HARDWARE:${android.os.Build.HARDWARE}")
            append("|SERIAL:${android.os.Build.SERIAL}")
        }
    }

    private fun hashString(input: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }.substring(0, 16).uppercase()
        } catch (e: Exception) {
            "ERROR${System.currentTimeMillis() % 10000}"
        }
    }

    /**
     * Форматирует ID устройства для отображения
     */
    fun formatDeviceId(deviceId: String): String {
        return deviceId.chunked(4).joinToString("-")
    }
}