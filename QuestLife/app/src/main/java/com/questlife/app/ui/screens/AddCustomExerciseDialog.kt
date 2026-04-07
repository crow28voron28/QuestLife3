package com.questlife.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.questlife.app.data.ExerciseDatabase  // ← добавлен импорт!
import com.questlife.app.models.*
import androidx.compose.foundation.text.KeyboardOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomExerciseDialog(
    onDismiss: () -> Unit,
    onSave: (Exercise) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExerciseCategory.STRENGTH) }
    var equipment by remember { mutableStateOf(ExerciseEquipment.NONE) }
    var difficulty by remember { mutableStateOf(ExerciseDifficulty.INTERMEDIATE) }
    var caloriesPerMin by remember { mutableStateOf("6.0") }
    var imageUrl1 by remember { mutableStateOf("") }
    var imageUrl2 by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить упражнение") },
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
                    label = { Text("Название упражнения") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание / техника выполнения") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 5
                )

                OutlinedTextField(
                    value = caloriesPerMin,
                    onValueChange = { caloriesPerMin = it },
                    label = { Text("Калорий в минуту") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = imageUrl1,
                    onValueChange = { imageUrl1 = it },
                    label = { Text("URL первого изображения (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                OutlinedTextField(
                    value = imageUrl2,
                    onValueChange = { imageUrl2 = it },
                    label = { Text("URL второго изображения (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isBlank()) return@TextButton  // защита от пустого имени

                    val safeExercise = Exercise(
                        id = java.util.UUID.randomUUID().toString(),
                        name = name.trim(),
                        description = description.trim(),
                        category = category,
                        equipment = equipment,
                        difficulty = difficulty,
                        muscleGroups = emptyList(), // можно потом добавить выбор
                        caloriesPerMinute = caloriesPerMin.toDoubleOrNull() ?: 6.0,
                        instructions = listOf(description.take(150)),
                        imageUrl = imageUrl1.trim(),
                        imageUrl2 = imageUrl2.trim()
                    )

                    ExerciseDatabase.addCustomExercise(safeExercise)  // ← теперь работает
                    onSave(safeExercise)  // ← передаём наверх, если нужно
                    onDismiss()  // ← закрываем диалог
                }
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}