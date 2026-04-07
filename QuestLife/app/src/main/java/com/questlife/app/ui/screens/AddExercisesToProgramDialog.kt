package com.questlife.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExercisesToProgramDialog(
    exercises: List<Exercise>,
    program: WorkoutProgram,
    workoutIndex: Int = 0,
    onDismiss: () -> Unit,
    onExercisesAdded: (List<WorkoutExercise>) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val selectedExercises = remember { mutableStateListOf<Pair<Exercise, WorkoutExercise>>() }
    
    val filteredExercises = exercises.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
        it.description.contains(searchQuery, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить упражнения") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Программа: ${program.name}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск упражнений") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    "Выбрано: ${selectedExercises.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(filteredExercises) { exercise ->
                        val selectedIndex = selectedExercises.indexOfFirst { it.first.id == exercise.id }
                        val isSelected = selectedIndex != -1
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isSelected) {
                                        selectedExercises.removeAt(selectedIndex)
                                    } else {
                                        selectedExercises.add(exercise to WorkoutExercise(exercise = exercise, sets = 3, reps = 10, weight = 0.0))
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) 
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) 
                                else 
                                    MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isSelected,
                                    onCheckedChange = {
                                        if (it) {
                                            selectedExercises.add(exercise to WorkoutExercise(exercise = exercise, sets = 3, reps = 10, weight = 0.0))
                                        } else {
                                            val idx = selectedExercises.indexOfFirst { pair -> pair.first.id == exercise.id }
                                            if (idx != -1) selectedExercises.removeAt(idx)
                                        }
                                    }
                                )
                                
                                if (exercise.imageUrl.isNotBlank() || exercise.imageUrl2.isNotBlank()) {
                                    val firstImage = exercise.imageUrl.takeIf { it.isNotBlank() } 
                                        ?: exercise.imageUrl2
                                    AsyncImage(
                                        model = firstImage,
                                        contentDescription = exercise.name,
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp)),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                } else {
                                    Box(
                                        modifier = Modifier.size(50.dp).clip(RoundedCornerShape(6.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("🏋️", style = MaterialTheme.typography.headlineMedium)
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                }
                                
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        exercise.name,
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        exercise.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2
                                    )
                                }
                                
                                if (isSelected) {
                                    val workoutEx = selectedExercises.first { it.first.id == exercise.id }.second
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = workoutEx.sets.toString(),
                                            onValueChange = { 
                                                val newSets = it.toIntOrNull() ?: 3
                                                val idx = selectedExercises.indexOfFirst { pair -> pair.first.id == exercise.id }
                                                if (idx != -1) {
                                                    selectedExercises[idx] = selectedExercises[idx].first to workoutEx.copy(sets = newSets)
                                                }
                                            },
                                            label = { Text("Подх.") },
                                            modifier = Modifier.width(80.dp),
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )
                                        OutlinedTextField(
                                            value = workoutEx.reps.toString(),
                                            onValueChange = { 
                                                val newReps = it.toIntOrNull() ?: 10
                                                val idx = selectedExercises.indexOfFirst { pair -> pair.first.id == exercise.id }
                                                if (idx != -1) {
                                                    selectedExercises[idx] = selectedExercises[idx].first to workoutEx.copy(reps = newReps)
                                                }
                                            },
                                            label = { Text("Повт.") },
                                            modifier = Modifier.width(80.dp),
                                            textStyle = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (selectedExercises.isNotEmpty()) {
                        onExercisesAdded(selectedExercises.map { it.second })
                    } else {
                        onDismiss()
                    }
                },
                enabled = selectedExercises.isNotEmpty()
            ) {
                Text("Добавить (${selectedExercises.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
