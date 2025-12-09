package com.example.airconditionerapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.airconditionerapp.databinding.ActivityCoolingCalculatorBinding

class CoolingCalculatorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCoolingCalculatorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCoolingCalculatorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Расчет мощности охлаждения"

        // Установка значений по умолчанию
        resetForm()

        binding.calculateButton.setOnClickListener {
            calculateCoolingPower()
        }

        binding.resetButton.setOnClickListener {
            resetForm()
        }
    }

    private fun calculateCoolingPower() {
        try {
            // Получаем значения из формы
            val area = binding.areaEditText.text.toString().toDoubleOrNull() ?: 0.0
            val height = binding.heightEditText.text.toString().toDoubleOrNull() ?: 0.0
            val people = binding.peopleEditText.text.toString().toDoubleOrNull() ?: 0.0
            val computers = binding.computersEditText.text.toString().toDoubleOrNull() ?: 0.0
            val tv = binding.tvEditText.text.toString().toDoubleOrNull() ?: 0.0
            val otherPower = binding.otherPowerEditText.text.toString().toDoubleOrNull() ?: 0.0

            // Получаем значение освещенности
            val qValue = when {
                binding.radioIlluminationLow.isChecked -> 30.0
                binding.radioIlluminationMedium.isChecked -> 35.0
                binding.radioIlluminationHigh.isChecked -> 40.0
                else -> 35.0
            }

            // Получаем уровень активности
            val activityMultiplier = when {
                binding.radioActivityLow.isChecked -> 0.1
                binding.radioActivityMedium.isChecked -> 0.13
                binding.radioActivityHigh.isChecked -> 0.2
                binding.radioActivityVeryHigh.isChecked -> 0.3
                else -> 0.13
            }

            // Расчет Q1 (теплоприток от помещения): Q1 = S x h x q / 1000
            val Q1 = area * height * qValue / 1000

            // Расчет Q2 (теплоприток от людей): Q2 = количество людей * уровень активности
            val Q2 = people * activityMultiplier

            // Расчет Q3 (теплоприток от техники)
            val computerHeat = computers * 0.3
            val tvHeat = tv * 0.2
            val otherHeat = otherPower * 0.3
            val Q3 = computerHeat + tvHeat + otherHeat

            // Общая мощность охлаждения: Q = Q1 + Q2 + Q3
            val totalCoolingPower = Q1 + Q2 + Q3

            // Расчет диапазона: от -5% до +15%
            val minRange = totalCoolingPower * 0.95
            val maxRange = totalCoolingPower * 1.15

            // Расчет BTU: BTU = Вт / 0.293
            val btuPower = (totalCoolingPower * 1000 / 0.293).toInt()

            // Определение рекомендуемой модели по BTU
            val btuModel = when {
                btuPower < 7300 -> "7000 BTU"
                btuPower < 9300 -> "9000 BTU"
                btuPower < 12300 -> "12000 BTU"
                btuPower < 18500 -> "18000 BTU"
                btuPower < 24500 -> "24000 BTU"
                btuPower < 36500 -> "36000 BTU"
                btuPower < 50500 -> "48000 BTU"
                else -> "60000 BTU или более"
            }

            // Рекомендации по сплит-системам
            val splitSystem = when {
                totalCoolingPower < 2.2 -> "Рекомендуется сплит-система №07 (до 21м²)"
                totalCoolingPower < 2.7 -> "Рекомендуется сплит-система №09 (до 26м²)"
                totalCoolingPower < 3.6 -> "Рекомендуется сплит-система №12 (до 36м²)"
                totalCoolingPower < 5.2 -> "Рекомендуется сплит-система №18 (до 54м²)"
                totalCoolingPower < 7.0 -> "Рекомендуется сплит-система №24 (до 72м²)"
                totalCoolingPower < 10.0 -> "Рекомендуется сплит-система №36 (до 108м²)"
                totalCoolingPower < 14.0 -> "Рекомендуется сплит-система №48 (до 144м²)"
                totalCoolingPower < 17.0 -> "Рекомендуется сплит-система №60 (до 180м²)"
                else -> "Рекомендуется использовать несколько сплит-систем"
            }

            // Отображение результатов
            displayResults(Q1, Q2, Q3, totalCoolingPower, minRange, maxRange, btuPower, btuModel, splitSystem)

        } catch (e: Exception) {
            android.app.AlertDialog.Builder(this)
                .setTitle("Ошибка")
                .setMessage("Проверьте правильность введенных данных")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun displayResults(
        Q1: Double,
        Q2: Double,
        Q3: Double,
        totalCoolingPower: Double,
        minRange: Double,
        maxRange: Double,
        btuPower: Int,
        btuModel: String,
        splitSystem: String
    ) {
        binding.q1Result.text = "${String.format("%.2f", Q1)} кВт"
        binding.q2Result.text = "${String.format("%.2f", Q2)} кВт"
        binding.q3Result.text = "${String.format("%.2f", Q3)} кВт"
        binding.coolingPower.text = "${String.format("%.2f", totalCoolingPower)} кВт"
        binding.coolingRange.text = "${String.format("%.2f", minRange)} - ${String.format("%.2f", maxRange)} кВт"
        binding.btuPower.text = "$btuPower BTU"
        binding.btuModel.text = btuModel
        binding.splitSystem.text = splitSystem
        binding.resultCardView.visibility = View.VISIBLE
    }

    private fun resetForm() {
        binding.areaEditText.setText("22")
        binding.heightEditText.setText("2.4")
        binding.peopleEditText.setText("2")
        binding.computersEditText.setText("0")
        binding.tvEditText.setText("0")
        binding.otherPowerEditText.setText("0")
        binding.radioIlluminationMedium.isChecked = true
        binding.radioActivityMedium.isChecked = true
        binding.resultCardView.visibility = View.GONE
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}