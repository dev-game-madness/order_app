package com.kaznach.ordersapp

object OrderCategories {
    val categoriesAndSubcategories = mapOf(
        "Строительные материалы" to listOf(
            "Основные материалы",
            "Отделочные материалы",
            "Инженерные системы",
            "Дополнительные материалы"
        ),
        "Строительные работы" to listOf(
            "Земляные работы",
            "Фундаментные работы",
            "Стенные работы",
            "Кровельные работы",
            "Отделочные работы",
            "Инженерные работы",
            "Дополнительные работы"
        ),
        "Проектирование" to listOf(
            "Архитектурное проектирование",
            "Конструктивное проектирование",
            "Инженерные системы",
            "Дополнительные услуги"
        ),
        "Оборудование и техника" to listOf(
            "Строительная техника",
            "Инструменты",
            "Инвентарь"
        ),
        "Услуги" to listOf(
            "Доставка материалов",
            "Монтаж и пуско-наладка",
            "Гарантийное обслуживание"
        ),
        "Другое" to listOf(
            "Другое"
        )
    )
}