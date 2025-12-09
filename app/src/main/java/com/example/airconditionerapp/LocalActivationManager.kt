package com.example.airconditionerapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.security.MessageDigest
import java.util.*

class LocalActivationManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "encrypted_activation"
        private const val KEY_IS_ACTIVATED = "is_activated"
        private const val KEY_ACTIVATION_DATE = "activation_date"
        private const val KEY_ACTIVATION_CODE = "activation_code"
        private const val KEY_DEVICE_ID_HASH = "device_id_hash"
        private const val KEY_GENERATION = "activation_generation"
        private const val KEY_EXPIRY_DATE = "expiry_date"
        private const val KEY_KEY_TYPE = "key_type"
        private const val KEY_ACTIVATION_COUNT = "activation_count"

        // Мастер-ключ из Python генератора
        private const val MASTER_KEY = "A330A200A250"

        // Префиксы ключей
        private val KEY_PREFIXES = mapOf(
            "TST" to "test",      // Тестовый (7 дней)
            "30D" to "30_days",   // 30 дней
            "90D" to "90_days",   // 90 дней
            "1YR" to "1_year",    // 1 год
            "LFT" to "lifetime",  // Навсегда
            "KEY" to "unknown"    // Неизвестный
        )

        // Описания типов ключей
        private val KEY_DESCRIPTIONS = mapOf(
            "test" to "Тестовый (7 дней)",
            "30_days" to "30 дней",
            "90_days" to "90 дней",
            "1_year" to "1 год",
            "lifetime" to "Навсегда",
            "master" to "Мастер-ключ"
        )

        // Сроки действия в днях
        private val KEY_DURATIONS = mapOf(
            "test" to 7,
            "30_days" to 30,
            "90_days" to 90,
            "1_year" to 365,
            "lifetime" to 9999,
            "master" to 9999
        )
    }

    private lateinit var encryptedPrefs: SharedPreferences

    init {
        initializeEncryptedPrefs()
    }

    private fun initializeEncryptedPrefs() {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            encryptedPrefs = EncryptedSharedPreferences.create(
                context,
                PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Fallback на обычные SharedPreferences
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Проверяет, активировано ли приложение
     */
    fun isActivated(): Boolean {
        val activated = encryptedPrefs.getBoolean(KEY_IS_ACTIVATED, false)
        if (!activated) return false

        // Проверяем срок действия
        return !isActivationExpired()
    }

    /**
     * Активирует приложение с указанным ключом
     */
    fun activate(activationKey: String): ActivationResult {
        val deviceId = DeviceUtils.getDeviceId(context)
        val cleanKey = activationKey.replace("-", "").uppercase()

        // 1. Проверяем мастер-ключ
        if (cleanKey == MASTER_KEY) {
            return activateWithMasterKey(cleanKey, deviceId)
        }

        // 2. Проверяем обычный ключ
        val validation = validateActivationKey(cleanKey, deviceId)

        if (validation.isValid) {
            // Сохраняем данные активации
            val keyType = validation.keyType ?: "unknown"
            val expiryDate = calculateExpiryDate(keyType)

            encryptedPrefs.edit()
                .putBoolean(KEY_IS_ACTIVATED, true)
                .putString(KEY_ACTIVATION_DATE, Date().toString())
                .putString(KEY_ACTIVATION_CODE, activationKey)
                .putString(KEY_DEVICE_ID_HASH, hashDeviceId(deviceId))
                .putString(KEY_KEY_TYPE, keyType)
                .putLong(KEY_EXPIRY_DATE, expiryDate)
                .putInt(KEY_ACTIVATION_COUNT, getActivationCount() + 1)
                .apply()

            return ActivationResult(
                success = true,
                message = "✅ Активация успешна!",
                deviceId = deviceId,
                keyType = keyType,
                expiryDate = expiryDate,
                description = KEY_DESCRIPTIONS[keyType] ?: "Неизвестный ключ"
            )
        }

        return ActivationResult(
            success = false,
            message = "❌ Ошибка активации",
            error = validation.error,
            deviceId = deviceId
        )
    }

    /**
     * Активация с мастер-ключом
     */
    private fun activateWithMasterKey(key: String, deviceId: String): ActivationResult {
        // Проверяем, не был ли уже использован мастер-ключ на этом устройстве
        val activationCount = getActivationCount()
        if (activationCount > 0) {
            return ActivationResult(
                success = false,
                message = "❌ Мастер-ключ уже использован на этом устройстве",
                error = "Master key already used",
                deviceId = deviceId
            )
        }

        // Мастер-ключ дает 30 дней
        val expiryDate = System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)

        encryptedPrefs.edit()
            .putBoolean(KEY_IS_ACTIVATED, true)
            .putString(KEY_ACTIVATION_DATE, Date().toString())
            .putString(KEY_ACTIVATION_CODE, key)
            .putString(KEY_DEVICE_ID_HASH, hashDeviceId(deviceId))
            .putString(KEY_KEY_TYPE, "master")
            .putLong(KEY_EXPIRY_DATE, expiryDate)
            .putInt(KEY_ACTIVATION_COUNT, 1)
            .apply()

        return ActivationResult(
            success = true,
            message = "✅ Мастер-ключ активирован! (30 дней)",
            deviceId = deviceId,
            keyType = "master",
            expiryDate = expiryDate,
            description = "Мастер-ключ (30 дней)"
        )
    }

    /**
     * Проверяет ключ активации
     */
    private fun validateActivationKey(key: String, deviceId: String): KeyValidation {
        // 1. Проверяем длину (19 символов без дефисов)
        if (key.length != 19) {
            return KeyValidation(false, error = "Неверная длина ключа. Ожидается 19 символов")
        }

        // 2. Проверяем префикс
        val prefix = key.substring(0, 3)
        val keyType = KEY_PREFIXES[prefix]

        if (keyType == null) {
            return KeyValidation(false, error = "Неизвестный префикс ключа: $prefix")
        }

        // 3. Проверяем хэш устройства
        val keyDeviceHash = key.substring(3, 11)
        val expectedDeviceHash = hashDeviceId(deviceId).substring(0, 8)

        if (keyDeviceHash != expectedDeviceHash) {
            return KeyValidation(false,
                error = "Ключ не для этого устройства. Хэш устройства не совпадает",
                keyType = keyType
            )
        }

        // 4. Проверяем контрольную сумму
        val dataPart = key.substring(0, 17)
        val checksum = key.substring(17, 19)
        val expectedChecksum = calculateChecksum(dataPart)

        if (checksum != expectedChecksum) {
            return KeyValidation(false,
                error = "Неверная контрольная сумма",
                keyType = keyType
            )
        }

        return KeyValidation(true, keyType = keyType)
    }

    /**
     * Вычисляет контрольную сумму (совместимо с Python генератором)
     */
    private fun calculateChecksum(data: String): String {
        var total = 0

        for ((index, char) in data.withIndex()) {
            // ord(char) * (position + 1) % 256
            total += (char.code * (index + 1)) % 256
        }

        val checksumValue = total % 256
        return String.format("%02X", checksumValue)
    }

    /**
     * Рассчитывает дату истечения срока действия
     */
    private fun calculateExpiryDate(keyType: String): Long {
        val days = KEY_DURATIONS[keyType] ?: 30

        return if (days == 9999) {
            Long.MAX_VALUE // "Навсегда"
        } else {
            System.currentTimeMillis() + (days * 24L * 60 * 60 * 1000)
        }
    }

    /**
     * Хэширует ID устройства (SHA-256 как в Python генераторе)
     */
    private fun hashDeviceId(deviceId: String): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(deviceId.toByteArray())
            bytes.joinToString("") { "%02x".format(it) }.uppercase()
        } catch (e: Exception) {
            "00000000000000000000000000000000"
        }
    }

    /**
     * Получает информацию об активации
     */
    fun getActivationInfo(): ActivationInfo {
        val deviceId = DeviceUtils.getDeviceId(context)
        val expiryDate = encryptedPrefs.getLong(KEY_EXPIRY_DATE, 0)
        val keyType = encryptedPrefs.getString(KEY_KEY_TYPE, "unknown") ?: "unknown"
        val isExpired = isActivationExpired()
        val activated = encryptedPrefs.getBoolean(KEY_IS_ACTIVATED, false) && !isExpired

        val remainingDays = if (expiryDate == Long.MAX_VALUE) {
            "Навсегда"
        } else if (expiryDate > 0) {
            val remainingMillis = expiryDate - System.currentTimeMillis()
            val days = (remainingMillis / (24 * 60 * 60 * 1000)).toInt()
            if (days > 0) "$days дней" else "Истек"
        } else {
            "Не активировано"
        }

        return ActivationInfo(
            isActivated = activated,
            activationDate = encryptedPrefs.getString(KEY_ACTIVATION_DATE, "Не активировано"),
            activationCode = encryptedPrefs.getString(KEY_ACTIVATION_CODE, ""),
            deviceId = deviceId,
            formattedDeviceId = DeviceUtils.formatDeviceId(deviceId),
            keyType = keyType,
            keyDescription = KEY_DESCRIPTIONS[keyType] ?: "Неизвестный ключ",
            expiryDate = expiryDate,
            isExpired = isExpired,
            remainingDays = remainingDays,
            activationCount = getActivationCount()
        )
    }

    /**
     * Получает количество активаций
     */
    private fun getActivationCount(): Int {
        return encryptedPrefs.getInt(KEY_ACTIVATION_COUNT, 0)
    }

    /**
     * Сбрасывает активацию (для тестирования)
     */
    fun resetActivation() {
        encryptedPrefs.edit().clear().apply()
    }

    /**
     * Проверяет целостность активации
     */
    fun verifyActivationIntegrity(): Boolean {
        if (!encryptedPrefs.getBoolean(KEY_IS_ACTIVATED, false)) return false

        val storedHash = encryptedPrefs.getString(KEY_DEVICE_ID_HASH, "")
        val currentHash = hashDeviceId(DeviceUtils.getDeviceId(context))

        return storedHash == currentHash
    }

    /**
     * Проверяет, истек ли срок действия активации
     */
    fun isActivationExpired(): Boolean {
        val expiryDate = encryptedPrefs.getLong(KEY_EXPIRY_DATE, 0)

        if (expiryDate == 0L || expiryDate == Long.MAX_VALUE) {
            return false
        }

        return System.currentTimeMillis() > expiryDate
    }

    /**
     * Проверяет, является ли ключ мастер-ключом
     */
    fun isMasterKey(key: String): Boolean {
        val cleanKey = key.replace("-", "").uppercase()
        return cleanKey == MASTER_KEY
    }

    /**
     * Генерирует тестовый ключ для текущего устройства
     * Для отладки и демонстрации
     */
    fun generateTestKey(): String {
        val deviceId = DeviceUtils.getDeviceId(context)
        val deviceHash = hashDeviceId(deviceId).substring(0, 8)
        val prefix = "TST" // Тестовый ключ
        val randomPart = generateRandomPart(6)

        val dataPart = prefix + deviceHash + randomPart
        val checksum = calculateChecksum(dataPart)

        val fullKey = dataPart + checksum

        // Форматируем как в Python генераторе
        return formatKey(fullKey)
    }

    /**
     * Генерирует случайную часть ключа
     */
    private fun generateRandomPart(length: Int): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = Random()

        return (1..length)
            .map { chars[random.nextInt(chars.length)] }
            .joinToString("")
    }

    /**
     * Форматирует ключ с дефисами
     */
    private fun formatKey(key: String): String {
        return "${key.substring(0, 3)}-${key.substring(3, 11)}-${key.substring(11, 17)}-${key.substring(17, 19)}"
    }
}

// Классы для возврата данных
data class ActivationResult(
    val success: Boolean,
    val message: String,
    val error: String? = null,
    val deviceId: String? = null,
    val keyType: String? = null,
    val expiryDate: Long = 0,
    val description: String? = null
)

data class ActivationInfo(
    val isActivated: Boolean,
    val activationDate: String?,
    val activationCode: String?,
    val deviceId: String,
    val formattedDeviceId: String,
    val keyType: String,
    val keyDescription: String,
    val expiryDate: Long,
    val isExpired: Boolean,
    val remainingDays: String,
    val activationCount: Int
)

data class KeyValidation(
    val isValid: Boolean,
    val keyType: String? = null,
    val error: String? = null
)