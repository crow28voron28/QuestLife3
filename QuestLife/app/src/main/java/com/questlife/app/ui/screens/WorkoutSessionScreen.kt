package com.questlife.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.questlife.app.data.ExerciseDatabase
import com.questlife.app.data.FitnessDatabase
import com.questlife.app.data.SetHistoryEntity
import com.questlife.app.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    program: WorkoutProgram,
    onFinish: () -> Unit
) {
    var currentExerciseIndex by remember { mutableStateOf(0) }
    var isResting by remember { mutableStateOf(false) }
    var restTimeLeft by remember { mutableStateOf(60) }
    var customRestTime by remember { mutableStateOf(60) }
    var completedExercises by remember { mutableStateOf(0) }
    var showRestSettingsDialog by remember { mutableStateOf(false) }

    var exerciseResults by remember { mutableStateOf<Map<String, Pair<Int, Double>>>(emptyMap()) }
    var showResultDialog by remember { mutableStateOf(false) }
    var currentResultInput by remember { mutableStateOf<Pair<Int, Double>>(Pair(0, 0.0)) }

    var showSimilarExercisesDialog by remember { mutableStateOf(false) }
    
    // Локальное состояние для заменённых упражнений во время тренировки
    // Ключ: индекс упражнения в списке, Значение: новое упражнение
    var replacedExercises by remember { mutableStateOf<Map<Int, Exercise>>(emptyMap()) }
    
    // Флаг для отслеживания факта замены упражнений
    var hasReplacedExercises by remember { mutableStateOf(false) }
    
    // Диалог сохранения новой программы
    var showSaveNewProgramDialog by remember { mutableStateOf(false) }
    
    // Поиск упражнений
    var exerciseSearchQuery by remember { mutableStateOf("") }
    var showExerciseSearchDialog by remember { mutableStateOf(false) }
    
    // Выбор дня тренировки
    var selectedDayIndex by remember { mutableStateOf(0) }
    var showDaySelectorDialog by remember { mutableStateOf(false) }
    
    // Диалог завершения тренировки с весом и калориями
    var showFinishWorkoutDialog by remember { mutableStateOf(false) }
    var inputWeight by remember { mutableStateOf("") }
    var inputCalories by remember { mutableStateOf("") }

    // Получаем доступную тренировку на основе выбранного дня
    val currentWorkout = if (program.workouts.isNotEmpty()) {
        if (selectedDayIndex < program.workouts.size) {
            program.workouts[selectedDayIndex]
        } else {
            program.workouts.firstOrNull()
        }
    } else {
        null
    }

    if (currentWorkout == null || currentWorkout.exercises.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(program.name) },
                    navigationIcon = {
                        IconButton(onClick = onFinish) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                        }
                    }
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text("⚠️", style = MaterialTheme.typography.displayLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Text("В этой программе нет упражнений", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Text("Добавьте упражнения в программу перед началом тренировки", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onFinish) { Text("Вернуться назад") }
            }
        }
        return
    }

    // Получаем текущее упражнение с учётом замен
    val originalExercise = currentWorkout.exercises.getOrNull(currentExerciseIndex)?.exercise
    val currentExercise = replacedExercises[currentExerciseIndex] ?: originalExercise
    
    // История подходов для текущего упражнения
    var setHistory by remember { mutableStateOf<List<SetHistoryEntity>>(emptyList()) }
    
    // Получаем контекст один раз в начале композабла
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    
    // Загружаем историю при смене упражнения (используем originalExercise для истории)
    LaunchedEffect(originalExercise?.id, program.id) {
        if (originalExercise != null) {
            FitnessDatabase.getDatabase(context)
                .fitnessDao()
                .getSetHistoryByExerciseAndProgram(originalExercise.id, program.id)
                .collect { history ->
                    setHistory = history.take(10) // Последние 10 подходов
                }
        } else {
            setHistory = emptyList()
        }
    }

    LaunchedEffect(isResting, restTimeLeft) {
        if (isResting && restTimeLeft > 0) {
            delay(1000L)
            restTimeLeft--
        } else if (isResting && restTimeLeft == 0) {
            isResting = false
            restTimeLeft = customRestTime
        }
    }
    
    // Функция сохранения подхода в историю
    val saveSetToHistory: (String, String, Int, Int, Double) -> Unit = { exerciseId, exerciseName, sets, reps, weight ->
        scope.launch {
            val db = FitnessDatabase.getDatabase(context)
            val volume = weight * reps * sets
            val history = SetHistoryEntity(
                id = java.util.UUID.randomUUID().toString(),
                exerciseId = exerciseId,
                exerciseName = exerciseName,
                programId = program.id,
                programName = program.name,
                workoutName = currentWorkout.name.ifEmpty { "Тренировка $selectedDayIndex" },
                sets = sets,
                reps = reps,
                weight = weight,
                volume = volume,
                force = volume, // Сила = тоннаж для поиска похожих упражнений
                completedAt = LocalDateTime.now().toString()
            )
            db.fitnessDao().insertSetHistory(history)
        }
    }

    // Функция сохранения и завершения
    fun saveAndFinish() {
        if (hasReplacedExercises) {
            showSaveNewProgramDialog = true
        } else {
            showFinishWorkoutDialog = true
        }
    }
    
    // Функция сохранения новой программы с заменёнными упражнениями
    fun saveAsNewProgram() {
        val newProgram = program.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${program.name} (изменённая)",
            workouts = program.workouts.mapIndexed { workoutIndex, workout ->
                if (workoutIndex == selectedDayIndex) {
                    workout.copy(
                        exercises = workout.exercises.mapIndexed { exerciseIndex, we ->
                            replacedExercises[exerciseIndex]?.let { newExercise ->
                                we.copy(exercise = newExercise)
                            } ?: we
                        }
                    )
                } else {
                    workout
                }
            }
        )
        com.questlife.app.ProgramStore.addProgram(newProgram)
        showSaveNewProgramDialog = false
        showFinishWorkoutDialog = true
    }
    
    // Функция финального сохранения с весом и калориями
    fun finalizeWorkout() {
        val workoutName = currentWorkout.name.ifEmpty { "Тренировка $selectedDayIndex" }
        
        // Создаём список упражнений с учётом замен
        val exercisesWithReplacements = currentWorkout.exercises.mapIndexed { index, we ->
            replacedExercises[index]?.let { newExercise ->
                we.copy(exercise = newExercise)
            } ?: we
        }
        
        val completedExercisesList = exercisesWithReplacements.mapNotNull { we ->
            val result = exerciseResults[we.exercise.id]
            if (result != null && (result.first > 0 || result.second > 0.0)) {
                val volume = result.second * result.first * we.sets // вес * повторы * подходы
                CompletedExercise(
                    exerciseId = we.exercise.id,
                    exerciseName = we.exercise.name,
                    sets = we.sets,
                    reps = result.first,
                    weight = result.second,
                    bestReps = result.first,
                    bestWeight = result.second,
                    totalVolume = volume
                )
            } else null
        }

        // Рассчитываем тоннаж правильно: сумма тоннажа всех упражнений
        val totalVolume = completedExercisesList.sumOf { it.totalVolume }
        
        // Рассчитываем примерное количество калорий на основе упражнений
        val estimatedCalories = completedExercisesList.sumOf { ex ->
            ExerciseDatabase.exercises.find { it.id == ex.exerciseId }?.caloriesPerMinute?.times(ex.sets * ex.reps)?.toInt() ?: 0
        }
        
        val actualCalories = inputCalories.toIntOrNull() ?: estimatedCalories

        val completedWorkout = CompletedWorkout(
            programId = program.id,
            programName = program.name,
            workoutName = workoutName,
            durationMinutes = 0,
            exercises = completedExercisesList,
            totalVolume = totalVolume
        )

        // Сохраняем тренировку с весом и калориями
        com.questlife.app.ProgramStore.addCompletedWorkout(completedWorkout, inputWeight.toDoubleOrNull(), actualCalories)
        
        onFinish()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(program.name) },
                navigationIcon = {
                    IconButton(onClick = onFinish) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Прогресс-бар: количество недель на количество тренировок
            val totalWorkouts = program.workouts.size
            val progressText = if (totalWorkouts > 0) {
                "Неделя $selectedDayIndex из ${program.durationWeeks} | Тренировка ${selectedDayIndex + 1} из $totalWorkouts"
            } else {
                "Неделя 1 из ${program.durationWeeks}"
            }
            
            Text(progressText, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            
            LinearProgressIndicator(
                progress = if (currentWorkout.exercises.isNotEmpty())
                    (completedExercises.toFloat() / currentWorkout.exercises.size) else 0f,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(24.dp))
            
            // Выбор дня тренировки
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("День тренировки: ${selectedDayIndex + 1}", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { showDaySelectorDialog = true }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Выбрать день")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Упражнение ${currentExerciseIndex + 1} из ${currentWorkout.exercises.size}",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    if (currentExercise != null) {
                        val exerciseId = currentExercise.id
                        val savedResult = exerciseResults[exerciseId]

                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val images = listOfNotNull(
                                    currentExercise.imageUrl.takeIf { it.isNotBlank() },
                                    currentExercise.imageUrl2.takeIf { it.isNotBlank() }
                                )

                                if (images.isNotEmpty()) {
                                    var currentImageIndex by remember { mutableStateOf(0) }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = images[currentImageIndex],
                                            contentDescription = currentExercise.name,
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
                                    Spacer(modifier = Modifier.height(12.dp))
                                }

                                Text(currentExercise.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(8.dp))

                                val displayDescription = if (currentExercise.description.isNotBlank()) {
                                    currentExercise.description
                                } else if (currentExercise.instructions.isNotEmpty()) {
                                    currentExercise.instructions.joinToString("\n")
                                } else {
                                    "Описание отсутствует"
                                }
                                Text(displayDescription, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth())

                                if (savedResult != null && (savedResult.first > 0 || savedResult.second > 0)) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceEvenly
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Повторы", style = MaterialTheme.typography.labelSmall)
                                                Text("${savedResult.first}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            }
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Text("Вес (кг)", style = MaterialTheme.typography.labelSmall)
                                                Text("${savedResult.second}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                                
                                // Отображение истории подходов
                                if (setHistory.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("История подходов:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    setHistory.forEachIndexed { index, set ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                            ),
                                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text("Подход #${setHistory.size - index}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    val dateStr = set.completedAt.split("T").firstOrNull() ?: set.completedAt
                                                    Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Вес", style = MaterialTheme.typography.labelSmall)
                                                        Text("${set.weight} кг", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Повторы", style = MaterialTheme.typography.labelSmall)
                                                        Text("${set.reps}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                                    }
                                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                        Text("Тоннаж", style = MaterialTheme.typography.labelSmall)
                                                        Text("${(set.volume / 1000).toInt()}т", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                                    }
                                                }
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedButton(
                                    onClick = { showSimilarExercisesDialog = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Заменить упражнение")
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    if (isResting) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Отдых", style = MaterialTheme.typography.headlineMedium)
                                Text("$restTimeLeft сек", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { isResting = false; restTimeLeft = customRestTime },
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Пропустить отдых") }

                                    OutlinedButton(
                                        onClick = { showRestSettingsDialog = true },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Настроить")
                                    }
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    isResting = true
                                    restTimeLeft = customRestTime
                                    // Автоматически открываем окно внесения результатов
                                    val existingResult = exerciseResults[currentExercise?.id] ?: Pair(0, 0.0)
                                    currentResultInput = existingResult
                                    showResultDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Завершить подход", style = MaterialTheme.typography.titleMedium)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        if (currentExerciseIndex > 0) {
                                            currentExerciseIndex--
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = currentExerciseIndex > 0
                                ) {
                                    Icon(Icons.Default.SkipPrevious, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Пред. упражнение")
                                }

                                Button(
                                    onClick = {
                                        if (currentExerciseIndex < currentWorkout.exercises.size - 1) {
                                            currentExerciseIndex++
                                            isResting = true
                                            restTimeLeft = customRestTime
                                            completedExercises++
                                        } else {
                                            saveAndFinish()
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("След. упражнение")
                                }

                                OutlinedButton(
                                    onClick = { showRestSettingsDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                                ) {
                                    Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Таймер: ${customRestTime}с")
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    val existingResult = exerciseResults[currentExercise?.id] ?: Pair(0, 0.0)
                                    currentResultInput = existingResult
                                    showResultDialog = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Изменить результат", style = MaterialTheme.typography.titleMedium)
                            }

                            OutlinedButton(
                                onClick = { saveAndFinish() },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error,
                                    containerColor = Color.Transparent
                                )
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Завершить тренировку", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                } // Конец Column скролла
            } // Конец Box
        } // Конец Column Scaffold

        // Диалог выбора дня тренировки
        if (showDaySelectorDialog) {
            AlertDialog(
                onDismissRequest = { showDaySelectorDialog = false },
                title = { Text("Выберите день тренировки") },
                text = {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(program.workouts.size) { index ->
                            FilterChip(
                                selected = selectedDayIndex == index,
                                onClick = {
                                    selectedDayIndex = index
                                    currentExerciseIndex = 0
                                    showDaySelectorDialog = false
                                },
                                label = { Text("День ${index + 1}: ${program.workouts[index].name}") },
                                leadingIcon = if (selectedDayIndex == index) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                        }
                    }
                },
                confirmButton = { Button(onClick = { showDaySelectorDialog = false }) { Text("Закрыть") } }
            )
        }
        
        // Диалог завершения тренировки с вводом веса и калорий
        if (showFinishWorkoutDialog) {
            AlertDialog(
                onDismissRequest = { showFinishWorkoutDialog = false },
                title = { Text("Завершение тренировки") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text("Введите данные по завершении тренировки:")
                        OutlinedTextField(
                            value = inputWeight,
                            onValueChange = { inputWeight = it },
                            label = { Text("Вес тела (кг)") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = inputCalories,
                            onValueChange = { inputCalories = it },
                            label = { Text("Калории за тренировку") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                    }
                },
                confirmButton = { Button(onClick = { finalizeWorkout(); showFinishWorkoutDialog = false }) { Text("Сохранить") } },
                dismissButton = { TextButton(onClick = { showFinishWorkoutDialog = false }) { Text("Отмена") } }
            )
        }
        
        // Диалог сохранения новой программы с заменёнными упражнениями
        if (showSaveNewProgramDialog) {
            AlertDialog(
                onDismissRequest = { showSaveNewProgramDialog = false },
                title = { Text("Сохранить новую программу?") },
                text = {
                    Text("Вы заменили одно или несколько упражнений. Хотите сохранить эту конфигурацию как новую программу?")
                },
                confirmButton = { 
                    Button(onClick = { saveAsNewProgram() }) { 
                        Text("Сохранить") 
                    } 
                },
                dismissButton = { 
                    TextButton(onClick = { 
                        showSaveNewProgramDialog = false
                        showFinishWorkoutDialog = true
                    }) { 
                        Text("Пропустить") 
                    } 
                }
            )
        }

        // Диалоги должны быть здесь, внутри Scaffold, но вне Column контента
        if (showRestSettingsDialog) {
            RestTimeSettingsDialog(
                currentTime = customRestTime,
                onDismiss = { showRestSettingsDialog = false },
                onConfirm = { newTime ->
                    customRestTime = newTime
                    restTimeLeft = newTime
                    showRestSettingsDialog = false
                }
            )
        }

        if (showResultDialog && currentExercise != null) {
            ExerciseResultDialog(
                reps = currentResultInput.first,
                weight = currentResultInput.second,
                onDismiss = { showResultDialog = false },
                onSave = { reps, weight ->
                    val sets = currentWorkout.exercises.getOrNull(currentExerciseIndex)?.sets ?: 1
                    exerciseResults = exerciseResults + (currentExercise.id to Pair(reps, weight))
                    // Сохраняем подход в историю сразу после нажатия "Завершить подход"
                    saveSetToHistory(currentExercise.id, currentExercise.name, sets, reps, weight)
                    showResultDialog = false
                }
            )
        }

        if (showSimilarExercisesDialog && currentExercise != null) {
            ExerciseSearchDialog(
                allExercises = ExerciseDatabase.exercises,
                searchQuery = exerciseSearchQuery,
                onSearchQueryChange = { exerciseSearchQuery = it },
                onDismiss = { 
                    showSimilarExercisesDialog = false
                    exerciseSearchQuery = ""
                },
                onExerciseSelected = { selectedExercise ->
                    // Заменяем упражнение только для текущей тренировки (локально)
                    replacedExercises = replacedExercises + (currentExerciseIndex to selectedExercise)
                    hasReplacedExercises = true
                    showSimilarExercisesDialog = false
                    exerciseSearchQuery = ""
                }
            )
        }
    } // Конец Scaffold
}

@Composable
fun ExerciseSearchDialog(
    allExercises: List<Exercise>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    // Показываем все упражнения, фильтруем только по поисковому запросу
    val filteredExercises = remember(searchQuery) {
        if (searchQuery.isEmpty()) {
            allExercises.take(100)
        } else {
            allExercises.filter { exercise ->
                exercise.name.contains(searchQuery, ignoreCase = true) ||
                exercise.muscleGroups.any { it.name.contains(searchQuery, ignoreCase = true) }
            }.take(100)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Выберите упражнение", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    label = { Text("Поиск по названию или мышцам") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Очистить")
                            }
                        }
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Всего упражнений: ${filteredExercises.size}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        text = {
            Column {
                if (filteredExercises.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", style = MaterialTheme.typography.headlineMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Упражнения не найдены", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredExercises) { exercise ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = { onExerciseSelected(exercise) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (exercise.imageUrl.isNotBlank() || exercise.imageUrl2.isNotBlank()) {
                                        val firstImage = exercise.imageUrl.takeIf { it.isNotBlank() } 
                                            ?: exercise.imageUrl2
                                        AsyncImage(
                                            model = firstImage,
                                            contentDescription = exercise.name,
                                            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                    } else {
                                        Box(
                                            modifier = Modifier.size(60.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("🏋️", style = MaterialTheme.typography.headlineSmall)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(
                                            exercise.muscleGroups.joinToString(", ") { it.name },
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Отмена") 
            } 
        }
    )
}

@Composable
fun SimilarExercisesDialog(
    currentExercise: Exercise,
    allExercises: List<Exercise>,
    onDismiss: () -> Unit,
    onExerciseSelected: (Exercise) -> Unit
) {
    // Фильтруем упражнения по тем же целевым мышцам (primaryMuscles через muscleGroups)
    // В нашей модели muscleGroups содержит все мышцы (и primary, и secondary) из exercises.json
    val similarExercises = remember(currentExercise) {
        allExercises.filter { exercise ->
            exercise.id != currentExercise.id &&
            exercise.muscleGroups.any { it in currentExercise.muscleGroups }
        }.sortedByDescending { exercise ->
            // Сортируем по количеству общих мышечных групп
            exercise.muscleGroups.count { it in currentExercise.muscleGroups }
        }.take(15)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Заменить упражнение") },
        text = {
            if (similarExercises.isEmpty()) {
                Text("Похожие упражнения не найдены")
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(similarExercises) { exercise ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onExerciseSelected(exercise) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (exercise.imageUrl.isNotBlank() || exercise.imageUrl2.isNotBlank()) {
                                    val firstImage = exercise.imageUrl.takeIf { it.isNotBlank() } 
                                        ?: exercise.imageUrl2
                                    AsyncImage(
                                        model = firstImage,
                                        contentDescription = exercise.name,
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(8.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(exercise.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Text(
                                        exercise.muscleGroups.joinToString(", ") { it.name },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Закрыть") } },
        dismissButton = { 
            TextButton(onClick = onDismiss) { 
                Text("Отмена") 
            } 
        }
    )
}

@Composable
fun ExerciseResultDialog(
    reps: Int,
    weight: Double,
    onDismiss: () -> Unit,
    onSave: (Int, Double) -> Unit
) {
    var inputReps by remember { mutableStateOf(reps) }
    var inputWeight by remember { mutableStateOf(weight) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Результат упражнения") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Запишите ваши результаты:")
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Повторы:", modifier = Modifier.width(100.dp))
                    OutlinedTextField(
                        value = if (inputReps == 0) "" else inputReps.toString(),
                        onValueChange = { inputReps = it.toIntOrNull() ?: 0 },
                        label = { Text("кол-во") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Вес (кг):", modifier = Modifier.width(100.dp))
                    OutlinedTextField(
                        value = if (inputWeight == 0.0) "" else inputWeight.toString(),
                        onValueChange = { inputWeight = it.toDoubleOrNull() ?: 0.0 },
                        label = { Text("кг") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
                Text("Быстрый выбор повторов:", style = MaterialTheme.typography.labelSmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(5, 8, 10, 12, 15, 20).forEach { r ->
                        FilterChip(selected = inputReps == r, onClick = { inputReps = r }, label = { Text("$r") })
                    }
                }
            }
        },
        confirmButton = { Button(onClick = { onSave(inputReps, inputWeight) }) { Text("Сохранить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}

@Composable
fun RestTimeSettingsDialog(
    currentTime: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selectedTime by remember { mutableStateOf(currentTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Время отдыха (секунды)") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Выберите длительность отдыха:")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    listOf(30, 60, 90, 120).forEach { time ->
                        FilterChip(
                            selected = selectedTime == time,
                            onClick = { selectedTime = time },
                            label = { Text("$time") },
                            leadingIcon = if (selectedTime == time) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                Divider()
                Column {
                    Text("Или выберите точно: $selectedTime сек")
                    Slider(
                        value = selectedTime.toFloat(),
                        onValueChange = { selectedTime = it.toInt() },
                        valueRange = 15f..300f,
                        steps = (300 - 15) / 15 - 1
                    )
                }
            }
        },
        confirmButton = { Button(onClick = { onConfirm(selectedTime) }) { Text("Применить") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Отмена") } }
    )
}