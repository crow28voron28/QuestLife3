package com.questlife.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.questlife.app.ProgramStore
import com.questlife.app.data.ExerciseDatabase
import com.questlife.app.models.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import java.time.LocalDateTime
import java.time.LocalDate
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(navController: NavController) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Программы", "Упражнения", "Статистика", "Достижения")

    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showCreateProgramDialog by remember { mutableStateOf(false) }
    var showAddExercisesToProgramDialog by remember { mutableStateOf(false) }
    var selectedProgramForExercises by remember { mutableStateOf<WorkoutProgram?>(null) }

    // Используем StateFlow из ProgramStore для реактивного обновления
    var userPrograms by remember { mutableStateOf<List<WorkoutProgram>>(emptyList()) }
    val scope = rememberCoroutineScope()

    // Загружаем программы из ProgramStore
    LaunchedEffect(Unit) {
        com.questlife.app.ProgramStore.programs.collect { programs ->
            userPrograms = programs
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Фитнес") }) },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {

                SmallFloatingActionButton(
                    onClick = { showAddExerciseDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = "Создать упражнение")
                }

                SmallFloatingActionButton(
                    onClick = { showCreateProgramDialog = true },
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Создать программу")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            when (selectedTab) {
                0 -> WorkoutProgramsTab(
                    navController = navController,
                    onAddExercises = { program ->
                        selectedProgramForExercises = program
                        showAddExercisesToProgramDialog = true
                    }
                )
                1 -> ExercisesTab()
                2 -> StatisticsTab()
                3 -> AchievementsTab()
            }
        }
    }


    if (showAddExerciseDialog) {
        AddCustomExerciseDialog(
            onDismiss = { showAddExerciseDialog = false },
            onSave = { exercise ->
                ExerciseDatabase.addCustomExercise(exercise)
                showAddExerciseDialog = false
            }
        )
    }

    if (showCreateProgramDialog) {
        CreateProgramDialog(
            onDismiss = { showCreateProgramDialog = false },
            onProgramCreated = { newProgram ->
                // Сохраняем в глобальное хранилище
                com.questlife.app.ProgramStore.addProgram(newProgram)
                showCreateProgramDialog = false
            }
        )
    }

    if (showAddExercisesToProgramDialog && selectedProgramForExercises != null) {
        AddExercisesToProgramDialog(
            exercises = ExerciseDatabase.exercises,
            program = selectedProgramForExercises!!,
            onDismiss = { 
                showAddExercisesToProgramDialog = false
                selectedProgramForExercises = null
            },
            onExercisesAdded = { newExercises ->
                // Добавляем упражнения к первой тренировке программы
                val updatedWorkouts = selectedProgramForExercises!!.workouts.toMutableList()
                if (updatedWorkouts.isNotEmpty()) {
                    val firstWorkout = updatedWorkouts[0]
                    updatedWorkouts[0] = firstWorkout.copy(
                        exercises = firstWorkout.exercises + newExercises
                    )
                } else {
                    updatedWorkouts.add(Workout(name = "Тренировка 1", exercises = newExercises))
                }
                
                val updatedProgram = selectedProgramForExercises!!.copy(workouts = updatedWorkouts)
                // Помечаем как измененную пользователем программу
                com.questlife.app.ProgramStore.updateProgram(updatedProgram, isUserEdit = true)
                showAddExercisesToProgramDialog = false
                selectedProgramForExercises = null
            }
        )
    }
}

// ==================== ВКЛАДКА ПРОГРАММ ====================
@Composable
fun WorkoutProgramsTab(
    navController: NavController,
    onAddExercises: (WorkoutProgram) -> Unit
) {
    // Получаем программы из хранилища
    val storedPrograms = com.questlife.app.ProgramStore.programs.collectAsState(initial = emptyList()).value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(storedPrograms) { program ->
            WorkoutProgramCard(
                program = program,
                navController = navController,
                onAddExercises = onAddExercises
            )
        }
        
        if (storedPrograms.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📋", style = MaterialTheme.typography.displayLarge)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Нет созданных программ",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Нажмите + чтобы создать свою первую программу",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutProgramCard(
    program: WorkoutProgram,
    navController: NavController,
    onAddExercises: (WorkoutProgram) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(program.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Редактировать", tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { showDeleteConfirmDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(program.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ProgramInfoChip("⏱️", "${program.durationWeeks} недель")
                ProgramInfoChip("📅", "${program.daysPerWeek} дней")
                ProgramInfoChip("💪", "${program.workouts.sumOf { it.exercises.size }} упражн.")
            }

            // Прогресс-бар выполнения программы
            Spacer(modifier = Modifier.height(12.dp))
            val progress = if (program.targetCompletions > 0) {
                program.completedCount.toFloat() / program.targetCompletions
            } else 0f
            LinearProgressIndicator(
                progress = progress.coerceIn(0f, 1f),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Выполнено: ${program.completedCount}/${program.targetCompletions}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                if (program.workouts.isEmpty()) {
                    Text("Нет упражнений в программе", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { onAddExercises(program) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Добавить упражнения")
                    }
                } else {
                    program.workouts.forEach { workout ->
                        Text(workout.name, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(8.dp))

                        workout.exercises.forEach { we ->
                            var showExerciseDetails by remember { mutableStateOf(false) }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable { showExerciseDetails = true }
                            ) {
                                // Отображение миниатюры упражнения
                                if (we.exercise.imageUrl.isNotBlank() || we.exercise.imageUrl2.isNotBlank()) {
                                    val firstImage = we.exercise.imageUrl.takeIf { it.isNotBlank() } 
                                        ?: we.exercise.imageUrl2
                                    AsyncImage(
                                        model = firstImage,
                                        contentDescription = we.exercise.name,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier.size(48.dp).background(Color.LightGray, RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏋️")
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(we.exercise.name, style = MaterialTheme.typography.bodyLarge)
                                Icon(Icons.Default.Info, contentDescription = "Инфо", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            
                            ExerciseDetailsDialog(
                                exercise = we.exercise,
                                onDismiss = { showExerciseDetails = false },
                                visible = showExerciseDetails
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Кнопка «Начать тренировку»
                if (program.workouts.isNotEmpty()) {
                    Button(
                        onClick = {
                            navController.navigate("workout_session/${program.id}")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Начать тренировку", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
    
    // Диалог подтверждения удаления
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Удаление программы") },
            text = { Text("Вы уверены, что хотите удалить программу \"${program.name}\"? Это действие нельзя отменить.") },
            confirmButton = {
                Button(
                    onClick = {
                        com.questlife.app.ProgramStore.deleteProgram(program)
                        showDeleteConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }
    
    // Диалог редактирования программы - временно отключен, т.к. EditProgramDialog не определен
    // if (showEditDialog) {
    //     EditProgramDialog(
    //         program = program,
    //         onDismiss = { showEditDialog = false },
    //         onProgramUpdated = { updatedProgram ->
    //             com.questlife.app.ProgramStore.updateProgram(updatedProgram)
    //             showEditDialog = false
    //         }
    //     )
    // }
}

@Composable
fun ProgramInfoChip(icon: String, text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
    ) {
        Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
            Text(icon)
            Spacer(modifier = Modifier.width(6.dp))
            Text(text, style = MaterialTheme.typography.labelMedium)
        }
    }
}

// ==================== ВКЛАДКА УПРАЖНЕНИЙ ====================
@Composable
fun ExercisesTab() {
    // Используем mutableStateList для реактивного обновления при загрузке данных
    var allExercises by remember { mutableStateOf(ExerciseDatabase.exercises) }
    var searchQuery by remember { mutableStateOf("") }
    
    // Обновляем список упражнений при изменении данных в ExerciseDatabase
    LaunchedEffect(Unit) {
        // Небольшая задержка чтобы дать время на загрузку из assets
        kotlinx.coroutines.delay(100)
        allExercises = ExerciseDatabase.exercises
    }
    
    val filteredExercises = remember(searchQuery, allExercises) {
        if (searchQuery.isEmpty()) {
            allExercises
        } else {
            val query = searchQuery.lowercase().trim()
            allExercises.filter { 
                it.name.lowercase().contains(query) || 
                it.description.contains(query, ignoreCase = true) ||
                it.muscleGroups.any { m -> m.name.lowercase().contains(query) }
            }
        }
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // Поле поиска
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text("Поиск упражнений") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            }
        )
        
        // Список упражнений
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredExercises) { exercise ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { }
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        // Название и описание
                        Text(
                            exercise.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            exercise.muscleGroups.joinToString(", ") { it.name },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (exercise.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                exercise.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Изображения (2 в ряд)
                        if (exercise.imageUrl.isNotBlank() || exercise.imageUrl2.isNotBlank()) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (exercise.imageUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = exercise.imageUrl,
                                        contentDescription = exercise.name,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                if (exercise.imageUrl2.isNotBlank()) {
                                    AsyncImage(
                                        model = exercise.imageUrl2,
                                        contentDescription = "${exercise.name} - вид 2",
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(150.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        // Информация об оборудовании и сложности
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                when (exercise.equipment) {
                                    ExerciseEquipment.NONE -> "🏋️‍♂️ Без инвентаря"
                                    ExerciseEquipment.DUMBBELL -> "🏋️ Гантели"
                                    ExerciseEquipment.BARBELL -> "🏋️ Штанга"
                                    ExerciseEquipment.MACHINE -> "🏋️ Тренажёр"
                                    ExerciseEquipment.CABLE -> "🏋️ Блок"
                                    ExerciseEquipment.KETTLEBELL -> "🏋️ Гиря"
                                    ExerciseEquipment.BODYWEIGHT -> "💪 Вес тела"
                                    else -> "🏋️ Другое"
                                },
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                when (exercise.difficulty) {
                                    ExerciseDifficulty.BEGINNER -> "Лёгкий"
                                    ExerciseDifficulty.INTERMEDIATE -> "Средний"
                                    ExerciseDifficulty.ADVANCED -> "Сложный"
                                    else -> "Эксперт"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when (exercise.difficulty) {
                                    ExerciseDifficulty.BEGINNER -> Color.Green
                                    ExerciseDifficulty.INTERMEDIATE -> Color(0xFFFFA500)
                                    ExerciseDifficulty.ADVANCED -> Color.Red
                                    else -> Color(0xFF8B0000)
                                }
                            )
                        }
                    }
                }
            }
            
            if (filteredExercises.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Упражнения не найдены",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ==================== ВКЛАДКА СТАТИСТИКИ ====================
@Composable
fun StatisticsTab() {
    val completedWorkouts = com.questlife.app.ProgramStore.completedWorkouts.collectAsState(initial = emptyList()).value
    
    // Вычисляем расширенную статистику
    val userStats = remember(completedWorkouts) {
        calculateUserFitnessStats(completedWorkouts)
    }
    
    // Группируем тренировки по датам для истории
    val workoutsByDate = remember(completedWorkouts) {
        completedWorkouts.groupBy { it.completedAt.toLocalDate() }
            .entries.sortedByDescending { it.key }
            .associate { it.key to it.value }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Заголовок статистики
        Text("📊 Моя статистика", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Отслеживай свой прогресс и достижения",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        // Основные карточки статистики
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCardLarge(
                title = "Всего тренировок",
                value = userStats.totalWorkouts.toString(),
                icon = Icons.Default.FitnessCenter,
                subtitle = "начато программ",
                color = MaterialTheme.colorScheme.primaryContainer
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard("Тоннаж", "${(userStats.totalVolume / 1000).toInt()}т", Icons.Default.TrendingUp)
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard("Ккал", userStats.totalCalories.toString(), Icons.Default.LocalFireDepartment)
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard("Серия", "${userStats.currentStreak} дн.", Icons.Default.Star)
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                StatCard("За неделю", userStats.workoutsThisWeek.toString(), Icons.Default.CalendarToday)
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard("За месяц", userStats.workoutsThisMonth.toString(), Icons.Default.CalendarMonth)
            }
            Box(modifier = Modifier.weight(1f)) {
                StatCard("Рекорды", userStats.personalRecords.size.toString(), Icons.Default.EmojiEvents)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Достижения
        if (userStats.recentAchievements.isNotEmpty()) {
            Text("🏆 Последние достижения", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(userStats.recentAchievements.take(5)) { achievement ->
                    AchievementCard(achievement)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Любимые упражнения
        if (userStats.favoriteExercises.isNotEmpty()) {
            Text("💪 Любимые упражнения", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.heightIn(max = 200.dp)
            ) {
                items(userStats.favoriteExercises.take(5)) { favExercise ->
                    FavoriteExerciseCard(favExercise)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // Статистика по мышцам
        if (userStats.muscleStats.isNotEmpty()) {
            Text("🦴 Проработка мышц", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))
            
            val sortedMuscles = userStats.muscleStats.entries.sortedByDescending { it.value.totalVolume }
            sortedMuscles.take(6).forEach { (muscle, stats) ->
                MuscleStatRow(muscle, stats)
                Spacer(modifier = Modifier.height(4.dp))
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        
        // История тренировок
        Text("📅 История тренировок", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        
        if (workoutsByDate.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📋", style = MaterialTheme.typography.displayLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Пока нет завершённых тренировок",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Начните свою первую программу!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            workoutsByDate.forEach { (date, workouts) ->
                WorkoutHistoryDayCard(date, workouts)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

fun calculateUserFitnessStats(workouts: List<CompletedWorkout>): UserFitnessStats {
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val startOfMonth = today.withDayOfMonth(1)
    
    val totalWorkouts = workouts.size
    val totalVolume = workouts.sumOf { it.totalVolume }
    val totalCalories = workouts.sumOf { it.calories ?: 0 }
    
    val workoutsThisWeek = workouts.count { it.completedAt.toLocalDate() >= startOfWeek }
    val workoutsThisMonth = workouts.count { it.completedAt.toLocalDate() >= startOfMonth }
    
    // Подсчёт серии (дней подряд)
    val uniqueDates = workouts.map { it.completedAt.toLocalDate() }.distinct().sortedDescending()
    var currentStreak = 0
    var longestStreak = 0
    
    if (uniqueDates.isNotEmpty()) {
        currentStreak = 1
        for (i in 1 until uniqueDates.size) {
            if (uniqueDates[i-1].minusDays(1) == uniqueDates[i]) {
                currentStreak++
            } else {
                break
            }
        }
        longestStreak = currentStreak
        // Можно улучшить подсчёт самой длинной серии
    }
    
    // Персональные рекорды
    val personalRecords = mutableListOf<PersonalRecord>()
    val exerciseBests = mutableMapOf<String, Pair<Double, Double>>() // exerciseId -> (maxWeight, maxReps)
    
    workouts.flatMap { it.exercises }.forEach { ex ->
        val current = exerciseBests[ex.exerciseId]
        val newMaxWeight = max(ex.weight, current?.first ?: 0.0)
        val newMaxReps = max(ex.reps.toDouble(), current?.second ?: 0.0)
        if (current == null || ex.weight > current.first || ex.reps > current.second.toInt()) {
            exerciseBests[ex.exerciseId] = newMaxWeight to newMaxReps
            if (ex.weight > (current?.first ?: 0.0)) {
                personalRecords.add(PersonalRecord(
                    exerciseId = ex.exerciseId,
                    exerciseName = ex.exerciseName,
                    recordType = RecordType.MAX_WEIGHT,
                    value = ex.weight,
                    achievedAt = now
                ))
            }
        }
    }
    
    // Статистика по мышцам
    val muscleStats = mutableMapOf<String, MuscleStatDetailed>()
    workouts.forEach { workout ->
        workout.exercises.forEach { ex ->
            // Упрощённо - по названию мышцы
            val muscleGroup = when {
                ex.exerciseName.contains("жим", ignoreCase = true) || ex.exerciseName.contains("груд", ignoreCase = true) -> "Грудь"
                ex.exerciseName.contains("тяг", ignoreCase = true) || ex.exerciseName.contains("спин", ignoreCase = true) -> "Спина"
                ex.exerciseName.contains("присед", ignoreCase = true) || ex.exerciseName.contains("ног", ignoreCase = true) -> "Ноги"
                ex.exerciseName.contains("плеч", ignoreCase = true) || ex.exerciseName.contains("жам", ignoreCase = true) -> "Плечи"
                ex.exerciseName.contains("бицепс", ignoreCase = true) || ex.exerciseName.contains("трицепс", ignoreCase = true) -> "Руки"
                ex.exerciseName.contains("пресс", ignoreCase = true) -> "Пресс"
                else -> "Другое"
            }
            
            val current = muscleStats[muscleGroup] ?: MuscleStatDetailed()
            muscleStats[muscleGroup] = current.copy(
                totalVolume = current.totalVolume + ex.totalVolume,
                totalSets = current.totalSets + ex.sets,
                totalReps = current.totalReps + (ex.reps * ex.sets),
                exercisesCount = current.exercisesCount + 1,
                lastTrained = workout.completedAt
            )
        }
    }
    
    // Любимые упражнения
    val exerciseCounts = workouts.flatMap { it.exercises }
        .groupingBy { it.exerciseId to it.exerciseName }
        .eachCount()
        .map { (pair, count) ->
            val totalVol = workouts.flatMap { it.exercises }
                .filter { it.exerciseId == pair.first }
                .sumOf { it.totalVolume }
            FavoriteExercise(pair.first, pair.second, count, totalVol.toDouble())
        }
        .sortedByDescending { it.timesPerformed }
    
    // Достижения
    val achievements = mutableListOf<Achievement>()
    if (totalWorkouts >= 1) {
        achievements.add(Achievement(
            id = "first_workout",
            title = "Первый шаг",
            description = "Завершена первая тренировка",
            icon = "🎉",
            achievedAt = workouts.minByOrNull { it.completedAt }?.completedAt ?: now,
            type = AchievementType.FIRST_WORKOUT
        ))
    }
    if (currentStreak >= 7) {
        achievements.add(Achievement(
            id = "streak_week",
            title = "Недельный воин",
            description = "$currentStreak дней подряд",
            icon = "🔥",
            achievedAt = now,
            type = AchievementType.STREAK_MASTER
        ))
    }
    if (personalRecords.isNotEmpty()) {
        achievements.add(Achievement(
            id = "pr_achieved",
            title = "Рекордсмен",
            description = "${personalRecords.size} персональных рекордов",
            icon = "🏆",
            achievedAt = now,
            type = AchievementType.PERSONAL_RECORD
        ))
    }
    
    return UserFitnessStats(
        totalWorkouts = totalWorkouts,
        totalVolume = totalVolume,
        totalCalories = totalCalories,
        totalDurationMinutes = workouts.sumOf { it.durationMinutes },
        currentStreak = currentStreak,
        longestStreak = longestStreak,
        workoutsThisMonth = workoutsThisMonth,
        workoutsThisWeek = workoutsThisWeek,
        personalRecords = personalRecords.take(10),
        muscleStats = muscleStats,
        favoriteExercises = exerciseCounts.take(10),
        recentAchievements = achievements
    )
}

@Composable
fun StatCardLarge(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = Modifier
            .widthIn(min = 0.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun StatCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier
            .widthIn(min = 0.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(achievement.icon, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(achievement.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(achievement.description, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FavoriteExerciseCard(favExercise: FavoriteExercise) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(favExercise.exerciseName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("${favExercise.timesPerformed} раз • ${(favExercise.totalVolume / 1000).toInt()}т", 
                     style = MaterialTheme.typography.labelSmall, 
                     color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
        }
    }
}

@Composable
fun MuscleStatRow(muscle: String, stats: MuscleStatDetailed) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(muscle, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text("${(stats.totalVolume / 1000).toInt()}т • ${stats.totalSets} подх.", 
                 style = MaterialTheme.typography.labelSmall, 
                 color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun WorkoutHistoryDayCard(date: java.time.LocalDate, workouts: List<CompletedWorkout>) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        date.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${workouts.size} тренировк${if (workouts.size == 1) "а" else if (workouts.size in 2..4) "и" else "ий"}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null
                )
            }
            
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                workouts.forEach { workout ->
                    WorkoutHistoryItem(workout)
                    if (workout != workouts.last()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun WorkoutHistoryItem(workout: CompletedWorkout) {
    var showDetails by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = { showDetails = true },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(workout.programName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(
                    workout.completedAt.toLocalTime().toString().substring(0, 5),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("${workout.exercises.size} упражнений", style = MaterialTheme.typography.labelSmall)
                Text("${(workout.totalVolume / 1000).toInt()}т", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                if ((workout.calories ?: 0) > 0) {
                    Text("${workout.calories} ккал", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
    
    if (showDetails) {
        WorkoutDetailsDialog(workout = workout, onDismiss = { showDetails = false })
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutDetailsDialog(workout: CompletedWorkout, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text(workout.programName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    workout.completedAt.toString().replace("T", " "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Общая информация
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Упражнений", style = MaterialTheme.typography.labelSmall)
                        Text(workout.exercises.size.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Тоннаж", style = MaterialTheme.typography.labelSmall)
                        Text("${(workout.totalVolume / 1000).toInt()}т", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                    if ((workout.calories ?: 0) > 0) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Ккал", style = MaterialTheme.typography.labelSmall)
                            Text("${workout.calories}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))
                
                // Список упражнений
                Text("Выполненные упражнения:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                
                workout.exercises.forEachIndexed { index, ex ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("${index + 1}. ${ex.exerciseName}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("${ex.sets} × ${ex.reps}", style = MaterialTheme.typography.labelMedium)
                                Text("${ex.weight} кг", style = MaterialTheme.typography.labelMedium)
                                Text("${(ex.totalVolume / 1000).toInt()}т", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } }
    )
}

// ==================== ДИАЛОГ ДЕТАЛЕЙ УПРАЖНЕНИЯ ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseDetailsDialog(exercise: Exercise, onDismiss: () -> Unit, visible: Boolean = true) {
    if (!visible) return
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(exercise.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Изображения
                val images = listOfNotNull(
                    exercise.imageUrl.takeIf { it.isNotBlank() },
                    exercise.imageUrl2.takeIf { it.isNotBlank() }
                )
                
                if (images.isNotEmpty()) {
                    var currentImageIndex by remember { mutableStateOf(0) }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = images[currentImageIndex],
                            contentDescription = exercise.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        
                        if (images.size > 1) {
                            Row(
                                modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(images.size) { index ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (index == currentImageIndex) 10.dp else 8.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                if (index == currentImageIndex) Color.White
                                                else Color.White.copy(alpha = 0.5f)
                                            )
                                    )
                                }
                            }
                            
                            IconButton(
                                onClick = { currentImageIndex = (currentImageIndex - 1 + images.size) % images.size },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = "Предыдущее", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            IconButton(
                                onClick = { currentImageIndex = (currentImageIndex + 1) % images.size },
                                modifier = Modifier.align(Alignment.CenterEnd)
                            ) {
                                Icon(Icons.Default.ChevronRight, contentDescription = "Следующее", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Описание
                if (exercise.description.isNotBlank()) {
                    Text("Описание:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(exercise.description, style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Инструкция
                if (exercise.instructions.isNotEmpty()) {
                    Text("Инструкция:", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    exercise.instructions.forEachIndexed { index, instruction ->
                        Text("${index + 1}. $instruction", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
                
                // Информация
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Мышечные группы
                    Column {
                        Text("Целевые мышцы:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        exercise.muscleGroups.forEach { mg ->
                            Text("• ${mg.name}", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    
                    // Оборудование и сложность
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            when (exercise.equipment) {
                                ExerciseEquipment.NONE -> "Без инвентаря"
                                ExerciseEquipment.DUMBBELL -> "Гантели"
                                ExerciseEquipment.BARBELL -> "Штанга"
                                ExerciseEquipment.MACHINE -> "Тренажёр"
                                ExerciseEquipment.CABLE -> "Блок"
                                ExerciseEquipment.KETTLEBELL -> "Гиря"
                                ExerciseEquipment.BODYWEIGHT -> "Вес тела"
                                else -> "Другое"
                            },
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            when (exercise.difficulty) {
                                ExerciseDifficulty.BEGINNER -> "Лёгкий"
                                ExerciseDifficulty.INTERMEDIATE -> "Средний"
                                ExerciseDifficulty.ADVANCED -> "Сложный"
                                else -> "Эксперт"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = when (exercise.difficulty) {
                                ExerciseDifficulty.BEGINNER -> Color.Green
                                ExerciseDifficulty.INTERMEDIATE -> Color(0xFFFFA500)
                                ExerciseDifficulty.ADVANCED -> Color.Red
                                else -> Color(0xFF8B0000)
                            }
                        )
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } }
    )
}


// ==================== ВКЛАДКА ДОСТИЖЕНИЙ ====================
@Composable
fun AchievementsTab() {
    val allAchievements = com.questlife.app.data.PredefinedWorkoutPrograms.getAllPossibleAchievements()
    val completedWorkouts by com.questlife.app.ProgramStore.completedWorkouts.collectAsState(initial = emptyList())
    
    // Подсчет выполнений программ
    val programCompletionCounts = mutableMapOf<String, Int>()
    completedWorkouts.forEach { workout ->
        val count = programCompletionCounts.getOrDefault(workout.programId, 0) + 1
        programCompletionCounts[workout.programId] = count
    }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "🏆 Достижения",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            val totalEarned = programCompletionCounts.entries.sumOf { (_, count) ->
                (if (count >= 1) 1 else 0) + (if (count >= 10) 1 else 0) + (if (count >= 50) 1 else 0)
            }
            Text(
                "Получено: $totalEarned из ${allAchievements.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Сгруппированные достижения по программам
        val groupedAchievements = allAchievements.groupBy { it.programName }
        
        groupedAchievements.forEach { (programName, achievements) ->
            item {
                Text(
                    programName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            items(achievements) { achievement ->
                val completions = programCompletionCounts.getOrDefault(achievement.programId, 0)
                val isEarned = when (achievement.type) {
                    AchievementType.FIRST_WORKOUT -> completions >= 1
                    AchievementType.CONSISTENCY_CHAMPION -> completions >= 10
                    AchievementType.VOLUME_MILESTONE -> completions >= 50
                    else -> false
                }
                
                AchievementCardSmall(achievement, isEarned, completions)
            }
        }
    }
}

@Composable
fun AchievementCardSmall(
    achievement: com.questlife.app.data.PredefinedWorkoutPrograms.ProgramAchievement, 
    isEarned: Boolean,
    progress: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEarned) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = achievement.icon,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.alpha(if (isEarned) 1.0f else 0.3f)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.achievementTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isEarned) FontWeight.Bold else FontWeight.Normal,
                    color = if (isEarned) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Text(
                    text = "${achievement.achievementDescription} • Прогресс: $progress",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isEarned) 
                        MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
            
            if (isEarned) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "Получено",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = "Заблокировано",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
