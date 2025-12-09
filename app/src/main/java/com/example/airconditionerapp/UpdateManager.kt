package com.example.airconditionerapp.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {

    companion object {
        private const val TAG = "UpdateManager"

        // URL к вашему файлу на GitHub (ЗАМЕНИТЕ USERNAME на свой!)
        private const val UPDATE_CONFIG_URL = "https://raw.githubusercontent.com/dima12312 /SplitMaster/main/update_config.json"

        // Ключи для JSON
        private const val MIN_VERSION_CODE = "min_version_code"
        private const val CURRENT_VERSION_CODE = "current_version_code"
        private const val UPDATE_URL = "update_url"
        private const val FORCE_UPDATE = "force_update"
        private const val UPDATE_MESSAGE = "update_message"
    }

    private val prefs = context.getSharedPreferences("update_prefs", Context.MODE_PRIVATE)

    /**
     * Проверяет наличие обновлений
     */
    fun checkForUpdates(activity: Activity, showNoUpdateMessage: Boolean = false) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Log.d(TAG, "Проверка обновлений...")

                val url = URL(UPDATE_CONFIG_URL)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000 // 10 секунд
                connection.readTimeout = 10000

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    val inputStream = connection.inputStream
                    val jsonText = inputStream.bufferedReader().use { it.readText() }
                    inputStream.close()

                    Log.d(TAG, "Получен конфиг: $jsonText")
                    parseUpdateConfig(jsonText, activity, showNoUpdateMessage)
                } else {
                    Log.e(TAG, "Ошибка HTTP: ${connection.responseCode}")
                    if (showNoUpdateMessage) {
                        activity.runOnUiThread {
                            Toast.makeText(activity, "Не удалось проверить обновления", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                connection.disconnect()

            } catch (e: Exception) {
                Log.e(TAG, "Ошибка проверки обновлений: ${e.message}")
                if (showNoUpdateMessage) {
                    activity.runOnUiThread {
                        Toast.makeText(activity, "Ошибка подключения", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /**
     * Парсит JSON конфиг
     */
    private fun parseUpdateConfig(
        configJson: String,
        activity: Activity,
        showNoUpdateMessage: Boolean
    ) {
        try {
            val json = JSONObject(configJson)
            val minVersion = json.optInt(MIN_VERSION_CODE, 0)
            val currentVersion = json.optInt(CURRENT_VERSION_CODE, 0)
            val updateUrl = json.optString(UPDATE_URL, "")
            val forceUpdate = json.optBoolean(FORCE_UPDATE, false)
            val message = json.optString(
                UPDATE_MESSAGE,
                "Доступно обновление приложения. Установите новую версию для улучшения работы."
            )

            val currentVersionCode = getCurrentVersionCode()

            Log.d(TAG, "Текущая версия: $currentVersionCode")
            Log.d(TAG, "Минимальная версия: $minVersion")
            Log.d(TAG, "Текущая версия на сервере: $currentVersion")

            // Проверяем, нужно ли обновление
            val needsUpdate = currentVersionCode < minVersion ||
                    (forceUpdate && currentVersionCode < currentVersion)

            activity.runOnUiThread {
                if (needsUpdate) {
                    showUpdateDialog(activity, message, updateUrl, forceUpdate)
                } else if (showNoUpdateMessage) {
                    Toast.makeText(activity, "✅ У вас установлена актуальная версия", Toast.LENGTH_LONG).show()
                }
            }

            // Сохраняем последнюю проверку
            prefs.edit()
                .putLong("last_update_check", System.currentTimeMillis())
                .putInt("last_checked_version", currentVersionCode)
                .apply()

        } catch (e: Exception) {
            Log.e(TAG, "Ошибка парсинга JSON: ${e.message}")
            if (showNoUpdateMessage) {
                activity.runOnUiThread {
                    Toast.makeText(activity, "Ошибка данных обновления", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Получает текущий versionCode приложения
     */
    private fun getCurrentVersionCode(): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionCode
        } catch (e: PackageManager.NameNotFoundException) {
            Log.e(TAG, "Не найден package info: ${e.message}")
            0
        }
    }

    /**
     * Показывает диалог обновления
     */
    private fun showUpdateDialog(
        activity: Activity,
        message: String,
        updateUrl: String,
        forceUpdate: Boolean
    ) {
        android.app.AlertDialog.Builder(activity)
            .setTitle("🔄 Обновление приложения")
            .setMessage(message)
            .setCancelable(!forceUpdate) // Нельзя отменить принудительное обновление

            .setPositiveButton("Обновить") { _, _ ->
                openUpdateUrl(activity, updateUrl)

                // Если принудительное обновление - закрываем приложение
                if (forceUpdate) {
                    activity.finishAffinity()
                }
            }

            .apply {
                if (!forceUpdate) {
                    setNegativeButton("Позже") { dialog, _ ->
                        dialog.dismiss()
                    }

                    setNeutralButton("Больше не напоминать") { _, _ ->
                        prefs.edit()
                            .putBoolean("dont_show_updates", true)
                            .apply()
                    }
                }
            }

            .setOnCancelListener {
                if (forceUpdate) {
                    // Если пользователь пытается закрыть принудительное обновление
                    openUpdateUrl(activity, updateUrl)
                    activity.finishAffinity()
                }
            }

            .show()
    }

    /**
     * Открывает ссылку на обновление
     */
    private fun openUpdateUrl(activity: Activity, url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))

            // Проверяем, можно ли открыть ссылку
            if (intent.resolveActivity(activity.packageManager) != null) {
                activity.startActivity(intent)
            } else {
                Toast.makeText(activity, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(activity, "Ошибка открытия ссылки", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Ошибка открытия URL: ${e.message}")
        }
    }

    /**
     * Проверяет, нужно ли делать проверку обновлений
     */
    fun shouldCheckForUpdate(): Boolean {
        // Проверяем, не отключил ли пользователь напоминания
        if (prefs.getBoolean("dont_show_updates", false)) {
            return false
        }

        val lastCheck = prefs.getLong("last_update_check", 0)
        val now = System.currentTimeMillis()
        val oneWeek = 7 * 24 * 60 * 60 * 1000L // 1 неделя

        return now - lastCheck > oneWeek
    }

    /**
     * Сбрасывает настройки обновлений (для тестирования)
     */
    fun resetUpdateSettings() {
        prefs.edit().clear().apply()
    }
}