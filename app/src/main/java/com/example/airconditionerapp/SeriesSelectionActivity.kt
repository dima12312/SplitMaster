package com.example.airconditionerapp

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.airconditionerapp.databinding.ActivitySeriesSelectionBinding

class SeriesSelectionActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySeriesSelectionBinding  // ViewBinding для доступа к элементам интерфейса
    private lateinit var adapter: SeriesAdapter                   // Адаптер для списка серий
    private var selectedBrand: String = ""                        // Выбранный бренд (получаем из предыдущей активности)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализация ViewBinding
        binding = ActivitySeriesSelectionBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Настройка кнопки "Назад" в ActionBar
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Получаем выбранный бренд из предыдущей активности
        selectedBrand = intent.getStringExtra("BRAND_NAME") ?: ""

        // Устанавливаем заголовок активности
        supportActionBar?.title = "Серии $selectedBrand"

        // Получаем список серий для выбранного бренда
        val seriesList = getSeriesForBrand(selectedBrand)

        // Инициализация адаптера списка серий
        adapter = SeriesAdapter(seriesList) { series ->
            // Обработчик клика по серии
            // При выборе серии переходим к отображению кодов ошибок
            val intent = Intent(this, ErrorCodesActivity::class.java)
            intent.putExtra("BRAND_NAME", selectedBrand)   // Передаем бренд
            intent.putExtra("SERIES_NAME", series)         // Передаем серию
            startActivity(intent)
        }

        // Настройка RecyclerView
        binding.seriesRecyclerView.layoutManager = GridLayoutManager(this, 2)  // 2 колонки
        binding.seriesRecyclerView.adapter = adapter
    }

    /**
     * Получает список серий для указанного бренда
     * @param brand Название бренда
     * @return Список серий кондиционеров для этого бренда
     */
    private fun getSeriesForBrand(brand: String): List<String> {
        // В реальном приложении эти данные должны загружаться из базы данных или API
        // Здесь приведены примеры данных для демонстрации
        return when (brand) {
            "EK" -> listOf("Alba", "Futura", "Futura Inverter",)
            "Aeronik" -> listOf("Общие коды")
            "Ballu" -> listOf("Общие коды")
            "Carrier" -> listOf("Общие коды")
            "Cooper&Hunter" -> listOf("Общие коды")
            "Daikin" -> listOf("Общие коды")
            "Gree" -> listOf("Общие коды")
            "Sanyo" -> listOf("Общие коды")
            "TCL" -> listOf("Общие коды")
            "LG" -> listOf("Общие коды")
            else -> emptyList()  // Если бренд не найден, возвращаем пустой список
        }
    }

    // Обработка нажатия кнопки "Назад" в ActionBar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}

/**
 * Адаптер для отображения списка серий кондиционеров
 * @param seriesList Список серий для отображения
 * @param onItemClick Колбэк, вызываемый при выборе серии
 */
class SeriesAdapter(
    private val seriesList: List<String>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<SeriesAdapter.ViewHolder>() {

    /**
     * ViewHolder для хранения ссылок на элементы интерфейса
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val button: Button = view.findViewById(R.id.buttonSeries)  // Кнопка с названием серии
    }

    /**
     * Создает новый ViewHolder
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Загружаем макет для элемента списка
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_series, parent, false)
        return ViewHolder(view)
    }

    /**
     * Привязывает данные к ViewHolder
     * @param holder ViewHolder для заполнения
     * @param position Позиция элемента в списке
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val series = seriesList[position]  // Получаем серию по позиции

        // Устанавливаем текст кнопки
        holder.button.text = series

        // Устанавливаем обработчик клика
        holder.button.setOnClickListener {
            onItemClick(series)  // Вызываем колбэк с выбранной серией
        }
    }

    /**
     * Возвращает количество элементов в списке
     */
    override fun getItemCount() = seriesList.size
}