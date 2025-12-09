package com.example.airconditionerapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.airconditionerapp.models.ErrorCode

class ErrorCodeAdapter(private var errorCodes: List<ErrorCode>) :
    RecyclerView.Adapter<ErrorCodeAdapter.ViewHolder>() {

    // Храним, какие элементы развернуты
    private val expandedItems = mutableSetOf<Int>()

    fun updateData(newErrorCodes: List<ErrorCode>) {
        this.errorCodes = newErrorCodes
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val brand: TextView = view.findViewById(R.id.textViewBrand)
        val series: TextView = view.findViewById(R.id.textViewSeries)
        val code: TextView = view.findViewById(R.id.textViewCode)
        val shortDescription: TextView = view.findViewById(R.id.textViewShortDescription)
        val fullDescription: TextView = view.findViewById(R.id.textViewFullDescription)
        val buttonMore: Button = view.findViewById(R.id.buttonMore)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_error_code, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val error = errorCodes[position]

        // Бренд и серия
        holder.brand.text = error.brand
        holder.series.text = error.series

        // Код ошибки
        holder.code.text = "Код: ${error.code}"

        // РАЗДЕЛЯЕМ ОПИСАНИЕ НА ЧАСТИ
        val description = error.description
        val splitResult = splitDescription(description)
        val shortDesc = splitResult.first
        val fullDesc = splitResult.second

        // Короткое описание (до первой точки или 100 символов)
        holder.shortDescription.text = shortDesc

        // Полное описание (всё остальное)
        holder.fullDescription.text = fullDesc

        // Проверяем, развернут ли элемент
        val isExpanded = expandedItems.contains(position)

        // Показываем/скрываем полное описание
        holder.fullDescription.visibility = if (isExpanded) View.VISIBLE else View.GONE

        // Меняем текст кнопки
        holder.buttonMore.text = if (isExpanded) "Скрыть ▲" else "Подробнее ▼"

        // Обработчик клика на кнопку
        holder.buttonMore.setOnClickListener {
            if (isExpanded) {
                expandedItems.remove(position)
            } else {
                expandedItems.add(position)
            }
            notifyItemChanged(position)
        }

        // Если полное описание пустое (вся информация в коротком), скрываем кнопку
        if (fullDesc.isEmpty() || fullDesc == shortDesc) {
            holder.buttonMore.visibility = View.GONE
        } else {
            holder.buttonMore.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int {
        return errorCodes.size
    }

    /**
     * Разделяет описание на короткую и полную части
     * Короткая: до первой точки или первые 100 символов
     * Полная: всё остальное
     */
    private fun splitDescription(description: String): Pair<String, String> {
        // Ищем первую точку
        val firstDotIndex = description.indexOf('.')

        // Ищем перенос строки после "🔧" (начало решений)
        val solutionsIndex = description.indexOf("🔧")

        val result = when {
            // Если есть точка ДО раздела с решениями
            firstDotIndex != -1 && (solutionsIndex == -1 || firstDotIndex < solutionsIndex) -> {
                val shortDesc = description.substring(0, firstDotIndex + 1)
                val fullDesc = description.substring(firstDotIndex + 1).trim()
                Pair(shortDesc, fullDesc)
            }

            // Если есть раздел с решениями (🔧)
            solutionsIndex != -1 -> {
                val shortDesc = description.substring(0, solutionsIndex).trim()
                val fullDesc = description.substring(solutionsIndex).trim()
                Pair(shortDesc, fullDesc)
            }

            // Если описание очень длинное без разделителей
            description.length > 100 -> {
                val shortDesc = description.substring(0, 100) + "..."
                val fullDesc = description.substring(100).trim()
                Pair(shortDesc, fullDesc)
            }

            // Если описание короткое
            else -> {
                Pair(description, "")
            }
        }

        return result
    }
}