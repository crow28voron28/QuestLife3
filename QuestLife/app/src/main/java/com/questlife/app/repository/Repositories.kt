package com.questlife.app.repository

import android.content.Context
import com.questlife.app.api.TheMealDbApi
import com.questlife.app.data.ExerciseDatabase
import com.questlife.app.data.MegaQuestDatabase
import com.questlife.app.data.RecipeLink
import com.questlife.app.data.RussianRecipesDatabase
import com.questlife.app.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// ==================== НУТРИЦИЯ ====================
class NutritionRepository {

    suspend fun searchFoods(query: String): List<Food> {
        // Пока заглушка — можно расширить позже
        return emptyList()
    }

    suspend fun findByBarcode(barcode: String): Food? = null
}

// ==================== УПРАЖНЕНИЯ ====================
class ExerciseRepository {

    suspend fun getAllExercises(): List<Exercise> {
        return ExerciseDatabase.exercises
    }

    suspend fun searchExercises(query: String): List<Exercise> {
        return ExerciseDatabase.searchExercises(query)
    }

    suspend fun getWorkoutPlans(): List<WorkoutProgram> {
        return emptyList() // пока нет реальных программ
    }
}

// ==================== КВЕСТЫ ====================
class QuestRepository {

    fun generateDailyQuests(count: Int = 5): Flow<List<HealthQuest>> = flow {
        emit(MegaQuestDatabase.generateDailyQuests(count))
    }

    fun generateWeeklyQuests(count: Int = 3): Flow<List<HealthQuest>> = flow {
        emit(MegaQuestDatabase.generateWeeklyQuests())
    }

    fun generateMonthlyQuests(count: Int = 2): Flow<List<HealthQuest>> = flow {
        emit(emptyList()) // пока не реализовано
    }

    suspend fun updateQuestProgress(quest: HealthQuest): HealthQuest {
        // Простая заглушка — возвращаем тот же квест
        return quest
    }

    fun getQuestsStatistics(): Map<String, Int> {
        return mapOf(
            "daily" to 5,
            "weekly" to 3,
            "completed" to 2
        )
    }
}

// ==================== РЕЦЕПТЫ ====================
class RecipeRepository(
    private val context: Context,
    private val theMealDbApi: TheMealDbApi? = null
) {
    
    private val russianRecipesDb by lazy { RussianRecipesDatabase.getInstance(context) }
    
    /**
     * Поиск рецептов в русской базе данных по названию
     */
    suspend fun searchRussianRecipes(query: String): List<RecipeLink> {
        return try {
            russianRecipesDb.searchRecipesByQuery(query)
        } catch (e: Exception) {
            android.util.Log.e("RecipeRepository", "Ошибка поиска в русской базе: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Получить случайные рецепты из русской базы
     */
    suspend fun getRandomRussianRecipes(limit: Int = 10): List<RecipeLink> {
        return try {
            russianRecipesDb.getRandomRecipes(limit)
        } catch (e: Exception) {
            android.util.Log.e("RecipeRepository", "Ошибка получения случайных рецептов: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Получить количество рецептов в русской базе
     */
    fun getRussianRecipeCount(): Int {
        return try {
            russianRecipesDb.getRecipeCount()
        } catch (e: Exception) {
            0
        }
    }
    
    /**
     * Поиск рецептов в TheMealDB API по названию
     */
    suspend fun searchTheMealDb(query: String): List<Meal> {
        return try {
            if (theMealDbApi == null) {
                android.util.Log.w("RecipeRepository", "TheMealDbApi не инициализирован")
                return emptyList()
            }
            val response = theMealDbApi.searchMeals(query)
            if (response.isSuccessful) {
                response.body()?.meals ?: emptyList()
            } else {
                android.util.Log.e("RecipeRepository", "Ошибка API: ${response.code()}")
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecipeRepository", "Ошибка сети: ${e.message}")
            emptyList()
        }
    }
    
    /**
     * Получить случайный рецепт из TheMealDB
     */
    suspend fun getRandomTheMealDbRecipe(): List<Meal> {
        return try {
            if (theMealDbApi == null) {
                return emptyList()
            }
            val response = theMealDbApi.getRandomMeals()
            if (response.isSuccessful) {
                response.body()?.meals ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            android.util.Log.e("RecipeRepository", "Ошибка сети: ${e.message}")
            emptyList()
        }
    }
}

// ==================== РЕПОЗИТОРИИ ====================

object Repositories {
    lateinit var appContext: Context
        private set
    
    fun initialize(context: Context) {
        appContext = context.applicationContext
    }
    
    val nutritionRepository = NutritionRepository()
    val exerciseRepository = ExerciseRepository()
    val questRepository = QuestRepository()
    
    fun getRecipeRepository(theMealDbApi: TheMealDbApi? = null): RecipeRepository {
        return RecipeRepository(appContext, theMealDbApi)
    }
}

// ==================== VIEWMODELS (простые) ====================

class NutritionViewModel {
    // Можно расширить позже
}

class ExerciseViewModel(private val repository: ExerciseRepository) {
    suspend fun loadExercises(): List<Exercise> = repository.getAllExercises()
    suspend fun searchExercises(query: String): List<Exercise> = repository.searchExercises(query)
}

class QuestViewModel(private val repository: QuestRepository) {
    fun loadDailyQuests(count: Int = 5): Flow<List<HealthQuest>> = repository.generateDailyQuests(count)
    fun loadWeeklyQuests(count: Int = 3): Flow<List<HealthQuest>> = repository.generateWeeklyQuests(count)
    fun loadMonthlyQuests(count: Int = 2): Flow<List<HealthQuest>> = repository.generateMonthlyQuests(count)

    suspend fun completeQuestStep(quest: HealthQuest): HealthQuest {
        return repository.updateQuestProgress(quest)
    }
}