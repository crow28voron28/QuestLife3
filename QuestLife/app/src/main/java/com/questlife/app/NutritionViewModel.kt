// NutritionViewModel.kt
package com.questlife.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.questlife.app.data.FoodDatabase
import com.questlife.app.models.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val _dailyNutrition = MutableStateFlow(
        DailyNutrition(
            targetCalories = 2000,
            targetMacros = Macros(proteins = 150.0, fats = 67.0, carbohydrates = 250.0),
            meals = emptyList()
        )
    )
    val dailyNutrition: StateFlow<DailyNutrition> = _dailyNutrition.asStateFlow()
    
    private val _foods = MutableStateFlow<List<Food>>(emptyList())
    val foods: StateFlow<List<Food>> = _foods.asStateFlow()
    
    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    init {
        loadFoods()
        loadTodaysMeals()
    }
    
    /**
     * Загрузка продуктов из базы данных
     */
    private fun loadFoods() {
        viewModelScope.launch {
            try {
                Log.d("NutritionVM", "Начало инициализации FoodDatabase")
                FoodDatabase.initialize(getApplication())
                Log.d("NutritionVM", "Инициализация завершена, получение списка продуктов")
                val loadedFoods = FoodDatabase.foods
                Log.d("NutritionVM", "Загружено продуктов: ${loadedFoods.size}")
                Log.d("NutritionVM", "Первые 5 продуктов: ${loadedFoods.take(5).map { it.name }}")
                _foods.value = loadedFoods
                _isLoading.value = false
            } catch (e: Exception) {
                Log.e("NutritionVM", "Ошибка загрузки продуктов: ${e.message}", e)
                _foods.value = FoodDatabase.popularFoods
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Обновление списка продуктов
     */
    fun refreshFoods() {
        _foods.value = FoodDatabase.foods
    }
    
    /**
     * Загрузка приёмов пищи за сегодня
     */
    private fun loadTodaysMeals() {
        // В реальной реализации здесь была бы загрузка из Room/базы данных
        // Пока оставляем пустым - данные хранятся только в памяти
    }
    
    /**
     * Добавить приём пищи
     */
    fun addMeal(mealEntry: MealEntry) {
        val currentMeals = _dailyNutrition.value.meals
        _dailyNutrition.value = _dailyNutrition.value.copy(
            meals = currentMeals + mealEntry
        )
    }
    
    /**
     * Удалить приём пищи
     */
    fun removeMeal(mealEntry: MealEntry) {
        val currentMeals = _dailyNutrition.value.meals
        _dailyNutrition.value = _dailyNutrition.value.copy(
            meals = currentMeals.filter { it.id != mealEntry.id }
        )
    }
    
    /**
     * Очистить все приёмы пищи за сегодня
     */
    fun clearTodaysMeals() {
        _dailyNutrition.value = _dailyNutrition.value.copy(
            meals = emptyList()
        )
    }
    
    /**
     * Установить целевые калории
     */
    fun setTargetCalories(calories: Int) {
        _dailyNutrition.value = _dailyNutrition.value.copy(
            targetCalories = calories
        )
    }
    
    /**
     * Установить целевые макронутриенты
     */
    fun setTargetMacros(macros: Macros) {
        _dailyNutrition.value = _dailyNutrition.value.copy(
            targetMacros = macros
        )
    }
}