package com.example.airconditionerapp

import android.util.Log
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.airconditionerapp.databinding.ActivityMainBinding
import com.example.airconditionerapp.utils.LocalActivationManager
import com.example.airconditionerapp.utils.UpdateManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var activationManager: LocalActivationManager
    private lateinit var updateManager: UpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        activationManager = LocalActivationManager(this)
        updateManager = UpdateManager(this)

        // ПРОВЕРКА АКТИВАЦИИ через LocalActivationManager
        if (!activationManager.isActivated()) {
            // Если не активировано - идем на активацию
            startActivity(Intent(this, ActivationActivity::class.java))
            finish()
            return
        }

        // Если активировано - показываем главный экран
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.title = "SplitМастер"

        // Проверка обновлений при запуске
        if (updateManager.shouldCheckForUpdate()) {
            updateManager.checkForUpdates(this)
        }

        binding.btnErrorCodes.setOnClickListener {
            startActivity(Intent(this, ErrorCodesActivity::class.java))
        }

        binding.btnCoolingCalculator.setOnClickListener {
            startActivity(Intent(this, CoolingCalculatorActivity::class.java))
        }

        binding.btnWhatsApp.setOnClickListener {
            openWhatsApp()
        }
        binding.btnTelegram.setOnClickListener {
            openTelegram()
        }
        binding.btnActivationInfo.setOnClickListener {
            showActivationInfo()
        }

        // Добавляем кнопку проверки обновлений
        binding.btnCheckUpdate.setOnClickListener {
            Toast.makeText(this, "🔍 Проверяем обновления...", Toast.LENGTH_SHORT).show()
            updateManager.checkForUpdates(this, showNoUpdateMessage = true)
        }
    }

    private fun openWhatsApp() {
        try {
            val phoneNumber = "79184779333"
            val message = "Здравствуйте!"
            val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp не установлен", Toast.LENGTH_SHORT).show()
            // Открываем браузер как запасной вариант
            val url = "https://wa.me/79184779333"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    private fun openTelegram() {
        try {
            // Формируем простое сообщение (без ID устройства)
             val message = "Здравствуйте!"

            // Пробуем открыть через приложение Telegram
            val telegramIntent = Intent(Intent.ACTION_VIEW)
            telegramIntent.data = Uri.parse("tg://msg?text=${Uri.encode(message)}&to=Split_Masteru")

            // Проверяем, установлен ли Telegram
            if (telegramIntent.resolveActivity(packageManager) != null) {
                startActivity(telegramIntent)
                Toast.makeText(this, "✅ Открываем Telegram...", Toast.LENGTH_SHORT).show()
            } else {
                // Если Telegram не установлен, открываем в браузере
                openTelegramInBrowser()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка открытия Telegram", Toast.LENGTH_SHORT).show()

            // Запасной вариант
            try {
                val url = "https://t.me/Split_Masteru"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "❌ Не удалось открыть Telegram", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openTelegramInBrowser() {
        try {
            val url = "https://t.me/Split_Masteru"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
            Toast.makeText(this, "✅ Открываем Telegram в браузере...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Не удалось открыть Telegram", Toast.LENGTH_LONG).show()
        }
    }

    private fun showActivationInfo() {
        val info = activationManager.getActivationInfo()
        val dateFormat = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
        val expiryDate = if (info.expiryDate == Long.MAX_VALUE) {
            "Навсегда"
        } else {
            dateFormat.format(java.util.Date(info.expiryDate))
        }

        android.app.AlertDialog.Builder(this)
            .setTitle("📊 Информация об активации")
            .setMessage(
                "✅ Приложение активировано\n\n" +
                        "🔑 Тип ключа: ${info.keyDescription}\n" +
                        "📅 Активировано: ${info.activationDate}\n" +
                        "⏳ Действует до: $expiryDate\n" +
                        "📊 Осталось: ${info.remainingDays}\n" +
                        "📱 ID устройства: ${info.formattedDeviceId}\n" +
                        "📦 Версия приложения: ${getAppVersion()}"
            )
            .setPositiveButton("ОК", null)
            .setNeutralButton("📋 Скопировать ID") { _, _ ->
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Device ID", info.deviceId)
                clipboard.setPrimaryClip(clip)
                Toast.makeText(this, "ID скопирован", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun getAppVersion(): String {
        return try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            "v${pInfo.versionName} (${pInfo.versionCode})"
        } catch (e: Exception) {
            "Неизвестно"
        }
    }
}