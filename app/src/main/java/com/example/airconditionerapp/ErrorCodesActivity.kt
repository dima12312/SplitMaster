package com.example.airconditionerapp

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.airconditionerapp.databinding.ActivityErrorCodesBinding
import com.example.airconditionerapp.models.ErrorCode
import com.example.airconditionerapp.models.PaginationConfig
import com.example.airconditionerapp.ErrorCodeAdapter
import com.example.airconditionerapp.models.ErrorCodeConstants

class ErrorCodesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityErrorCodesBinding
    private lateinit var adapter: ErrorCodeAdapter
    private val allErrorCodes = mutableListOf<ErrorCode>()
    private var filteredErrorCodes = mutableListOf<ErrorCode>()
    private var displayedErrorCodes = mutableListOf<ErrorCode>()
    private var selectedBrand: String? = null
    private var selectedSeries: String? = null
    private var currentPage = 1
    private var isLoading = false
    private var hasMoreItems = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityErrorCodesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        selectedBrand = intent.getStringExtra("BRAND_NAME")
        selectedSeries = intent.getStringExtra("SERIES_NAME")

        val title = when {
            selectedBrand != null && selectedSeries != null -> "Коды ошибок: $selectedBrand $selectedSeries"
            selectedBrand != null -> "Коды ошибок: $selectedBrand"
            else -> "Поиск кодов ошибок"
        }
        supportActionBar?.title = title

        initializeErrorCodes()
        adapter = ErrorCodeAdapter(displayedErrorCodes)
        binding.resultsRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.resultsRecyclerView.adapter = adapter
        binding.resultsContainer.visibility = View.GONE
        binding.loadMoreContainer.visibility = View.GONE

        setupSearch()
        setupPagination()

        if (selectedBrand != null && selectedSeries != null) {
            filterByBrandAndSeries(selectedBrand!!, selectedSeries!!)
        } else if (selectedBrand != null) {
            filterByBrand(selectedBrand!!)
        }
    }

    private fun initializeErrorCodes() {
        allErrorCodes.clear()
        allErrorCodes.addAll(ErrorCodeConstants.ERROR_CODES)


    }

    private fun setupSearch() {
        binding.searchButton.setOnClickListener {
            hideKeyboard()
            val searchTerm = binding.searchEditText.text.toString().trim()
            if (searchTerm.isEmpty()) {
                binding.resultsContainer.visibility = View.GONE
                binding.loadMoreContainer.visibility = View.GONE
                return@setOnClickListener
            }
            // Используем УМНЫЙ поиск
            searchErrorCodesSmart(searchTerm)
        }

        binding.filterAeronik.setOnClickListener { showSeriesDialog("Aeronik") }
        binding.filterBallu.setOnClickListener { showSeriesDialog("Ballu") }
        binding.filterCarrier.setOnClickListener { showSeriesDialog("Carrier") }
        binding.filterCooperHunter.setOnClickListener { showSeriesDialog("Cooper&Hunter") }
        binding.filterDaikin.setOnClickListener { showSeriesDialog("Daikin") }
        binding.filterGree.setOnClickListener { showSeriesDialog("Gree") }
        binding.filterSanyo.setOnClickListener { showSeriesDialog("Sanyo") }
        binding.filterTcl.setOnClickListener { showSeriesDialog("TCL") }
        binding.filterLg.setOnClickListener { showSeriesDialog("LG") }
        binding.filterEk.setOnClickListener { showSeriesDialog("EK") }

        binding.clearButton.setOnClickListener {
            hideKeyboard()
            binding.searchEditText.text?.clear()
            binding.resultsContainer.visibility = View.GONE
            binding.loadMoreContainer.visibility = View.GONE
            resetPagination()
        }
    }

    // === УМНЫЙ ПОИСК ===

    /**
     * Умный поиск кодов ошибок - находит и русские, и английские буквы
     */
    private fun searchErrorCodesSmart(query: String) {
        hideKeyboard()
        resetPagination()
        filteredErrorCodes.clear()

        // Нормализуем запрос (приводим к единому формату)
        val normalizedQuery = normalizeForSearch(query)

        for (error in allErrorCodes) {
            // Нормализуем все поля для поиска
            if (normalizeForSearch(error.code).contains(normalizedQuery) ||
                normalizeForSearch(error.brand).contains(normalizedQuery) ||
                normalizeForSearch(error.series).contains(normalizedQuery) ||
                normalizeForSearch(error.description).contains(normalizedQuery)
            ) {
                filteredErrorCodes.add(error)
            }
        }

        filteredErrorCodes.sortWith(compareBy({ it.brand }, { it.series }, { it.code }))

        if (filteredErrorCodes.isEmpty()) {
            showNoResults()
        } else {
            showResults(query)
            loadFirstPage()
        }
    }

    /**
     * Нормализует текст для поиска:
     * 1. Переводит в нижний регистр
     * 2. Заменяет русские буквы на похожие английские
     * 3. Удаляет лишние пробелы
     */
    private fun normalizeForSearch(text: String): String {
        return text.lowercase()
            // Заменяем русские буквы на английские
            .replace('е', 'e')   // русская "е" -> английская "e"
            .replace('ё', 'e')   // русская "ё" -> английская "e"
            .replace('а', 'a')   // русская "а" -> английская "a"
            .replace('о', 'o')   // русская "о" -> английская "o"
            .replace('с', 'c')   // русская "с" -> английская "c"
            .replace('р', 'p')   // русская "р" -> английская "p"
            .replace('х', 'x')   // русская "х" -> английская "x"
            .replace('у', 'y')   // русская "у" -> английская "y"
            .replace('к', 'k')   // русская "к" -> английская "k"
            .replace('н', 'h')   // русская "н" -> английская "h"
            .replace('в', 'b')   // русская "в" -> английская "b"
            .replace('м', 'm')   // русская "м" -> английская "m"
            .replace('т', 't')   // русская "т" -> английская "t"
            // Удаляем лишние пробелы и знаки препинания для более точного поиска
            .replace(" ", "")
            .replace("-", "")
            .replace(".", "")
            .replace(",", "")
            .replace(";", "")
            .replace(":", "")
    }

    // === ОСТАЛЬНЫЕ МЕТОДЫ (остаются без изменений) ===

    private fun showSeriesDialog(brand: String) {
        val seriesList = getSeriesForBrand(brand)
        if (seriesList.isEmpty()) return
        if (seriesList.size == 1) {
            filterByBrandAndSeries(brand, seriesList[0])
            return
        }
        val items = seriesList.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Выберите серию $brand")
            .setItems(items) { _, which ->
                filterByBrandAndSeries(brand, items[which])
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun getSeriesForBrand(brand: String): List<String> {
        return when (brand) {
            "Cooper&Hunter" -> listOf("Общие коды", "Премиум серия", "Инверторная серия")
            "Aeronik" -> listOf("Общие коды", "Бюджетная серия")
            "EK" -> listOf("Alba", "Futura", "Futura Inverter", "мультисплит","Inventor Полупром")
            "Ballu" -> listOf("BS07", "BS09", "BS12", "BS18", "BS24", "BS36")
            "Carrier" -> listOf("CS07", "CS09", "CS12", "CS18", "CS24", "CS36", "CS48")
            "Daikin" -> listOf("DS07", "DS09", "DS12", "DS18", "DS24", "DS36", "DS48", "DS60")
            "Gree" -> listOf("Общие коды")
            "Sanyo" -> listOf("SY07", "SY09", "SY12", "SY18")
            "TCL" -> listOf("TS07", "TS09", "TS12", "TS18", "TS24")
            "LG" -> listOf("LS07", "LS09", "LS12", "LS18", "LS24", "LS36")
            else -> listOf("Общие коды")
        }
    }

    private fun filterByBrand(brand: String) {
        filteredErrorCodes.clear()
        filteredErrorCodes.addAll(allErrorCodes.filter {
            it.brand.equals(
                brand,
                ignoreCase = true
            )
        })
        showResultsForBrand(brand)
        loadFirstPage()
    }

    private fun filterByBrandAndSeries(brand: String, series: String) {
        filteredErrorCodes.clear()
        filteredErrorCodes.addAll(allErrorCodes.filter {
            it.brand.equals(brand, ignoreCase = true) && it.series.equals(series, ignoreCase = true)
        })
        showResultsForBrandAndSeries(brand, series)
        loadFirstPage()
    }

    private fun setupPagination() {
        binding.buttonLoadMore.setOnClickListener { loadMoreItems() }
    }

    private fun showNoResults() {
        binding.noResults.visibility = View.VISIBLE
        binding.resultsRecyclerView.visibility = View.GONE
        binding.resultCount.text = "Найдено ошибок: 0"
        binding.loadMoreContainer.visibility = View.GONE
        binding.resultsContainer.visibility = View.VISIBLE
        binding.resultsTitle.text = "Результаты поиска"
    }

    private fun showResults(query: String) {
        binding.noResults.visibility = View.GONE
        binding.resultsRecyclerView.visibility = View.VISIBLE
        binding.resultsContainer.visibility = View.VISIBLE
        binding.resultsTitle.text = "Результаты поиска: $query"
    }

    private fun showResultsForBrand(brand: String) {
        binding.noResults.visibility = View.GONE
        binding.resultsRecyclerView.visibility = View.VISIBLE
        binding.resultsContainer.visibility = View.VISIBLE
        binding.resultsTitle.text = "Бренд: $brand"
    }

    private fun showResultsForBrandAndSeries(brand: String, series: String) {
        binding.noResults.visibility = View.GONE
        binding.resultsRecyclerView.visibility = View.VISIBLE
        binding.resultsContainer.visibility = View.VISIBLE
        binding.resultsTitle.text = "$brand $series"
    }

    private fun loadFirstPage() {
        currentPage = 1
        displayedErrorCodes.clear()
        val startIndex = 0
        val endIndex = minOf(PaginationConfig.PAGE_SIZE, filteredErrorCodes.size)

        if (endIndex > 0) {
            displayedErrorCodes.addAll(filteredErrorCodes.subList(startIndex, endIndex))
            adapter.updateData(displayedErrorCodes)
            binding.resultCount.text =
                "Найдено ошибок: ${filteredErrorCodes.size} (показано: ${displayedErrorCodes.size})"
            hasMoreItems = filteredErrorCodes.size > displayedErrorCodes.size
            binding.loadMoreContainer.visibility = if (hasMoreItems) View.VISIBLE else View.GONE
        } else {
            showNoResults()
        }
    }

    private fun loadMoreItems() {
        if (isLoading || !hasMoreItems) return
        isLoading = true
        binding.buttonLoadMore.isEnabled = false
        binding.progressBar.visibility = View.VISIBLE

        binding.resultsRecyclerView.postDelayed({
            val startIndex = currentPage * PaginationConfig.PAGE_SIZE
            val endIndex = minOf(startIndex + PaginationConfig.PAGE_SIZE, filteredErrorCodes.size)

            if (startIndex < filteredErrorCodes.size) {
                displayedErrorCodes.addAll(filteredErrorCodes.subList(startIndex, endIndex))
                adapter.updateData(displayedErrorCodes)
                currentPage++
                binding.resultCount.text =
                    "Найдено ошибок: ${filteredErrorCodes.size} (показано: ${displayedErrorCodes.size})"
                hasMoreItems = filteredErrorCodes.size > displayedErrorCodes.size
                binding.loadMoreContainer.visibility = if (hasMoreItems) View.VISIBLE else View.GONE
            }
            isLoading = false
            binding.buttonLoadMore.isEnabled = true
            binding.progressBar.visibility = View.GONE
        }, 500)
    }

    private fun resetPagination() {
        currentPage = 1
        isLoading = false
        hasMoreItems = true
        filteredErrorCodes.clear()
        displayedErrorCodes.clear()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    private fun hideKeyboard() {
        val inputMethodManager =
            getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocus = currentFocus
        if (currentFocus != null) {
            inputMethodManager.hideSoftInputFromWindow(currentFocus.windowToken, 0)
            currentFocus.clearFocus()
        } else {
            inputMethodManager.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }
    }

}