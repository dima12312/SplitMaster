package com.example.airconditionerapp.models

// Класс для хранения информации о коде ошибки
data class ErrorCode(
    val id: Int,                    // Уникальный идентификатор ошибки
    val brand: String,              // Название бренда (например, "Daikin")
    val series: String,             // Серия кондиционера (например, "AS07")
    val code: String,               // Код ошибки (например, "E1")
    val description: String         // Описание ошибки
)

// Класс для хранения информации о бренде и его сериях
data class Brand(
    val id: Int,                    // Уникальный идентификатор бренда
    val name: String,               // Название бренда
    val seriesList: List<String>    // Список доступных серий для этого бренда
)

// Объект для настроек пагинации
object PaginationConfig {
    const val PAGE_SIZE = 20        // Количество элементов, отображаемых на одной странице
}