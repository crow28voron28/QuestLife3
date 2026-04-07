package com.questlife.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.questlife.app.data.ExerciseDatabase
import com.questlife.app.data.FitnessDatabase
import com.questlife.app.data.WorkoutProgramEntity
import com.questlife.app.data.CompletedWorkoutEntity
import com.questlife.app.data.SetHistoryEntity
import com.questlife.app.data.RussianRecipesDatabase
import com.questlife.app.models.WorkoutProgram
import com.questlife.app.models.Workout
import com.questlife.app.models.WorkoutExercise
import com.questlife.app.models.CompletedWorkout
import com.questlife.app.models.ProgramStats
import com.questlife.app.repository.Repositories
import com.questlife.app.ui.screens.*
import com.questlife.app.ui.theme.QuestLifeTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.questlife.app.models.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDateTime

// Глобальное хранилище программ и статистики с использованием Room Database
object ProgramStore {
    private var database: FitnessDatabase? = null
    private val gson = Gson()
    private var scope: kotlinx.coroutines.CoroutineScope? = null
    
    private val _programs = MutableStateFlow<List<WorkoutProgram>>(emptyList())
    val programs: StateFlow<List<WorkoutProgram>> = _programs
    
    // Хранилище завершенных тренировок
    private val _completedWorkouts = MutableStateFlow<List<CompletedWorkout>>(emptyList())
    val completedWorkouts: StateFlow<List<CompletedWorkout>> = _completedWorkouts
    
    // Статистика по мышцам
    private val _muscleStats = MutableStateFlow<Map<String, MuscleStat>>(emptyMap())
    val muscleStats: StateFlow<Map<String, MuscleStat>> = _muscleStats
    
    fun initDatabase(context: android.content.Context, coroutineScope: kotlinx.coroutines.CoroutineScope) {
        database = FitnessDatabase.getDatabase(context)
        scope = coroutineScope
        loadFromDatabase()
        // Загружаем предопределенные программы из program_base.json
        loadPredefinedPrograms()
    }
    
    private fun loadPredefinedPrograms() {
        scope?.launch {
            // Очищаем все программы перед загрузкой из JSON, чтобы избежать дублирования и старых данных
            database?.fitnessDao()?.deleteAllPrograms()
            
            // Загружаем программы из program_base.json
            val predefinedPrograms = com.questlife.app.data.PredefinedWorkoutPrograms.getAllPredefinedPrograms()
            
            // Добавляем программы из JSON (помечаем как не измененные)
            predefinedPrograms.forEach { program ->
                database?.fitnessDao()?.insertProgram(program.toEntity(isCustom = false, originalId = null))
            }
        }
    }
    
    private fun loadFromDatabase() {
        database?.let { db ->
            scope?.launch {
                db.fitnessDao().getAllPrograms().collect { entities ->
                    _programs.value = entities.map { entity ->
                        entity.toWorkoutProgram()
                    }
                }
            }
            scope?.launch {
                db.fitnessDao().getAllCompletedWorkouts().collect { entities ->
                    _completedWorkouts.value = entities.map { entity ->
                        entity.toCompletedWorkout()
                    }
                }
            }
        }
    }
    
    fun addProgram(program: WorkoutProgram) {
        database?.let { db ->
            scope?.launch {
                // Новые программы - пользовательские (кастомные)
                db.fitnessDao().insertProgram(program.toEntity(isCustom = true, originalId = null))
            }
        } ?: run {
            _programs.value = _programs.value + program
        }
    }
    
    fun updateProgram(program: WorkoutProgram, isUserEdit: Boolean = false, originalId: String? = null) {
        database?.let { db ->
            scope?.launch {
                // Если программа редактируется пользователем, создаем копию с пометкой "измененная"
                val existingEntity = db.fitnessDao().getProgramById(program.id)
                val shouldMarkAsCustom = existingEntity?.isCustom == false && isUserEdit
                
                db.fitnessDao().updateProgram(
                    program.toEntity(
                        isCustom = if (shouldMarkAsCustom) true else existingEntity?.isCustom ?: false,
                        originalId = if (shouldMarkAsCustom) program.id else existingEntity?.originalId
                    )
                )
            }
        } ?: run {
            _programs.value = _programs.value.map { 
                if (it.id == program.id) program else it 
            }
        }
    }
    
    fun deleteProgram(program: WorkoutProgram) {
        database?.let { db ->
            scope?.launch {
                db.fitnessDao().deleteProgram(program.toEntity())
            }
        } ?: run {
            _programs.value = _programs.value.filter { it.id != program.id }
        }
    }
    
    fun getProgramById(id: String): WorkoutProgram? {
        return _programs.value.find { it.id == id }
    }
    
    // Добавление завершенной тренировки
    fun addCompletedWorkout(workout: CompletedWorkout, weight: Double? = null, calories: Int? = null) {
        val workoutWithWeight = workout.copy(
            durationMinutes = 0 // Можно рассчитать реальную длительность
        )
        
        database?.let { db ->
            scope?.launch {
                val entity = workoutWithWeight.toEntity(weight, calories)
                db.fitnessDao().insertCompletedWorkout(entity)
                
                // Сохраняем историю каждого подхода
                workoutWithWeight.exercises.forEach { ex ->
                    val history = SetHistoryEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        exerciseId = ex.exerciseId,
                        exerciseName = ex.exerciseName,
                        programId = workoutWithWeight.programId,
                        programName = workoutWithWeight.programName,
                        workoutName = workoutWithWeight.workoutName,
                        sets = ex.sets,
                        reps = ex.reps,
                        weight = ex.weight,
                        volume = ex.weight * ex.reps * ex.sets,
                        force = ex.weight * ex.reps * ex.sets, // Сила = тоннаж
                        completedAt = LocalDateTime.now().toString()
                    )
                    db.fitnessDao().insertSetHistory(history)
                }
                
                // Обновляем статистику по мышцам
                updateMuscleStatistics(workoutWithWeight)
            }
        } ?: run {
            _completedWorkouts.value = _completedWorkouts.value + workoutWithWeight
        }
        
        // Увеличиваем счетчик выполнений программы
        val program = _programs.value.find { it.id == workout.programId }
        if (program != null) {
            val updatedProgram = program.copy(
                completedCount = program.completedCount + 1
            )
            updateProgram(updatedProgram)
        }
    }
    
    // Обновление статистики по мышцам
    private fun updateMuscleStatistics(workout: CompletedWorkout) {
        val today = java.time.LocalDate.now().toString()
        val muscleGroupsMap = mutableMapOf<String, MuscleStat>()
        
        workout.exercises.forEach { ex ->
            // Находим упражнение в базе для получения групп мышц (primary + secondary)
            val exercise = ExerciseDatabase.exercises.find { it.id == ex.exerciseId }
            // Используем muscleGroups из упражнения, которые уже включают primary и secondary мышцы
            val musclesToCount = exercise?.muscleGroups ?: emptyList()
            
            musclesToCount.forEach { mg ->
                val groupName = mg.name
                val current = muscleGroupsMap[groupName] ?: MuscleStat()
                muscleGroupsMap[groupName] = current.copy(
                    exercisesCount = current.exercisesCount + 1,
                    totalSets = current.totalSets + ex.sets,
                    totalReps = current.totalReps + (ex.reps * ex.sets),
                    totalVolume = current.totalVolume + (ex.weight * ex.reps * ex.sets)
                )
            }
        }
        
        // Сохраняем статистику
        database?.let { db ->
            scope?.launch {
                muscleGroupsMap.forEach { (muscleGroup, stats) ->
                    val entity = com.questlife.app.data.MuscleStatisticsEntity(
                        id = java.util.UUID.randomUUID().toString(),
                        muscleGroup = muscleGroup,
                        workoutDate = today,
                        exercisesCount = stats.exercisesCount,
                        totalSets = stats.totalSets,
                        totalReps = stats.totalReps,
                        totalVolume = stats.totalVolume
                    )
                    db.fitnessDao().insertMuscleStatistics(entity)
                }
            }
        }
        
        // Обновляем состояние
        _muscleStats.value = muscleGroupsMap
    }
    
    // Получение статистики по программе
    fun getProgramStats(programId: String): ProgramStats {
        val workouts = _completedWorkouts.value.filter { it.programId == programId }
        val timesCompleted = workouts.size
        val lastCompletedAt = workouts.maxByOrNull { it.completedAt }?.completedAt
        
        // Лучшие результаты по упражнениям
        val bestExercises = mutableMapOf<String, CompletedExercise>()
        workouts.flatMap { it.exercises }.forEach { exercise ->
            val currentBest = bestExercises[exercise.exerciseId]
            if (currentBest == null || 
                exercise.reps > currentBest.reps || 
                exercise.weight > currentBest.weight) {
                bestExercises[exercise.exerciseId] = exercise
            }
        }
        
        return ProgramStats(
            programId = programId,
            timesCompleted = timesCompleted,
            lastCompletedAt = lastCompletedAt,
            totalWorkouts = timesCompleted,
            bestExercises = bestExercises
        )
    }
    
    // Получение всех завершенных тренировок для истории
    fun getAllCompletedWorkouts(): List<CompletedWorkout> {
        return _completedWorkouts.value.sortedByDescending { it.completedAt }
    }
    
    // Поиск похожих упражнений по мышцам и типу
    fun findSimilarExercises(currentExercise: Exercise, limit: Int = 10): List<Exercise> {
        return ExerciseDatabase.exercises.filter { exercise ->
            exercise.id != currentExercise.id &&
            (exercise.muscleGroups.any { it in currentExercise.muscleGroups } ||
             exercise.category == currentExercise.category)
        }.sortedByDescending { exercise ->
            // Сортируем по количеству общих мышечных групп
            exercise.muscleGroups.count { it in currentExercise.muscleGroups }
        }.take(limit)
    }
}

// Вспомогательные данные для статистики мышц
data class MuscleStat(
    val exercisesCount: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val totalVolume: Double = 0.0
)

// Extension функции для конвертации между Entity и Model
fun WorkoutProgram.toEntity(isCustom: Boolean = false, originalId: String? = null): WorkoutProgramEntity {
    return WorkoutProgramEntity(
        id = this.id,
        name = this.name,
        description = this.description,
        goal = this.goal,
        durationWeeks = this.durationWeeks,
        daysPerWeek = this.daysPerWeek,
        workoutsJson = com.google.gson.Gson().toJson(this.workouts),
        targetCompletions = this.targetCompletions,
        completedCount = this.completedCount,
        isCustom = isCustom,
        originalId = originalId
    )
}

fun WorkoutProgramEntity.toWorkoutProgram(): WorkoutProgram {
    val type = object : TypeToken<List<Workout>>() {}.type
    val workouts = com.google.gson.Gson().fromJson<List<Workout>>(this.workoutsJson, type) ?: emptyList()
    return WorkoutProgram(
        id = this.id,
        name = this.name + if (this.isCustom) " (измененная)" else "",
        description = this.description,
        goal = this.goal,
        durationWeeks = this.durationWeeks,
        daysPerWeek = this.daysPerWeek,
        workouts = workouts,
        targetCompletions = this.targetCompletions,
        completedCount = this.completedCount
    )
}

fun CompletedWorkout.toEntity(weight: Double?, calories: Int?): CompletedWorkoutEntity {
    return CompletedWorkoutEntity(
        id = this.id,
        programId = this.programId,
        programName = this.programName,
        workoutName = this.workoutName,
        completedAt = this.completedAt.toString(),
        durationMinutes = this.durationMinutes,
        exercisesJson = com.google.gson.Gson().toJson(this.exercises),
        totalVolume = this.totalVolume,
        weight = weight,
        calories = calories
    )
}

fun CompletedWorkoutEntity.toCompletedWorkout(): CompletedWorkout {
    val type = object : TypeToken<List<CompletedExercise>>() {}.type
    val exercises = com.google.gson.Gson().fromJson<List<CompletedExercise>>(this.exercisesJson, type) ?: emptyList()
    return CompletedWorkout(
        id = this.id,
        programId = this.programId,
        programName = this.programName,
        workoutName = this.workoutName,
        completedAt = LocalDateTime.parse(this.completedAt),
        durationMinutes = this.durationMinutes,
        exercises = exercises,
        totalVolume = this.totalVolume,
        weight = this.weight,
        calories = this.calories
    )
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Инициализируем PredefinedWorkoutPrograms с контекстом ПЕРВЫМ
        com.questlife.app.data.PredefinedWorkoutPrograms.init(this)

        setContent {
            QuestLifeTheme {
                // Инициализируем репозитории с контекстом
                Repositories.initialize(this@MainActivity)
                
                // Загружаем базу упражнений из JSON перед отображением контента
                LaunchedEffect(Unit) {
                    ExerciseDatabase.loadFromAssets(this@MainActivity)
                    // Инициализируем базу данных фитнеса после загрузки упражнений
                    ProgramStore.initDatabase(this@MainActivity, lifecycleScope)
                    // Инициализируем русскую базу рецептов
                    RussianRecipesDatabase.initialize(this@MainActivity)
                }
                
                QuestLifeApp()
            }
        }
    }
}

@Composable
fun QuestLifeApp() {
    val navController = rememberNavController()
    
    // Состояние персонажа - сохраняется в памяти
    var character by remember { mutableStateOf<Character?>(null) }
    var showGenderSelection by remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { BottomNavigationBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Quests.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Quests.route) {
                QuestsScreen(
                    character = character,
                    onSetupCharacter = {
                        if (character == null) {
                            showGenderSelection = true
                        }
                    },
                    onCharacterUpdate = { updatedCharacter ->
                        character = updatedCharacter
                    }
                )
            }
            composable(Screen.Health.route)     { HealthScreen(navController) }
            composable(Screen.Character.route)  { CharacterScreen(character = character) }
            composable(Screen.Inventory.route)  { InventoryScreen() }
            composable(Screen.Shop.route)       { ShopScreen() }

            // Экран рецептов
            composable("recipes") {
                RecipesScreen(
                    onBackClick = { navController.popBackStack() },
                    onRecipeClick = { }
                )
            }
            
            // Экран диет
            composable("diets") {
                DietsScreen(
                    onBackClick = { navController.popBackStack() },
                    onDietClick = { }
                )
            }

            // Экран тренировки - теперь с реальными данными
            composable("workout_session/{programId}") { backStackEntry ->
                val programId = backStackEntry.arguments?.getString("programId") ?: ""
                
                // Ищем программу в хранилище или создаем временную
                val program = ProgramStore.getProgramById(programId) 
                    ?: WorkoutProgram(
                        id = programId,
                        name = "Тренировка",
                        description = "Текущая тренировка",
                        goal = "CUSTOM",
                        durationWeeks = 4,
                        daysPerWeek = 3,
                        workouts = emptyList()
                    )
                
                WorkoutSessionScreen(
                    program = program,
                    onFinish = { navController.popBackStack() }
                )
            }
        }
        
        // Диалог выбора пола
        if (showGenderSelection) {
            GenderSelectionScreen(
                onGenderSelected = { gender ->
                    character = Character(
                        name = "Герой",
                        gender = gender
                    )
                    showGenderSelection = false
                }
            )
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar {
        val screens = listOf(
            Screen.Quests,
            Screen.Health,
            Screen.Character,
            Screen.Inventory,
            Screen.Shop
        )

        screens.forEach { screen ->
            NavigationBarItem(
                icon = { Icon(screen.icon, contentDescription = screen.title) },
                label = { Text(screen.title) },
                selected = currentRoute == screen.route,
                onClick = {
                    navController.navigate(screen.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Quests    : Screen("quests",    "Квесты",     Icons.Default.CheckCircle)
    object Health    : Screen("health",    "Здоровье",   Icons.Default.Favorite)
    object Character : Screen("character", "Герой",      Icons.Default.Person)
    object Inventory : Screen("inventory", "Инвентарь",  Icons.Default.ShoppingBag)
    object Shop      : Screen("shop",      "Магазин",    Icons.Default.ShoppingCart)
}

// ==================== ЭКРАН ЗДОРОВЬЕ ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(navController: NavController) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Фитнес", "Питание")

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Здоровье") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            TabRow(selectedTabIndex = selectedTabIndex) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTabIndex) {
                    0 -> FitnessScreen(navController)
                    1 -> NutritionScreen(
                        onNavigateToRecipes = { navController.navigate("recipes") },
                        onNavigateToDiets = { navController.navigate("diets") }
                    )
                    else -> Text(
                        "Ошибка выбора вкладки",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}