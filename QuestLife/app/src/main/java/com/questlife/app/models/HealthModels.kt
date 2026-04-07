package com.questlife.app.models

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

// ============= МАКРОНУТРИЕНТЫ И ПИТАНИЕ =============

data class Macros(
    val proteins: Double = 0.0,
    val fats: Double = 0.0,
    val carbohydrates: Double = 0.0
) {
    val calories: Double get() = (proteins * 4) + (fats * 9) + (carbohydrates * 4)
}

data class Food(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val macros: Macros = Macros(),
    val servingSize: Double = 100.0,
    val servingUnit: String = "г",
    val category: FoodCategory = FoodCategory.OTHER,
    val isCustom: Boolean = false
)

enum class FoodCategory {
    BREAKFAST, LUNCH, DINNER, SNACK, PROTEIN, VEGETABLES, FRUITS, GRAINS, DAIRY, BEVERAGES, SWEETS, OTHER
}

enum class MealType(val displayName: String) {
    BREAKFAST("Завтрак"),
    LUNCH("Обед"),
    DINNER("Ужин"),
    SNACK("Перекус")
}

data class MealEntry(
    val id: String = UUID.randomUUID().toString(),
    val food: Food,
    val amount: Double = 100.0,
    val amountUnit: String = "г",
    val mealType: MealType,
    val date: LocalDate = LocalDate.now()
)

data class DailyNutrition(
    val targetCalories: Int = 2000,
    val targetMacros: Macros = Macros(150.0, 67.0, 250.0),
    val meals: List<MealEntry> = emptyList()
) {
    val totalCalories: Int
        get() = meals.sumOf { (it.food.macros.calories * it.amount / it.food.servingSize).toInt() }

    val totalMacros: Macros
        get() = Macros(
            proteins = meals.sumOf { (it.food.macros.proteins * it.amount / it.food.servingSize) },
            fats = meals.sumOf { (it.food.macros.fats * it.amount / it.food.servingSize) },
            carbohydrates = meals.sumOf { (it.food.macros.carbohydrates * it.amount / it.food.servingSize) }
        )
}

// ============= ФИТНЕС =============

data class Exercise(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val category: ExerciseCategory,
    val equipment: ExerciseEquipment = ExerciseEquipment.NONE,
    val difficulty: ExerciseDifficulty = ExerciseDifficulty.BEGINNER,
    val muscleGroups: List<MuscleGroup> = emptyList(),
    val caloriesPerMinute: Double = 5.0,
    val instructions: List<String> = emptyList(),
    val imageUrl: String = "",
    val imageUrl2: String = ""  // Второе фото упражнения
)

enum class ExerciseCategory { CARDIO, STRENGTH, FLEXIBILITY, BALANCE, YOGA, OTHER }

enum class ExerciseEquipment {
    NONE, DUMBBELL, BARBELL, KETTLEBELL, MACHINE, CABLE, BODYWEIGHT, OTHER
}

enum class ExerciseDifficulty { BEGINNER, INTERMEDIATE, ADVANCED, EXPERT }

enum class MuscleGroup {
    CHEST, BACK, SHOULDERS, BICEPS, TRICEPS, FOREARMS,
    ABS, GLUTES, QUADRICEPS, HAMSTRINGS, CALVES, FULL_BODY
}

// Данные о выполнении упражнения в рамках завершенной тренировки
data class CompletedExercise(
    val exerciseId: String,
    val exerciseName: String,
    val sets: Int = 0,
    val reps: Int = 0,
    val weight: Double = 0.0,
    val bestReps: Int = 0,
    val bestWeight: Double = 0.0,
    val totalVolume: Double = 0.0 // тоннаж подхода: вес * повторы * подходы
)

// Данные о завершенной тренировке
data class CompletedWorkout(
    val id: String = UUID.randomUUID().toString(),
    val programId: String,
    val programName: String,
    val workoutName: String,
    val completedAt: LocalDateTime = LocalDateTime.now(),
    val durationMinutes: Int = 0,
    val exercises: List<CompletedExercise> = emptyList(),
    val totalVolume: Double = 0.0, // общий тоннаж (вес * повторы)
    val weight: Double? = null, // вес тела во время тренировки
    val calories: Int? = null // сожженные калории за тренировку
)

// Статистика программы
data class ProgramStats(
    val programId: String,
    val timesCompleted: Int = 0,
    val lastCompletedAt: LocalDateTime? = null,
    val totalWorkouts: Int = 0,
    val bestExercises: Map<String, CompletedExercise> = emptyMap() // лучшие результаты по упражнениям
)

data class WorkoutProgram(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val goal: String = "",
    val durationWeeks: Int = 4,
    val daysPerWeek: Int = 3,
    val workouts: List<Workout> = emptyList(),
    val targetCompletions: Int = 10, // целевое количество выполнений
    val completedCount: Int = 0 // сколько раз выполнена
)

// Детальная статистика пользователя
data class UserFitnessStats(
    val totalWorkouts: Int = 0,
    val totalVolume: Double = 0.0, // общий тоннаж за всё время
    val totalCalories: Int = 0,
    val totalDurationMinutes: Int = 0,
    val currentStreak: Int = 0, // дней подряд
    val longestStreak: Int = 0,
    val workoutsThisMonth: Int = 0,
    val workoutsThisWeek: Int = 0,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val muscleStats: Map<String, MuscleStatDetailed> = emptyMap(),
    val favoriteExercises: List<FavoriteExercise> = emptyList(),
    val recentAchievements: List<Achievement> = emptyList()
)

data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val recordType: RecordType, // MAX_WEIGHT, MAX_REPS, MAX_VOLUME
    val value: Double,
    val achievedAt: LocalDateTime
)

enum class RecordType { MAX_WEIGHT, MAX_REPS, MAX_VOLUME }

data class MuscleStatDetailed(
    val totalVolume: Double = 0.0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val exercisesCount: Int = 0,
    val lastTrained: LocalDateTime? = null
)

data class FavoriteExercise(
    val exerciseId: String,
    val exerciseName: String,
    val timesPerformed: Int,
    val totalVolume: Double
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val achievedAt: LocalDateTime,
    val type: AchievementType
)

enum class AchievementType { 
    FIRST_WORKOUT, 
    VOLUME_MILESTONE, 
    STREAK_MASTER, 
    PERSONAL_RECORD,
    CONSISTENCY_CHAMPION,
    MUSCLE_SPECIALIST
}

// ============= КВЕСТЫ И КАЛЕНДАРЬ =============

data class Quest(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val type: QuestType,
    val difficulty: QuestDifficulty = QuestDifficulty.MEDIUM,
    val xpReward: Int = 50,
    val goldReward: Int = 10,
    val isCompleted: Boolean = false,
    val completedAt: LocalDateTime? = null,
    val scheduledDateTime: LocalDateTime? = null,
    val hasReminder: Boolean = false
)

// Для совместимости
typealias HealthQuest = Quest

enum class QuestType { DAILY, WEEKLY, MONTHLY }
enum class QuestDifficulty { TRIVIAL, EASY, MEDIUM, HARD, EPIC }
enum class QuestCategory { PHYSICAL, MENTAL, SOCIAL, CREATIVE, HEALTH }

// Модели для календаря задач
data class CalendarTask(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "",
    val description: String = "",
    val dateTime: LocalDateTime,
    val isCompleted: Boolean = false,
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalDateTime? = null,
    val category: QuestCategory = QuestCategory.HEALTH,
    val xpReward: Int = 50,
    val goldReward: Int = 10
)

// ============= ПЕРСОНАЖ И ИНВЕНТАРЬ =============

// 50 Профессий
enum class Profession(val displayName: String, val icon: String) {
    STUDENT("Студент", "🎓"),
    MANAGER("Менеджер", "💼"),
    PROGRAMMER("Программист", "💻"),
    TEACHER("Учитель", "👨‍🏫"),
    DOCTOR("Врач", "👨‍⚕️"),
    ENGINEER("Инженер", "⚙️"),
    DESIGNER("Дизайнер", "🎨"),
    ACCOUNTANT("Бухгалтер", "📊"),
    LAWYER("Юрист", "⚖️"),
    SALESPERSON("Продавец", "🛒"),
    COOK("Повар", "👨‍🍳"),
    DRIVER("Водитель", "🚗"),
    ADMINISTRATOR("Администратор", "📋"),
    PSYCHOLOGIST("Психолог", "🧠"),
    MARKETER("Маркетолог", "📢"),
    EDITOR("Редактор", "✏️"),
    PHOTOGRAPHER("Фотограф", "📷"),
    MUSICIAN("Музыкант", "🎵"),
    ARTIST("Художник", "🖌️"),
    ARCHITECT("Архитектор", "🏛️"),
    BUILDER("Строитель", "🔨"),
    ECONOMIST("Экономист", "📈"),
    TRANSLATOR("Переводчик", "🌐"),
    REALTOR("Риелтор", "🏠"),
    TRAINER("Тренер", "💪"),
    BLOGGER("Блогер", "📹"),
    WRITER("Писатель", "📝"),
    ACTOR("Актёр", "🎭"),
    POLICEMAN("Полицейский", "👮"),
    FIREFIGHTER("Пожарный", "👨‍🚒"),
    DENTIST("Стоматолог", "🦷"),
    ANALYST("Аналитик", "📊"),
    HR_SPECIALIST("HR-специалист", "👥"),
    PR_MANAGER("PR-менеджер", "📰"),
    COPYWRITER("Копирайтер", "✍️"),
    FITNESS_TRAINER("Фитнес-тренер", "🏋️"),
    NANNY("Няня", "👶"),
    CLEANER("Уборщик", "🧹"),
    SECRETARY("Секретарь", "📞"),
    GUARD("Охранник", "🛡️"),
    COURIER("Курьер", "📦"),
    TAXI_DRIVER("Таксист", "🚕"),
    MAKEUP_ARTIST("Визажист", "💄"),
    HAIRDRESSER("Парикмахер", "💇"),
    DENTAL_HYGIENIST("Стоматолог-гигиенист", "🦷"),
    NURSE("Медсестра", "💉"),
    REAL_ESTATE_AGENT("Агент по недвижимости", "🏢"),
    DELIVERY_PERSON("Доставщик", "🚴"),
    PC_OPERATOR("Оператор ПК", "⌨️"),
    CONSULTANT("Консультант", "🎯");

    companion object {
        fun getAll(): List<Profession> = values().toList()
    }
}

// 20 Классов персонажей
enum class CharacterClass(val displayName: String, val icon: String, val category: ClassCategory) {
    WARRIOR("Воин", "⚔️", ClassCategory.RPG_CLASSIC),
    MAGE("Маг", "🔮", ClassCategory.RPG_CLASSIC),
    ARCHER("Лучник", "🏹", ClassCategory.RPG_CLASSIC),
    KNIGHT("Рыцарь", "🛡️", ClassCategory.RPG_CLASSIC),
    THIEF("Вор", "🗡️", ClassCategory.RPG_CLASSIC),
    PRIEST("Жрец", "✨", ClassCategory.RPG_CLASSIC),
    NECROMANCER("Некромант", "💀", ClassCategory.RPG_CLASSIC),
    DRUID("Друид", "🌿", ClassCategory.RPG_CLASSIC),
    PALADIN("Паладин", "⭐", ClassCategory.RPG_CLASSIC),
    BARD("Бард", "🎸", ClassCategory.RPG_CLASSIC),
    SORCERER("Чародей", "🌟", ClassCategory.FANTASY),
    MONK("Монах", "🙏", ClassCategory.FANTASY),
    DEMON_HUNTER("Охотник на демонов", "👹", ClassCategory.FANTASY),
    ALCHEMIST("Алхимик", "🧪", ClassCategory.FANTASY),
    CYBERNETICIST("Кибернетик", "🤖", ClassCategory.SCI_FI),
    SPACE_MARINE("Космодесантник", "🚀", ClassCategory.SCI_FI),
    HACKER("Хакер", "💾", ClassCategory.SCI_FI),
    DRONE_ENGINEER("Инженер дронов", "🛸", ClassCategory.SCI_FI),
    BIOENGINEER("Биоинженер", "🧬", ClassCategory.SCI_FI),
    MECH_PILOT("Пилот мехов", "🦾", ClassCategory.SCI_FI);

    companion object {
        fun getAll(): List<CharacterClass> = values().toList()
        fun getByCategory(category: ClassCategory): List<CharacterClass> = values().filter { it.category == category }
    }
}

enum class ClassCategory(val displayName: String) {
    RPG_CLASSIC("Классические RPG"),
    FANTASY("Фэнтези"),
    SCI_FI("Sci-Fi")
}

data class Character(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val level: Int = 1,
    val experience: Int = 0,
    val experienceToNextLevel: Int = 100,
    val health: Int = 100,
    val maxHealth: Int = 100,
    val gold: Int = 0,
    val gender: String = "Мужской",
    val profession: Profession = Profession.STUDENT,
    val characterClass: CharacterClass = CharacterClass.WARRIOR,
    val stats: CharacterStats = CharacterStats(),
    val equippedItems: EquippedItems = EquippedItems()
)

data class CharacterStats(
    val strength: Int = 5,
    val agility: Int = 5,
    val intelligence: Int = 5,
    val vitality: Int = 5
)

data class EquippedItems(
    val weapon: Item? = null,
    val armor: Item? = null,
    val accessory: Item? = null
)

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: ItemType = ItemType.ARMOR,
    val rarity: ItemRarity = ItemRarity.COMMON,
    val description: String = "",
    val price: Int = 0,
    val statsBonus: CharacterStats = CharacterStats(),
    val iconEmoji: String = "⚔️"
)

enum class ItemType { WEAPON, ARMOR, ACCESSORY, CONSUMABLE }
enum class ItemRarity { COMMON, UNCOMMON, RARE, EPIC, LEGENDARY }

// ============= ЗАГЛУШКИ ДЛЯ КОМПИЛЯЦИИ =============

data class Workout(
    val name: String = "",
    val description: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val estimatedDuration: Int = 60
)

data class WorkoutExercise(
    val exercise: Exercise,
    val sets: Int = 3,
    val reps: Int = 10,
    val weight: Double = 0.0
)

data class UserProfile(
    val name: String = "",
    val age: Int? = null,
    val gender: String? = null,
    val currentWeight: Double? = null,
    val targetWeight: Double? = null,
    val height: Double? = null
)

// ============= РЕЦЕПТЫ И ДИЕТЫ =============

data class RecipeIngredient(
    val foodId: String,
    val amount: Float,
    val unit: String
)

// Псевдоним типа для совместимости с TheMealDB
typealias Ingredient = RecipeIngredient

data class Recipe(
    val id: Int = 0,
    val name: String = "",
    val category: String = "",
    val difficulty: String = "",
    val prepTimeMinutes: Int = 0,
    val cookTimeMinutes: Int = 0,
    val servings: Int = 1,
    val description: String = "",
    val ingredients: List<RecipeIngredient> = emptyList(),
    val instructions: List<String> = emptyList(),
    val macros: Macros = Macros(),
    val calories: Double = 0.0,
    val protein: Double = 0.0,
    val fat: Double = 0.0,
    val carbohydrates: Double = 0.0,
    val tags: List<String> = emptyList(),
    val imageUrl: String = ""
)

data class Diet(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String? = null,
    val targetCalories: Int = 2000,
    val proteinRatio: Int = 30,
    val fatRatio: Int = 30,
    val carbRatio: Int = 40,
    val allowedFoodIds: List<String> = emptyList(),
    val forbiddenFoodIds: List<String> = emptyList(),
    val recipeExamples: List<String> = emptyList(),
    val tips: List<String> = emptyList()
)