package com.questlife.app.data

import com.questlife.app.models.*
import java.util.UUID
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream

object PredefinedWorkoutPrograms {

    private var context: android.content.Context? = null

    fun init(context: android.content.Context) {
        this.context = context.applicationContext
    }

    // Загрузка всех программ из program_base.json
    fun getAllPredefinedPrograms(): List<WorkoutProgram> {
        return loadProgramsFromJson()
    }
    
    // Загрузка программ из JSON файла в assets
    private fun loadProgramsFromJson(): List<WorkoutProgram> {
        val programs = mutableListOf<WorkoutProgram>()
        
        try {
            // Получаем InputStream из assets через Context
            val ctx = context ?: return emptyList()
            val inputStream = ctx.assets.open("program_base.json")
            
            val jsonString = inputStream.bufferedReader().use { it.readText() }
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val programObj = jsonArray.getJSONObject(i)
                val program = parseProgramFromJson(programObj)
                if (program != null) {
                    programs.add(program)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return programs
    }
    
    // Парсинг программы из JSON объекта
    private fun parseProgramFromJson(obj: JSONObject): WorkoutProgram? {
        return try {
            // Получаем id - может быть String или Int
            val id = when (val rawId = obj.opt("id")) {
                is String -> rawId
                is Int -> "program_$rawId"
                is Long -> "program_$rawId"
                else -> UUID.randomUUID().toString()
            }
            val name = obj.optString("name", obj.optString("title", "Без названия"))
            val description = obj.optString("description", "")
            
            // Определяем difficulty
            val difficultyStr = obj.optString("difficulty", "BEGINNER")
            val goal = obj.optString("goal", obj.optString("target", "Fitness"))
            
            // daysPerWeek может быть в разных полях
            val daysPerWeek = obj.optInt("daysPerWeek", obj.optInt("days_per_week", 3))
            val durationWeeks = obj.optInt("durationWeeks", obj.optInt("duration_weeks", 8))
            
            // Парсим workoutDays или days
            val workouts = mutableListOf<Workout>()
            val workoutDaysArray = if (obj.has("workoutDays")) {
                obj.getJSONArray("workoutDays")
            } else if (obj.has("days")) {
                obj.getJSONArray("days")
            } else {
                JSONArray()
            }
            
            for (i in 0 until workoutDaysArray.length()) {
                val dayObj = workoutDaysArray.getJSONObject(i)
                val workout = parseWorkoutFromJson(dayObj)
                if (workout != null) {
                    workouts.add(workout)
                }
            }
            
            WorkoutProgram(
                id = id,
                name = name,
                description = description,
                goal = goal,
                durationWeeks = if (durationWeeks > 0) durationWeeks else 8,
                daysPerWeek = if (daysPerWeek > 0) daysPerWeek else 3,
                workouts = workouts.ifEmpty { 
                    listOf(Workout(name = "Тренировка", exercises = emptyList(), estimatedDuration = 45)) 
                },
                targetCompletions = 30,
                completedCount = 0
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    // Парсинг тренировки из JSON объекта
    private fun parseWorkoutFromJson(obj: JSONObject): Workout? {
        return try {
            val dayNumber = obj.optInt("dayNumber", obj.optInt("day_number", 0))
            val name = obj.optString("name", "День $dayNumber")
            val description = obj.optString("description", "")
            
            // Парсим упражнения
            val exercises = mutableListOf<WorkoutExercise>()
            val exercisesArray = if (obj.has("exercises")) {
                obj.getJSONArray("exercises")
            } else {
                JSONArray()
            }
            
            for (i in 0 until exercisesArray.length()) {
                val exerciseObj = exercisesArray.getJSONObject(i)
                
                // Сначала пытаемся найти по ID (приоритет)
                val exerciseId = exerciseObj.optString("id", "")
                val exerciseName = exerciseObj.optString("name", "")
                
                val exercise = if (exerciseId.isNotEmpty()) {
                    findExerciseById(exerciseId) ?: findExerciseByName(exerciseName) ?: createFallbackExercise(exerciseName, MuscleGroup.FULL_BODY)
                } else {
                    findExerciseByName(exerciseName) ?: createFallbackExercise(exerciseName, MuscleGroup.FULL_BODY)
                }
                
                val sets = exerciseObj.optInt("sets", 3)
                val reps = when (val r = exerciseObj.opt("reps")) {
                    is Int -> r
                    is String -> r.toIntOrNull() ?: 10
                    else -> 10
                }
                val duration = exerciseObj.optInt("duration", 0)
                
                exercises.add(WorkoutExercise(exercise, sets, reps, 0.0))
            }
            
            Workout(
                name = name,
                description = description,
                exercises = exercises,
                estimatedDuration = 45
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // Вспомогательная функция для поиска упражнения по ID
    fun findExerciseById(id: String): Exercise? {
        return ExerciseDatabase.exercises.find { it.id == id }
    }

    // Вспомогательная функция для поиска упражнения по названию
    fun findExerciseByName(name: String): Exercise? {
        val normalizedName = normalizeExerciseName(name)
        
        // 1. Точное совпадение
        val exactMatch = ExerciseDatabase.exercises.find { 
            normalizeExerciseName(it.name).equals(normalizedName, ignoreCase = true) 
        }
        if (exactMatch != null) return exactMatch
        
        // 2. Частичное совпадение - название из JSON содержится в названии упражнения
        val containsMatch = ExerciseDatabase.exercises.find { 
            normalizeExerciseName(it.name).contains(normalizedName, ignoreCase = true) 
        }
        if (containsMatch != null) return containsMatch
        
        // 3. Обратное частичное совпадение - название упражнения содержится в названии из JSON
        val reverseContainsMatch = ExerciseDatabase.exercises.find { exercise ->
            normalizedName.contains(normalizeExerciseName(exercise.name), ignoreCase = true)
        }
        if (reverseContainsMatch != null) return reverseContainsMatch
        
        return null
    }
    
    // Нормализация названия упражнения для лучшего сравнения
    private fun normalizeExerciseName(name: String): String {
        return name
            .lowercase()
            .replace("ё", "е")
            .replace("-", " ")
            .replace("_", " ")
            .replace("  ", " ")
            .trim()
    }
    
    // Поиск упражнения по ключевым словам для популярных названий
    private fun findExerciseByKeywords(name: String): Exercise? {
        val normalizedName = normalizeExerciseName(name)
        
        val keywordMappings = mapOf(
            "жим лежа" to listOf("жим штанги лёжа", "жим лёжа", "поочерёдный жим лёжа на полу"),
            "жим на наклонной" to listOf("жим гантелей на наклонной", "жим на наклонной скамье"),
            "жим узким хватом" to listOf("жим узким хватом", "жим лежа узким хватом"),
            "жим гантелей" to listOf("жим гантелей", "жим с гантелями"),
            "жим ногами" to listOf("жим ногами", "leg press"),
            "приседания со штангой" to listOf("приседания со штангой", "полный присед со штангой"),
            "приседания" to listOf("приседания", "присед", "сквот"),
            "тяга верхнего блока" to listOf("тяга верхнего блока", "тяга блока к груди"),
            "тяга горизонтального" to listOf("тяга горизонтального блока", "тяга блока сидя", "тяга к поясу"),
            "тяга блока" to listOf("тяга блока", "тяга верхнего блока", "тяга горизонтального блока"),
            "тяга штанги в наклоне" to listOf("тяга штанги в наклоне", "тяга в наклоне"),
            "тяга гантели" to listOf("тяга гантели", "тяга одной гантели"),
            "тяга гантелей" to listOf("тяга гантелей", "тяга двух гантелей"),
            "тяга к подбородку" to listOf("тяга к подбородку", "протяжка"),
            "face pulls" to listOf("face pulls", "тяга к лицу"),
            "армейский жим" to listOf("жим штанги стоя", "жим стоя", "военный жим", "жим армейский"),
            "подъем штанги на бицепс" to listOf("сгибание рук со штангой", "подъем на бицепс", "сгибание рук стоя"),
            "подъем на бицепс" to listOf("подъем на бицепс", "сгибание рук", "бицепс"),
            "концентрированный подъем" to listOf("концентрированный подъем", "концентрированное сгибание"),
            "французский жим" to listOf("французский жим", "разгибание рук лёжа"),
            "разгибание из-за головы" to listOf("разгибание из-за головы", "трицепс из-за головы"),
            "планка" to listOf("планка", "боковая планка"),
            "боковая планка" to listOf("боковая планка", "side plank"),
            "скручивания" to listOf("скручивания", "скручивание", "кранч"),
            "обратные скручивания" to listOf("обратные скручивания", "reverse crunch"),
            "подтягивания" to listOf("подтягивания", "подтягивание"),
            "отжимания" to listOf("отжимания", "отжимание", "пуш-ап"),
            "выпады" to listOf("выпады", "выпад", "сплит-присед"),
            "мертвая тяга" to listOf("мертвая тяга", "становая тяга", "тяга на прямых ногах"),
            "становая тяга" to listOf("становая тяга", "тяга сумо"),
            "румынская тяга" to listOf("румынская тяга", "тяга на прямых ногах"),
            "разведение гантелей" to listOf("разведение гантелей", "разводка", "махи гантелями"),
            "махи" to listOf("махи", "разведение", "raise"),
            "берпи" to listOf("берпи", "burpee"),
            "альпинист" to listOf("альпинист", "mountain climber", "скалолаз"),
            "джампинг джек" to listOf("джампинг джек", "jumping jack", "прыжки"),
            "прыжки" to listOf("прыжки", "запрыгивания", "jumps"),
            "запрыгивания" to listOf("запрыгивания", "прыжки на тумбу", "box jumps"),
            "разгибание ног" to listOf("разгибание ног", "разгибание в тренажёре", "экстензия"),
            "сгибание ног" to listOf("сгибание ног", "сгибание в тренажёре", "флексия"),
            "ягодичный мост" to listOf("ягодичный мост", "мостик", "hip thrust"),
            "подъем на носки" to listOf("подъем на носки", "икры", "calf raise"),
            "гиперэкстензия" to listOf("гиперэкстензия", "гипер"),
            "велосипед" to listOf("велосипед", "air bike"),
            "трастеры" to listOf("трастеры", "thruster"),
            "рывок" to listOf("рывок", "snatch"),
            "взятие" to listOf("взятие на грудь", "clean"),
            "бег" to listOf("бег", "running"),
            "ходьба" to listOf("ходьба", "walking"),
            "вакуум" to listOf("вакуум", "vacuum"),
            "кошка" to listOf("кошка-корова", "cat cow"),
            "голубь" to listOf("голубь", "pigeon"),
            "воин" to listOf("воин", "warrior"),
            "дерево" to listOf("дерево", "tree pose"),
            "лодка" to listOf("лодка", "boat pose"),
            "ребенок" to listOf("поза ребенка", "child pose"),
            "собака" to listOf("птица-собака", "bird dog"),
            "його" to listOf("йога", "yoga"),
            "гребля" to listOf("гребля", "rowing"),
            "эллипс" to listOf("эллипс", "elliptical"),
            "шу" to listOf("сурья намаскар", "sun salutation"),
            "табата" to listOf("табата", "tabata"),
            "кроссфит" to listOf("кроссфит", "crossfit"),
            "круговая" to listOf("круговая тренировка", "circuit"),
            "суперсет" to listOf("суперсет", "superset"),
            "тр" to listOf("тренажёр", "тренажер", "machine"),
            
            // Дополнительные сопоставления для program_base.json
            "бицепс со штангой" to listOf("сгибание рук со штангой", "подъем штанги на бицепс"),
            "боковые махи" to listOf("боковой подъём", "подъём рук в стороны"),
            "броски мяча" to listOf("бросок медбола", "бросок мяча"),
            "выпады назад" to listOf("обратные выпады", "выпады"),
            "жим лежа + тяга блока" to listOf("жим штанги лёжа", "тяга верхнего блока"),
            "жим на наклонной + тяга штанги" to listOf("жим гантелей на наклонной", "тяга штанги в наклоне"),
            "концентрированный подъем на бицепс" to listOf("концентрированное сгибание", "концентрированное сгибание с гантелью"),
            "кошка-корова" to listOf("кошка (растяжка спины)", "кошка"),
            "лицевая тяга" to listOf("тяга к лицу", "face pulls"),
            "махи в наклоне" to listOf("подъём гантелей для задних дельт в наклоне", "разведение рук назад"),
            "махи в стороны" to listOf("подъём рук в стороны", "боковой подъём"),
            "махи вперед" to listOf("подъём гантелей вперёд", "подъём рук вперёд"),
            "молотки с гантелями" to listOf("хаммер-сгибания", "молот"),
            "наклон к ногам" to listOf("наклон вперёд", "растяжка"),
            "отведение ноги в сторону" to listOf("отведение бедра", "подъём ноги в сторону"),
            "повороты корпуса" to listOf("повороты туловища", "русский поворот"),
            "подъем корпуса" to listOf("скручивания", "подъём тела"),
            "подъем на скамье скотта" to listOf("сгибание на скамье Скотта"),
            "подъем штанги стоя" to listOf("жим штанги стоя", "тяга штанги к подбородку"),
            "приседания с выпрыгиванием" to listOf("приседания с прыжком", "прыжковые приседания"),
            "приседания сумо" to listOf("приседания широкой постановкой ног", "плие"),
            "прыжки на тумбу" to listOf("прыжок на тумбу", "боковой прыжок на тумбу"),
            "пуловер" to listOf("пуловер с гантелью", "пуловер со штангой"),
            "разведение в наклоне" to listOf("разведение гантелей в наклоне", "подъём гантелей для задних дельт"),
            "разведение в тренажере" to listOf("разведение рук назад на тренажёре"),
            "разгибание на блоке" to listOf("разгибание рук на блоке вниз", "разгибание трицепса"),
            "разгибания из-за головы" to listOf("разгибание трицепса над головой", "разгибание трицепса с канатом над головой"),
            "рывок гантели" to listOf("рывок", "рывок гири"),
            "рывок гири" to listOf("рывок гири одной рукой", "рывок"),
            "сведение в кроссовере" to listOf("кроссовер для груди", "сведение"),
            "сгибание ног в тренажере" to listOf("сгибание ног лёжа", "сгибание ног сидя"),
            "скрутка лежа" to listOf("скручивания лёжа", "скручивания на полу"),
            "турецкий подъем" to listOf("турецкий подъём с гирей"),
            "тяга т-грифа" to listOf("тяга т-штанги", "тяга т-штанги лёжа", "тяга т-штанги с рукоятью"),
            "тяга блока к поясу" to listOf("тяга нижнего блока сидя", "тяга горизонтального блока"),
            "тяга одной гантели" to listOf("тяга гантели одной рукой"),
            "тяга штанги к поясу" to listOf("тяга штанги в наклоне"),
            "французский жим с гантелью" to listOf("французский жим", "разгибание трицепса с гантелью"),
            "фронтальные приседания" to listOf("фронтальные приседания со штангой", "фронтальный присед"),
            "ягодичный мостик" to listOf("ягодичный мост", "ягодичный мост со штангой"),
            
            // Для оставшихся не найденных упражнений
            "берпи" to listOf("берпи", "burpee"),
            "базовые" to listOf("базовое упражнение"),
            "голубь" to listOf("голубь", "pigeon"),
            "дерево" to listOf("дерево", "tree pose"),
            "запрыгивания" to listOf("запрыгивания", "прыжок на тумбу", "боковой прыжок на тумбу"),
            "йога" to listOf("йога", "yoga"),
            "кошка" to listOf("кошка (растяжка спины)", "cat cow"),
            "кр" to listOf("кроссфит", "crossfit"),
            "круговая" to listOf("круговая тренировка", "circuit"),
            "лодка" to listOf("лодка", "boat pose"),
            "молот" to listOf("хаммер-сгибания", "молот"),
            "плавание" to listOf("плавание", "прогулка"),
            "птица" to listOf("птица-собака", "bird dog"),
            "разведение" to listOf("разведение гантелей", "разведение рук"),
            "разгибание" to listOf("разгибание трицепса", "разгибание рук"),
            "русский твист" to listOf("русский поворот", "повороты туловища"),
            "сгибание рук" to listOf("сгибание рук со штангой", "сгибание рук с гантелями"),
            "спринт" to listOf("спринт", "бег"),
            "стояние" to listOf("баланс", "равновесие"),
            "суперсет" to listOf("суперсет", "super set"),
            "сурья" to listOf("сурья намаскар", "sun salutation"),
            "табата" to listOf("табата", "tabata"),
            "тяга горизонтального" to listOf("тяга горизонтального блока", "тяга блока сидя"),
            "эллипс" to listOf("эллиптический тренажёр", "elliptical"),
            "шаги" to listOf("ходьба", "steps")
        )
        
        for ((shortName, variants) in keywordMappings) {
            if (normalizedName.contains(shortName)) {
                for (variant in variants) {
                    val match = ExerciseDatabase.exercises.find { 
                        normalizeExerciseName(it.name).contains(variant, ignoreCase = true)
                    }
                    if (match != null) return match
                }
            }
        }
        
        return null
    }

    // Fallback упражнение если не найдено в базе
    private fun createFallbackExercise(name: String, muscleGroup: MuscleGroup): Exercise {
        // Маппинг названий упражнений на ID изображений из exercises.json
        val normalizedName = name.lowercase().trim()
        val imageId = when {
            // Русские названия - точные соответствия с exercises.json
            name.contains("Жим лежа", ignoreCase = true) && !name.contains("наклон") && !name.contains("узким") && !name.contains("гантель") -> "Barbell_Bench_Press_-_Medium_Grip"
            name.contains("Тяга верхнего блока", ignoreCase = true) -> "Lat_Pulldown"
            name.contains("Тяга горизонтального", ignoreCase = true) || name.contains("Тяга блока к поясу", ignoreCase = true) -> "Seated_Cable_Row"
            name.contains("Тяга штанги в наклоне", ignoreCase = true) -> "Bent_Over_Barbell_Row"
            name.contains("Тяга гантели", ignoreCase = true) -> "One_Arm_Dumbbell_Row"
            name.contains("Тяга т-грифа", ignoreCase = true) -> "T-Bar_Row"
            name.contains("Отжиман", ignoreCase = true) -> "Push_Up"
            name.contains("Приседания со штангой", ignoreCase = true) -> "Barbell_Squat"
            name.contains("Присед", ignoreCase = true) && !name.contains("фронтальн") && !name.contains("сумо") && !name.contains("гантель") -> "Bodyweight_Squat"
            name.contains("Подтягиван", ignoreCase = true) -> "Pull-Up"
            name.contains("Планк", ignoreCase = true) && !name.contains("боков") -> "Plank"
            name.contains("Боковая планка", ignoreCase = true) -> "Side_Plank"
            name.contains("Выпад", ignoreCase = true) -> "Bodyweight_Walking_Lunge"
            name.contains("Скручиван", ignoreCase = true) && !name.contains("обратн") -> "Cable_Crunch"
            name.contains("Обратные скручивания", ignoreCase = true) -> "Reverse_Crunch"
            name.contains("Берпи", ignoreCase = true) -> "Burpee"
            name.contains("Мост", ignoreCase = true) || name.contains("Ягодичный мост", ignoreCase = true) -> "Barbell_Glute_Bridge"
            name.contains("Становая тяга", ignoreCase = true) -> "Barbell_Deadlift"
            name.contains("Мертвая тяга", ignoreCase = true) || name.contains("Румынская тяга", ignoreCase = true) -> "Romanian_Deadlift"
            name.contains("Жим стоя", ignoreCase = true) || name.contains("Армейский жим", ignoreCase = true) || name.contains("Жим штанги стоя", ignoreCase = true) -> "Barbell_Shoulder_Press"
            name.contains("Жим гантелей", ignoreCase = true) && name.contains("наклон", ignoreCase = true) -> "Incline_Dumbbell_Press"
            name.contains("Жим гантелей", ignoreCase = true) -> "Dumbbell_Press"
            name.contains("Жим узким хватом", ignoreCase = true) -> "Close-Grip_Barbell_Bench_Press"
            name.contains("Жим на наклонной", ignoreCase = true) -> "Barbell_Incline_Bench_Press_-_Medium_Grip"
            name.contains("Подъем штанги на бицепс", ignoreCase = true) || name.contains("Сгибание рук со штангой", ignoreCase = true) -> "Barbell_Curl"
            name.contains("Подъем гантели на бицепс", ignoreCase = true) || name.contains("Сгибание рук с гантелями", ignoreCase = true) -> "Alternating_Dumbbell_Curl"
            name.contains("Молотки", ignoreCase = true) -> "Hammer_Curls"
            name.contains("Концентрированный подъем", ignoreCase = true) -> "Concentration_Curls"
            name.contains("Французский жим", ignoreCase = true) -> "Skullcrushers"
            name.contains("Разгибание на блоке", ignoreCase = true) || name.contains("Разгибание трицепса", ignoreCase = true) -> "Cable_Tricep_Pushdown"
            name.contains("Разгибание из-за головы", ignoreCase = true) -> "Overhead_Tricep_Ext"
            name.contains("Махи в стороны", ignoreCase = true) || name.contains("Подъем рук в стороны", ignoreCase = true) -> "Lateral_Raises"
            name.contains("Махи вперед", ignoreCase = true) -> "Front_Raises"
            name.contains("Махи в наклоне", ignoreCase = true) || name.contains("Разведение в наклоне", ignoreCase = true) -> "Bent_Over_Lateral_Raise"
            name.contains("Разведение гантелей", ignoreCase = true) || name.contains("Разводка", ignoreCase = true) -> "Dumbbell_Flyes"
            name.contains("Сведение в кроссовере", ignoreCase = true) -> "Cable_Crossover"
            name.contains("Пуловер", ignoreCase = true) -> "Dumbbell_Pullover"
            name.contains("Тяга к подбородку", ignoreCase = true) || name.contains("Протяжка", ignoreCase = true) -> "Close-Grip_Upright_Row"
            name.contains("Face pull", ignoreCase = true) || name.contains("Тяга к лицу", ignoreCase = true) -> "Face_Pulls"
            name.contains("Шраги", ignoreCase = true) -> "Barbell_Shrug"
            name.contains("Разгибание ног", ignoreCase = true) -> "Leg_Extensions"
            name.contains("Сгибание ног", ignoreCase = true) -> "Leg_Curls"
            name.contains("Жим ногами", ignoreCase = true) -> "Leg_Press"
            name.contains("Подъем на носки", ignoreCase = true) || name.contains("Икры", ignoreCase = true) -> "Calf_Raises"
            name.contains("Гиперэкстензия", ignoreCase = true) -> "Hyperextensions"
            name.contains("Запрыгивания", ignoreCase = true) || name.contains("Прыжок на тумбу", ignoreCase = true) -> "Box_Jumps"
            name.contains("Альпинист", ignoreCase = true) || name.contains("Скалолаз", ignoreCase = true) -> "Mountain_Climbers"
            name.contains("Джампинг джек", ignoreCase = true) -> "Jumping_Jacks"
            name.contains("Бег", ignoreCase = true) -> "Running"
            name.contains("Велосипед", ignoreCase = true) && name.contains("пресс", ignoreCase = true) -> "Air_Bike"
            name.contains("Вакуум", ignoreCase = true) -> "Vacuum"
            name.contains("Кобра", ignoreCase = true) -> "Cobra_Stretch"
            name.contains("Голубь", ignoreCase = true) -> "One-Legged_King_Pigeon_Pose"
            name.contains("Кошка", ignoreCase = true) -> "Cat-Cow"
            name.contains("Ребенок", ignoreCase = true) || name.contains("Поза ребенка", ignoreCase = true) -> "Child's_Pose"
            name.contains("Воин", ignoreCase = true) -> "Warrior_I_Pose"
            name.contains("Дерево", ignoreCase = true) && name.contains("поза", ignoreCase = true) -> "Tree_Pose"
            name.contains("Лодка", ignoreCase = true) && name.contains("поза", ignoreCase = true) -> "Boat_Pose"
            name.contains("Собака мордой вниз", ignoreCase = true) || name.contains("Собака", ignoreCase = true) && name.contains("вниз", ignoreCase = true) -> "Downward_Dog"
            name.contains("Бабочка", ignoreCase = true) -> "Butterfly_Stretch"
            
            // Английские названия - основные упражнения
            normalizedName.contains("bench press") && !normalizedName.contains("incline") && !normalizedName.contains("decline") && !normalizedName.contains("dumbbell") -> "Barbell_Bench_Press_-_Medium_Grip"
            normalizedName.contains("lat pulldown") -> "Lat_Pulldown"
            normalizedName.contains("cable row") || normalizedName.contains("seated row") -> "Seated_Cable_Row"
            normalizedName.contains("barbell row") && normalizedName.contains("bent over") -> "Bent_Over_Barbell_Row"
            normalizedName.contains("dumbbell row") || normalizedName.contains("one arm row") -> "One_Arm_Dumbbell_Row"
            normalizedName.contains("push-up") || normalizedName.contains("push up") -> "Push_Up"
            normalizedName.contains("squat") && !normalizedName.contains("front") && !normalizedName.contains("goblet") && !normalizedName.contains("sumo") -> "Barbell_Squat"
            normalizedName.contains("pull-up") || normalizedName.contains("pull up") || normalizedName.contains("chin-up") -> "Pull-Up"
            normalizedName.contains("plank") && !normalizedName.contains("side") -> "Plank"
            normalizedName.contains("side plank") -> "Side_Plank"
            normalizedName.contains("lunge") -> "Bodyweight_Walking_Lunge"
            normalizedName.contains("crunch") && !normalizedName.contains("reverse") -> "Cable_Crunch"
            normalizedName.contains("reverse crunch") -> "Reverse_Crunch"
            normalizedName.contains("burpee") -> "Burpee"
            normalizedName.contains("glute bridge") || normalizedName.contains("hip thrust") -> "Barbell_Glute_Bridge"
            normalizedName.contains("deadlift") && !normalizedName.contains("romanian") -> "Barbell_Deadlift"
            normalizedName.contains("romanian deadlift") -> "Romanian_Deadlift"
            normalizedName.contains("shoulder press") || normalizedName.contains("overhead press") || normalizedName.contains("military press") -> "Barbell_Shoulder_Press"
            normalizedName.contains("incline dumbbell press") -> "Incline_Dumbbell_Press"
            normalizedName.contains("dumbbell press") -> "Dumbbell_Press"
            normalizedName.contains("close grip bench") -> "Close-Grip_Barbell_Bench_Press"
            normalizedName.contains("incline press") -> "Barbell_Incline_Bench_Press_-_Medium_Grip"
            normalizedName.contains("barbell curl") -> "Barbell_Curl"
            normalizedName.contains("dumbbell curl") -> "Alternating_Dumbbell_Curl"
            normalizedName.contains("hammer curl") -> "Hammer_Curls"
            normalizedName.contains("concentration curl") -> "Concentration_Curls"
            normalizedName.contains("skull crusher") -> "Skullcrushers"
            normalizedName.contains("tricep pushdown") -> "Cable_Tricep_Pushdown"
            normalizedName.contains("overhead tricep") -> "Overhead_Tricep_Ext"
            normalizedName.contains("lateral raise") -> "Lateral_Raises"
            normalizedName.contains("front raise") -> "Front_Raises"
            normalizedName.contains("bent over lateral") -> "Bent_Over_Lateral_Raise"
            normalizedName.contains("dumbbell fly") -> "Dumbbell_Flyes"
            normalizedName.contains("cable crossover") -> "Cable_Crossover"
            normalizedName.contains("pullover") -> "Dumbbell_Pullover"
            normalizedName.contains("upright row") -> "Close-Grip_Upright_Row"
            normalizedName.contains("face pull") -> "Face_Pulls"
            normalizedName.contains("shrug") -> "Barbell_Shrug"
            normalizedName.contains("leg extension") -> "Leg_Extensions"
            normalizedName.contains("leg curl") -> "Leg_Curls"
            normalizedName.contains("leg press") -> "Leg_Press"
            normalizedName.contains("calf raise") -> "Calf_Raises"
            normalizedName.contains("hyperextension") -> "Hyperextensions"
            normalizedName.contains("box jump") -> "Box_Jumps"
            normalizedName.contains("mountain climber") -> "Mountain_Climbers"
            normalizedName.contains("jumping jack") -> "Jumping_Jacks"
            normalizedName.contains("running") -> "Running"
            normalizedName.contains("air bike") || normalizedName.contains("bicycle crunch") -> "Air_Bike"
            normalizedName.contains("cobra") -> "Cobra_Stretch"
            normalizedName.contains("pigeon") -> "One-Legged_King_Pigeon_Pose"
            normalizedName.contains("cat-cow") || normalizedName.contains("cat cow") -> "Cat-Cow"
            normalizedName.contains("child pose") -> "Child's_Pose"
            normalizedName.contains("warrior") -> "Warrior_I_Pose"
            normalizedName.contains("tree pose") -> "Tree_Pose"
            normalizedName.contains("boat pose") -> "Boat_Pose"
            normalizedName.contains("downward dog") -> "Downward_Dog"
            normalizedName.contains("butterfly") -> "Butterfly_Stretch"
            normalizedName.contains("front lever") -> "Front_Lever_Progression"
            normalizedName.contains("handstand") -> "Handstand_Push-Up"
            normalizedName.contains("planche") -> "Planche_Progression"
            normalizedName.contains("dragon flag") -> "Dragon_Flag"
            normalizedName.contains("human flag") -> "Human_Flag_Progression"
            normalizedName.contains("inverted row") -> "Inverted_Rows"
            normalizedName.contains("australian pull-up") -> "Inverted_Rows"
            normalizedName.contains("arch hang") -> "Arch_Hangs"
            normalizedName.contains("active hang") || normalizedName.contains("dead hang") -> "Active_Hang"
            normalizedName.contains("band pull-apart") -> "Band_Pull-Aparts"
            normalizedName.contains("dislocate") || normalizedName.contains("pass-through") -> "Shoulder_Dislocates"
            normalizedName.contains("world's greatest stretch") -> "World's_Greatest_Stretch"
            normalizedName.contains("thoracic rotation") -> "Thoracic_Rotations"
            normalizedName.contains("open book") -> "Open_Books"
            normalizedName.contains("thread the needle") -> "Thread_the_Needle"
            normalizedName.contains("happy baby") -> "Happy_Baby"
            normalizedName.contains("spinal twist") -> "Spinal_Twist"
            normalizedName.contains("seated forward fold") || normalizedName.contains("seated forward bend") -> "Seated_Forward_Bend"
            normalizedName.contains("standing forward fold") || normalizedName.contains("standing forward bend") -> "Standing_Forward_Bend"
            normalizedName.contains("runner's lunge") -> "Runner's_Lunge"
            normalizedName.contains("frog stretch") -> "Frog_Stretch"
            normalizedName.contains("90/90") || normalizedName.contains("ninety ninety") -> "90_90_Hamstring"
            normalizedName.contains("piriformis stretch") -> "Piriformis_Stretch"
            normalizedName.contains("figure four stretch") -> "Figure_Four_Stretch"
            normalizedName.contains("it band stretch") -> "IT_Band_Stretch"
            normalizedName.contains("quad stretch") -> "Standing_Quad_Stretch"
            normalizedName.contains("hamstring stretch") -> "Standing_Hamstring_Stretch"
            normalizedName.contains("calf stretch") -> "Standing_Calf_Stretch"
            normalizedName.contains("hip flexor stretch") -> "HipFlexor_Stretch"
            normalizedName.contains("pec stretch") || normalizedName.contains("chest stretch") -> "Doorway_Chest_Stretch"
            normalizedName.contains("triceps stretch") -> "Triceps_Stretch"
            normalizedName.contains("biceps stretch") -> "Biceps_Stretch"
            normalizedName.contains("shoulder stretch") || normalizedName.contains("cross-body stretch") -> "Cross-Body_Shoulder_Stretch"
            normalizedName.contains("neck stretch") || normalizedName.contains("neck rotation") -> "Neck_Rotations"
            normalizedName.contains("wrist stretch") || normalizedName.contains("wrist curl") -> "Wrist_Extensor_Curls"
            normalizedName.contains("ankle circle") -> "Ankle_Circles"
            normalizedName.contains("arm circle") -> "Arm_Circles"
            normalizedName.contains("shoulder roll") -> "Shoulder_Rolls"
            normalizedName.contains("jump rope") || normalizedName.contains("skipping rope") -> "Jump_Rope"
            normalizedName.contains("running") || normalizedName.contains("run") -> "Running"
            normalizedName.contains("walking") || normalizedName.contains("walk") && !normalizedName.contains("farmer") -> "Brisk_Walking"
            normalizedName.contains("cycling") || normalizedName.contains("bike") -> "Cycling"
            normalizedName.contains("swimming") || normalizedName.contains("swim") -> "Swimming"
            normalizedName.contains("rowing") || normalizedName.contains("row") && !normalizedName.contains("barbell") && !normalizedName.contains("cable") && !normalizedName.contains("dumbbell") -> "Rowing_Ergometer"
            normalizedName.contains("elliptical") -> "Elliptical_Machine"
            normalizedName.contains("stair climbing") || normalizedName.contains("stair master") -> "Stair_Climbing"
            normalizedName.contains("treadmill") -> "Treadmill_Running"
            
            else -> "3_4_Sit-Up" // Дефолтное изображение
        }
        
        return Exercise(
            id = UUID.randomUUID().toString(),
            name = name,
            description = "Базовое упражнение: $name",
            category = ExerciseCategory.STRENGTH,
            equipment = ExerciseEquipment.NONE,
            difficulty = ExerciseDifficulty.BEGINNER,
            muscleGroups = listOf(muscleGroup),
            caloriesPerMinute = 5.0,
            instructions = listOf("Выполняйте упражнение правильно", "Следите за техникой", "Дышите равномерно"),
            imageUrl = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/$imageId/0.jpg",
            imageUrl2 = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/exercises/$imageId/1.jpg"
        )
    }

    // === ДОСТИЖЕНИЯ ДЛЯ ПРОГРАММ ===
    data class ProgramAchievement(
        val programId: String,
        val programName: String,
        val achievementTitle: String,
        val achievementDescription: String,
        val icon: String,
        val type: AchievementType
    )

    fun getAchievementsForProgram(programId: String, programName: String): List<ProgramAchievement> {
        return listOf(
            ProgramAchievement(
                programId = programId,
                programName = programName,
                achievementTitle = "Дебют: $programName",
                achievementDescription = "Первое завершение программы \"$programName\"",
                icon = "🏆",
                type = AchievementType.FIRST_WORKOUT
            ),
            ProgramAchievement(
                programId = programId,
                programName = programName,
                achievementTitle = "Мастер: $programName",
                achievementDescription = "Завершить программу \"$programName\" 10 раз",
                icon = "👑",
                type = AchievementType.CONSISTENCY_CHAMPION
            ),
            ProgramAchievement(
                programId = programId,
                programName = programName,
                achievementTitle = "Легенда: $programName",
                achievementDescription = "Завершить программу \"$programName\" 50 раз",
                icon = "⭐",
                type = AchievementType.VOLUME_MILESTONE
            )
        )
    }

    // Получить все возможные достижения
    fun getAllPossibleAchievements(): List<ProgramAchievement> {
        val allAchievements = mutableListOf<ProgramAchievement>()
        getAllPredefinedPrograms().forEach { program ->
            allAchievements.addAll(getAchievementsForProgram(program.id, program.name))
        }
        return allAchievements
    }
}
