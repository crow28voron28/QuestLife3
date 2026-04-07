package com.questlife.app.data

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.questlife.app.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.util.UUID

object ExerciseDatabase {

    private var baseExercises: List<Exercise> = emptyList()
    private val customExercises = mutableListOf<Exercise>()

    val exercises: List<Exercise>
        get() = baseExercises + customExercises

    suspend fun loadFromAssets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.assets.open("exercises.json")
                val reader = InputStreamReader(inputStream)

                val listType = object : TypeToken<List<ExerciseJson>>() {}.type
                val jsonList: List<ExerciseJson> = Gson().fromJson(reader, listType)

                baseExercises = jsonList.map { it.toExercise() }

                reader.close()
                inputStream.close()

                Log.i("ExerciseDB", "✅ Загружено ${baseExercises.size} упражнений (с переводом)")
            } catch (e: Exception) {
                Log.e("ExerciseDB", "❌ Ошибка загрузки JSON", e)
                baseExercises = getFallbackExercises()
            }
        }
    }

    fun addCustomExercise(exercise: Exercise) {
        customExercises.add(exercise)
    }

    fun searchExercises(query: String): List<Exercise> {
        if (query.isBlank()) return exercises
        val q = query.lowercase().trim()
        return exercises.filter {
            it.name.lowercase().contains(q) || 
            it.description.lowercase().contains(q) ||
            it.muscleGroups.any { m -> m.name.lowercase().contains(q) }
        }
    }

    fun getBodyweightExercises(): List<Exercise> =
        exercises.filter { it.equipment == ExerciseEquipment.NONE }

    private fun getFallbackExercises(): List<Exercise> = listOf(
        Exercise(
            id = UUID.randomUUID().toString(),
            name = "Отжимания от пола",
            description = "Классические отжимания от пола",
            category = ExerciseCategory.STRENGTH,
            equipment = ExerciseEquipment.NONE,
            difficulty = ExerciseDifficulty.BEGINNER,
            muscleGroups = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS),
            caloriesPerMinute = 7.0,
            instructions = listOf("Примите упор лежа", "Опуститесь вниз", "Вернитесь вверх"),
            imageUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Push_Up/0.jpg",
            imageUrl2 = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/Push_Up/1.jpg"
        )
    )
}

// ==================== JSON МОДЕЛЬ ====================
data class ExerciseJson(
    val name: String?,
    val instructions: List<String>?,
    val equipment: String?,
    val primaryMuscles: List<String>?,
    val secondaryMuscles: List<String>?,
    val images: List<String>?,
    val id: String?
) {
    fun toExercise(): Exercise {
        // Формируем URL для изображений
        // Приоритет: используем пути из JSON, если они есть и валидны, иначе генерируем из ID
        val baseRepoUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/"
        
        var firstImage = ""
        var secondImage = ""
        
        // Всегда генерируем URL на основе ID как основной источник
        // Это обеспечивает корректное отображение для всех упражнений
        if (id != null) {
            firstImage = "$baseRepoUrl$id/0.jpg"
            secondImage = "$baseRepoUrl$id/1.jpg"
        }
        
        // Если в JSON есть изображения, используем их (они могут быть полными URL или путями)
        if (!images.isNullOrEmpty() && images.size >= 1) {
            // Проверяем, не пустой ли путь
            if (images[0].isNotEmpty()) {
                firstImage = if (images[0].startsWith("http://") || images[0].startsWith("https://")) {
                    images[0]
                } else {
                    "$baseRepoUrl${images[0]}"
                }
            }
            
            if (images.size >= 2 && images[1].isNotEmpty()) {
                secondImage = if (images[1].startsWith("http://") || images[1].startsWith("https://")) {
                    images[1]
                } else {
                    "$baseRepoUrl${images[1]}"
                }
            }
        }

        // Собираем все группы мышц (primary + secondary)
        val allMuscles = mutableListOf<String>()
        primaryMuscles?.let { allMuscles.addAll(it) }
        secondaryMuscles?.let { allMuscles.addAll(it) }

        return Exercise(
            id = id ?: UUID.randomUUID().toString(),
            name = translateName(name.orEmpty()),
            description = translateDescription(instructions?.firstOrNull() ?: ""),
            category = ExerciseCategory.STRENGTH,
            equipment = when (equipment?.lowercase()) {
                "body weight", "none", null, "", "только тело" -> ExerciseEquipment.NONE
                "dumbbell", "dumbbells", "гантели" -> ExerciseEquipment.DUMBBELL
                "barbell", "штанга" -> ExerciseEquipment.BARBELL
                "machine", "тренажёр", "тренажер" -> ExerciseEquipment.MACHINE
                "cable", "блок" -> ExerciseEquipment.CABLE
                "kettlebell", "гиря" -> ExerciseEquipment.KETTLEBELL
                else -> ExerciseEquipment.NONE
            },
            difficulty = ExerciseDifficulty.INTERMEDIATE,
            muscleGroups = allMuscles.mapNotNull { mapMuscle(it) }.ifEmpty { listOf(MuscleGroup.FULL_BODY) },
            caloriesPerMinute = 7.0,
            instructions = instructions?.map { translateInstruction(it) } ?: emptyList(),
            imageUrl = firstImage,
            imageUrl2 = secondImage
        )
    }

    // ==================== ПРОСТОЙ ПЕРЕВОД ====================
    private fun translateName(name: String): String {
        val lowerName = name.lowercase()
        return when {
            lowerName.contains("90/90 hamstring") -> "90/90 Растяжка подколенных сухожилий"
            lowerName.contains("3/4 sit-up") -> "Подъём туловища 3/4"
            lowerName.contains("ab roller") -> "Ролик для пресса"
            lowerName.contains("alternating dumbbell curl") -> "Попеременные сгибания рук с гантелями"
            lowerName.contains("barbell bench press") -> "Жим штанги лёжа"
            lowerName.contains("barbell squat") -> "Приседания со штангой"
            lowerName.contains("burpee") -> "Берпи"
            lowerName.contains("deadlift") -> "Мёртвая тяга"
            lowerName.contains("pull up") -> "Подтягивания"
            lowerName.contains("push up") -> "Отжимания"
            else -> {
                // Если имя уже на русском (содержит кириллицу), оставляем как есть
                if (name.any { it in 'а'..'я' || it in 'А'..'Я' || it == 'ё' || it == 'Ё' }) {
                    name
                } else {
                    // Для остальных английских назаний пытаемся перевести по ключевым словам
                    translateEnglishName(name)
                }
            }
        }
    }
    
    private fun translateEnglishName(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("curl") && lower.contains("bicep") -> "Сгибание рук на бицепс"
            lower.contains("curl") && lower.contains("hammer") -> "Молотки на бицепс"
            lower.contains("curl") -> "Сгибание рук"
            lower.contains("press") && lower.contains("bench") -> "Жим лёжа"
            lower.contains("press") && lower.contains("shoulder") -> "Жим плечами"
            lower.contains("press") -> "Жим"
            lower.contains("squat") -> "Приседания"
            lower.contains("lunge") -> "Выпады"
            lower.contains("row") -> "Тяга"
            lower.contains("fly") -> "Разводка"
            lower.contains("raise") -> "Подъёмы"
            lower.contains("extension") -> "Разгибания"
            lower.contains("flexion") -> "Сгибания"
            lower.contains("stretch") -> "Растяжка"
            lower.contains("plank") -> "Планка"
            lower.contains("crunch") -> "Скручивания"
            lower.contains("leg") -> "Упражнение на ноги"
            lower.contains("arm") -> "Упражнение на руки"
            lower.contains("back") -> "Упражнение на спину"
            lower.contains("chest") -> "Упражнение на грудь"
            else -> name // оставляем оригинал если нет перевода
        }.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun translateDescription(desc: String): String = when {
        desc.contains("Lie on your back") -> "Лягте на спину, одна нога вытянута прямо."
        desc.contains("Sit-Up") -> "Подъём туловища из положения лёжа"
        else -> desc
    }

    private fun translateInstruction(instruction: String): String = instruction
        .replace("Lie on your back", "Лягте на спину")
        .replace("with one leg", "одна нога")
        .replace("extended straight out", "вытянута прямо")
        .replace("Sit up", "Поднимите туловище")

    private fun mapMuscle(muscle: String): MuscleGroup? = when (muscle.lowercase().trim()) {
        // English
        "chest" -> MuscleGroup.CHEST
        "biceps", "бицепсы" -> MuscleGroup.BICEPS
        "triceps", "трицепсы" -> MuscleGroup.TRICEPS
        "shoulders" -> MuscleGroup.SHOULDERS
        "back", "lats", "широчайшие", "середина спины", "средняя часть спины" -> MuscleGroup.BACK
        "quads", "quadriceps" -> MuscleGroup.QUADRICEPS
        "hamstrings" -> MuscleGroup.HAMSTRINGS
        "glutes" -> MuscleGroup.GLUTES
        "abs" -> MuscleGroup.ABS
        "calves" -> MuscleGroup.CALVES
        "forearms" -> MuscleGroup.FOREARMS
        "full body" -> MuscleGroup.FULL_BODY
        // Russian translations
        "грудь", "грудные мышцы" -> MuscleGroup.CHEST
        "бицепс" -> MuscleGroup.BICEPS
        "трицепс" -> MuscleGroup.TRICEPS
        "плечи", "дельты" -> MuscleGroup.SHOULDERS
        "квадрицепсы", "квадрицепс", "передняя поверхность бедра" -> MuscleGroup.QUADRICEPS
        "бицепс бедра", "бицепсы бедра", "задняя поверхность бедра" -> MuscleGroup.HAMSTRINGS
        "ягодицы", "глаuteus" -> MuscleGroup.GLUTES
        "пресс", "абдоминальные", "живот" -> MuscleGroup.ABS
        "икры", "голень" -> MuscleGroup.CALVES
        "предплечья" -> MuscleGroup.FOREARMS
        "все тело", "полное тело" -> MuscleGroup.FULL_BODY
        "поясница", "трапеции", "шея" -> MuscleGroup.BACK
        "отводящие", "отводящие мышцы", "приводящие", "приводящие мышцы" -> MuscleGroup.QUADRICEPS
        else -> null
    }
}