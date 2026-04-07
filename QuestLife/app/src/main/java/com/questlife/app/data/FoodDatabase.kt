package com.questlife.app.data

import android.content.Context
import com.questlife.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream

// База популярных продуктов с БЖУ
object FoodDatabase {
    
    private var allFoods: List<Food> = emptyList()
    private val customFoods = mutableListOf<Food>()
    private var isInitialized = false
    
    /**
     * Инициализация базы данных из JSON файла в assets
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) {
            android.util.Log.d("FoodDatabase", "База уже инициализирована, продуктов: ${allFoods.size}")
            return@withContext
        }
        
        try {
            android.util.Log.d("FoodDatabase", "Начало загрузки foods.json из assets")
            val inputStream: InputStream = context.assets.open("foods.json")
            android.util.Log.d("FoodDatabase", "Файл найден, размер: ${inputStream.available()} байт")
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            android.util.Log.d("FoodDatabase", "JSON прочитан, длина: ${jsonString.length} символов")
            val jsonArray = org.json.JSONArray(jsonString)
            android.util.Log.d("FoodDatabase", "JSONArray создан, количество элементов: ${jsonArray.length()}")
            
            val foodsList = mutableListOf<Food>()
            for (i in 0 until jsonArray.length()) {
                val foodObj = jsonArray.getJSONObject(i)
                val macrosObj = foodObj.getJSONObject("macros")
                
                val food = Food(
                    id = foodObj.optString("id", java.util.UUID.randomUUID().toString()),
                    name = foodObj.optString("name", "Без названия"),
                    description = foodObj.optString("description", ""),
                    macros = Macros(
                        proteins = macrosObj.optDouble("proteins", 0.0),
                        fats = macrosObj.optDouble("fats", 0.0),
                        carbohydrates = macrosObj.optDouble("carbohydrates", 0.0)
                    ),
                    servingSize = foodObj.optDouble("servingSize", 100.0),
                    servingUnit = foodObj.optString("servingUnit", "г"),
                    category = try {
                        FoodCategory.valueOf(foodObj.optString("category", "OTHER"))
                    } catch (e: Exception) {
                        android.util.Log.w("FoodDatabase", "Неизвестная категория для продукта ${foodObj.optString("name")}, используем OTHER")
                        FoodCategory.OTHER
                    },
                    isCustom = false
                )
                foodsList.add(food)
            }
            
            allFoods = foodsList
            isInitialized = true
            android.util.Log.d("FoodDatabase", "Инициализация завершена успешно! Загружено продуктов: ${allFoods.size}")
            android.util.Log.d("FoodDatabase", "Первые 10 продуктов: ${allFoods.take(10).map { it.name }}")
        } catch (e: Exception) {
            android.util.Log.e("FoodDatabase", "Критическая ошибка при загрузке JSON: ${e.message}", e)
            e.printStackTrace()
            // Fallback к базовым продуктам если JSON не загрузился
            allFoods = popularFoods
            isInitialized = true
        }
    }
    
    /**
     * Все продукты: из JSON + пользовательские
     */
    val foods: List<Food>
        get() = if (isInitialized) allFoods + customFoods else popularFoods + customFoods
    
    /**
     * Популярные продукты для быстрого доступа (топ 50 по алфавиту)
     */
    val popularFoods: List<Food>
        get() = foods.take(50)
    
    // Базовые продукты (fallback если JSON не загрузился)
    private val basePopularFoods = listOf(
        Food(
            name = "Куриная грудка",
            macros = Macros(proteins = 23.0, fats = 1.9, carbohydrates = 0.0),
            servingSize = 100.0,
            category = FoodCategory.PROTEIN
        ),
        Food(
            name = "Яйцо куриное",
            macros = Macros(proteins = 12.7, fats = 11.5, carbohydrates = 0.7),
            servingSize = 100.0,
            servingUnit = "шт (≈50г)",
            category = FoodCategory.PROTEIN
        ),
        Food(
            name = "Творог 5%",
            macros = Macros(proteins = 16.0, fats = 5.0, carbohydrates = 2.0),
            servingSize = 100.0,
            category = FoodCategory.DAIRY
        ),
        Food(
            name = "Овсяная каша",
            macros = Macros(proteins = 12.3, fats = 6.1, carbohydrates = 59.5),
            servingSize = 100.0,
            category = FoodCategory.GRAINS
        ),
        Food(
            name = "Гречка вареная",
            macros = Macros(proteins = 4.2, fats = 1.3, carbohydrates = 21.0),
            servingSize = 100.0,
            category = FoodCategory.GRAINS
        )
    )

    /**
     * Добавить пользовательский продукт
     */
    fun addCustomFood(food: Food) {
        customFoods.add(food.copy(isCustom = true))
    }

    /**
     * Поиск продуктов по названию (оптимизированный)
     */
    fun searchFoods(query: String): List<Food> {
        if (query.isEmpty()) return foods.take(50)
        return foods.filter {
            it.name.contains(query, ignoreCase = true)
        }.take(100) // Ограничиваем результат для производительности
    }

    /**
     * Фильтр по категориям
     */
    fun getFoodsByCategory(category: FoodCategory): List<Food> {
        return foods.filter { it.category == category }
    }

    /**
     * Продукты с высоким содержанием белка
     */
    fun getProteinFoods(): List<Food> {
        return foods.filter { it.macros.proteins > 10.0 }
    }
    
    /**
     * Получить количество продуктов в базе
     */
    fun getFoodCount(): Int = foods.size
}