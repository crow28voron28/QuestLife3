package com.questlife.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.questlife.app.data.ExerciseDatabase
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProgramDialog(
    onDismiss: () -> Unit,
    onProgramCreated: (WorkoutProgram) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var durationWeeks by remember { mutableStateOf("4") }
    var daysPerWeek by remember { mutableStateOf("3") }
    val workouts = remember { mutableStateListOf<Workout>() }
    var showExerciseSelector by remember { mutableStateOf(false) }
    var currentWorkoutIndex by remember { mutableStateOf(-1) }
    
    val allExercises = ExerciseDatabase.exercises

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать программу") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название программы") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                OutlinedTextField(
                    value = durationWeeks,
                    onValueChange = { durationWeeks = it },
                    label = { Text("Длительность (недель)") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = daysPerWeek,
                    onValueChange = { daysPerWeek = it },
                    label = { Text("Дней в неделю") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Тренировки:", style = MaterialTheme.typography.titleMedium)
                
                workouts.forEachIndexed { index, workout ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("День ${index + 1}: ${workout.name}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                IconButton(onClick = {
                                    workouts.removeAt(index)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Удалить тренировку", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                            Text("${workout.exercises.size} упражнений", style = MaterialTheme.typography.bodySmall)
                            
                            if (workout.exercises.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(8.dp))
                                workout.exercises.forEach { we ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(we.exercise.name, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        Row {
                                            Text("${we.sets}x${we.reps}", style = MaterialTheme.typography.labelSmall)
                                            if (we.weight > 0) {
                                                Text(" @ ${we.weight}кг", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            TextButton(
                                onClick = {
                                    currentWorkoutIndex = index
                                    showExerciseSelector = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить упражнения")
                            }
                        }
                    }
                }
                
                Button(
                    onClick = {
                        workouts.add(Workout(name = "День ${workouts.size + 1}", description = "", exercises = emptyList()))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Добавить день тренировки")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isBlank()) return@TextButton

                    val newProgram = WorkoutProgram(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        description = description.trim(),
                        goal = "GET_FIT",
                        durationWeeks = durationWeeks.toIntOrNull() ?: 4,
                        daysPerWeek = daysPerWeek.toIntOrNull() ?: 3,
                        workouts = workouts.toList()
                    )

                    onProgramCreated(newProgram)
                    onDismiss()
                },
                enabled = workouts.isNotEmpty()
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
    
    if (showExerciseSelector && currentWorkoutIndex >= 0 && currentWorkoutIndex < workouts.size) {
        AddExercisesToProgramDialog(
            exercises = allExercises,
            program = WorkoutProgram(name = name),
            workoutIndex = currentWorkoutIndex,
            onDismiss = { 
                showExerciseSelector = false
                currentWorkoutIndex = -1
            },
            onExercisesAdded = { newExercises ->
                val currentWorkout = workouts[currentWorkoutIndex]
                workouts[currentWorkoutIndex] = currentWorkout.copy(
                    exercises = currentWorkout.exercises + newExercises
                )
                showExerciseSelector = false
                currentWorkoutIndex = -1
            }
        )
    }
}
