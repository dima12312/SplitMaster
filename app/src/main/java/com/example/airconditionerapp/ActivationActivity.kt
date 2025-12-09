package com.example.airconditionerapp

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.airconditionerapp.databinding.ActivityActivationBinding
import com.example.airconditionerapp.utils.LocalActivationManager
import java.text.SimpleDateFormat
import java.util.*

class ActivationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityActivationBinding
    private lateinit var activationManager: LocalActivationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityActivationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        activationManager = LocalActivationManager(this)

        // Проверяем, уже активировано ли
        val activationInfo = activationManager.getActivationInfo()

        if (activationInfo.isActivated) {
            showActivatedStatus(activationInfo)
            binding.cardActivationStatus.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                goToMain()
            }, 3000)
            return
        } else if (activationInfo.isExpired) {
            showExpiredStatus(activationInfo)
        }

        setupUI()
    }

    private fun setupUI() {
        // Получаем ID устройства
        val deviceId = com.example.airconditionerapp.utils.DeviceUtils.getDeviceId(this)
        val formattedId = com.example.airconditionerapp.utils.DeviceUtils.formatDeviceId(deviceId)
        binding.tvDeviceId.text = formattedId

        // Кнопка WhatsApp
        binding.btnWhatsApp.setOnClickListener {
            openWhatsApp()
        }

        // Кнопка Telegram - в методе setupUI() добавьте:
        binding.btnTelegram.setOnClickListener {
            openTelegram()}

        // Кнопка тестового ключа
        binding.btnTestActivation.setOnClickListener {
            // Генерируем тестовый ключ для текущего устройства
            val testKey = activationManager.generateTestKey()
            binding.etActivationKey.setText(testKey)

            Toast.makeText(this, "🧪 Тестовый ключ сгенерирован (7 дней)", Toast.LENGTH_LONG).show()
            hideKeyboard()
        }

        // Кнопка активации
        binding.btnActivate.setOnClickListener {
            activateApp()
        }

        // Обработка нажатия Enter в поле ввода
        binding.etActivationKey.setOnEditorActionListener { _, _, _ ->
            hideKeyboard()
            activateApp()
            true
        }
    }

    private fun openWhatsApp() {
        try {
            val deviceId = com.example.airconditionerapp.utils.DeviceUtils.getDeviceId(this)
            val formattedId = com.example.airconditionerapp.utils.DeviceUtils.formatDeviceId(deviceId)
            val phoneNumber = "79184779333"
            val message = """
                Здравствуйте! 
                Мне нужен ключ активации для приложения SplitМастер.
                
                ID устройства: $formattedId
                Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                Android: ${android.os.Build.VERSION.RELEASE}
                
                Пожалуйста, вышлите ключ активации.
                Спасибо!
            """.trimIndent()

            val url = "https://wa.me/$phoneNumber?text=${Uri.encode(message)}"

            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse(url)
            startActivity(intent)

            // Показываем сообщение об успехе
            Toast.makeText(this, "✅ Открывается WhatsApp...", Toast.LENGTH_SHORT).show()

        } catch (e: Exception) {
            Toast.makeText(this, "❌ WhatsApp не установлен", Toast.LENGTH_SHORT).show()

            // Запасной вариант - открываем браузер
            try {
                val url = "https://wa.me/79184779333"
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(intent)
            } catch (e2: Exception) {
                Toast.makeText(this, "❌ Не удалось открыть WhatsApp", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun openTelegram() {
        try {
            val deviceId = com.example.airconditionerapp.utils.DeviceUtils.getDeviceId(this)
            val formattedId = com.example.airconditionerapp.utils.DeviceUtils.formatDeviceId(deviceId)

            val message = """
            Здравствуйте!
            Мне нужен ключ активации для приложения SplitМастер.
            
            ID устройства: $formattedId
            Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
            Android: ${android.os.Build.VERSION.RELEASE}
            
            Пожалуйста, вышлите ключ активации.
            Спасибо!
        """.trimIndent()

            // Открываем Telegram через Intent
            val telegramIntent = Intent(Intent.ACTION_VIEW)
            telegramIntent.data = Uri.parse("tg://msg?text=${Uri.encode(message)}&to=Split_Masteru")

            // Проверяем, установлен ли Telegram
            if (telegramIntent.resolveActivity(packageManager) != null) {
                startActivity(telegramIntent)
                Toast.makeText(this, "✅ Открывается Telegram...", Toast.LENGTH_SHORT).show()
            } else {
                // Если Telegram не установлен, открываем в браузере
                openTelegramInBrowser(message)
            }

        } catch (e: Exception) {
            Toast.makeText(this, "❌ Ошибка открытия Telegram", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openTelegramInBrowser(message: String) {
        try {
            // Формируем URL для веб-версии Telegram
            val url = "https://t.me/Split_Masteru?text=${Uri.encode(message)}"
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "❌ Не удалось открыть Telegram", Toast.LENGTH_LONG).show()
        }
    }

    private fun showTelegramFallback() {
        try {
            val deviceId = com.example.airconditionerapp.utils.DeviceUtils.getDeviceId(this)
            val formattedId = com.example.airconditionerapp.utils.DeviceUtils.formatDeviceId(deviceId)

            val message = """
            Здравствуйте!
            Мне нужен ключ активации для приложения SplitМастер.
            
            ID устройства: $formattedId
            Устройство: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
            Android: ${android.os.Build.VERSION.RELEASE}
            
            Пожалуйста, вышлите ключ активации.
            Спасибо!
        """.trimIndent()

            // 1. Копируем сообщение
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Активация", message)
            clipboard.setPrimaryClip(clip)

            // 2. Показываем диалог с инструкцией
            AlertDialog.Builder(this)
                .setTitle("📋 Telegram не установлен")
                .setMessage("Сообщение с вашим ID скопировано в буфер обмена:\n\n" +
                        "$formattedId\n\n" +
                        "Что делать:\n" +
                        "1. Установите Telegram из Play Маркета\n" +
                        "2. Найдите @Split_Masteru\n" +
                        "3. Вставьте это сообщение\n" +
                        "4. Отправьте для получения ключа")
                .setPositiveButton("Установить Telegram") { _, _ ->
                    // Открываем Play Маркет
                    openPlayStore()
                }
                .setNeutralButton("Скопировать еще раз") { _, _ ->
                    // Копируем еще раз
                    Toast.makeText(this, "Скопировано: $formattedId", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("OK", null)
                .show()

        } catch (e: Exception) {
            Toast.makeText(this, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPlayStore() {
        try {
            // Пробуем открыть Play Маркет
            val playStoreIntent = Intent(Intent.ACTION_VIEW)
            playStoreIntent.data = Uri.parse("market://details?id=org.telegram.messenger")
            startActivity(playStoreIntent)
        } catch (e: Exception) {
            // Если Play Маркет не доступен, открываем в браузере
            val webIntent = Intent(Intent.ACTION_VIEW)
            webIntent.data = Uri.parse("https://play.google.com/store/apps/details?id=org.telegram.messenger")
            startActivity(webIntent)
        }
    }
    private fun activateApp() {
        val key = binding.etActivationKey.text.toString().trim()

        if (key.isEmpty()) {
            Toast.makeText(this, "Введите ключ активации", Toast.LENGTH_SHORT).show()
            return
        }

        // Проверяем формат ключа
        if (!key.matches(Regex("^[A-Z0-9]{3}-[A-Z0-9]{8}-[A-Z0-9]{6}-[A-Z0-9]{2}$"))) {
            Toast.makeText(this, "Неверный формат ключа", Toast.LENGTH_LONG).show()
            return
        }

        // Скрываем клавиатуру
        hideKeyboard()

        // Блокируем кнопку и показываем прогресс
        binding.btnActivate.isEnabled = false
        binding.btnActivate.text = "⏳ Проверка..."
        binding.progressBar.visibility = View.VISIBLE

        // Используем LocalActivationManager для безопасной активации
        Handler(Looper.getMainLooper()).postDelayed({
            val result = activationManager.activate(key)

            if (result.success) {
                // Показываем успех
                binding.tvActivationStatus.text = "✅ АКТИВИРОВАНО"
                binding.tvActivationStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                // Показываем информацию о ключе
                val activationInfo = activationManager.getActivationInfo()
                showActivatedStatus(activationInfo)
                binding.cardActivationStatus.visibility = View.VISIBLE

                // Вибрация успеха
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator?
                vibrator?.vibrate(100)

                Toast.makeText(this, result.message, Toast.LENGTH_LONG).show()

                // Переход через 3 секунды
                Handler(Looper.getMainLooper()).postDelayed({
                    goToMain()
                }, 3000)
            } else {
                // Вибрация ошибки
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator?
                vibrator?.vibrate(longArrayOf(0, 200, 100, 200), -1)

                Toast.makeText(this, result.error ?: "❌ Ошибка активации", Toast.LENGTH_LONG).show()
                binding.btnActivate.isEnabled = true
                binding.btnActivate.text = "✅ Активировать"
                binding.progressBar.visibility = View.GONE
            }
        }, 1500)
    }

    private fun showActivatedStatus(info: com.example.airconditionerapp.utils.ActivationInfo) {
        binding.tvActivationStatus.text = "✅ АКТИВИРОВАНО"
        binding.tvActivationStatus.setTextColor(getColor(android.R.color.holo_green_dark))

        // Форматируем дату
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val expiryDate = if (info.expiryDate == Long.MAX_VALUE) {
            "Навсегда"
        } else {
            dateFormat.format(Date(info.expiryDate))
        }

        // Показываем детали активации
        binding.tvActivationDetails.text =
            "📱 ID: ${info.formattedDeviceId}\n" +
                    "🔑 Тип: ${info.keyDescription}\n" +
                    "📅 Действует до: $expiryDate\n" +
                    "⏳ Осталось: ${info.remainingDays}"
    }

    private fun showExpiredStatus(info: com.example.airconditionerapp.utils.ActivationInfo) {
        binding.tvActivationStatus.text = "❌ СРОК ДЕЙСТВИЯ ИСТЕК"
        binding.tvActivationStatus.setTextColor(getColor(android.R.color.holo_red_dark))
        binding.cardActivationStatus.visibility = View.VISIBLE

        // Форматируем дату
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val expiryDate = if (info.expiryDate == Long.MAX_VALUE) {
            "Навсегда"
        } else {
            dateFormat.format(Date(info.expiryDate))
        }

        binding.tvActivationDetails.text =
            "📱 ID: ${info.formattedDeviceId}\n" +
                    "🔑 Тип: ${info.keyDescription}\n" +
                    "📅 Истекло: $expiryDate\n\n" +
                    "⚠️ Требуется новая активация"
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }

    private fun hideKeyboard() {
        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        } else {
            inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
    }
}