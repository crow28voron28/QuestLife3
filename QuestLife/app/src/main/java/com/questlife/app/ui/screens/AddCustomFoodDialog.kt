package com.questlife.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomFoodDialog(
    onDismiss: () -> Unit,
    onFoodCreated: (Food) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var proteins by remember { mutableStateOf("0") }
    var fats by remember { mutableStateOf("0") }
    var carbs by remember { mutableStateOf("0") }
    var servingSize by remember { mutableStateOf("100") }
    var category by remember { mutableStateOf(FoodCategory.OTHER) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать свой продукт") },
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
                    label = { Text("Название продукта") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = proteins,
                    onValueChange = { proteins = it },
                    label = { Text("Белки (г на 100 г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = fats,
                    onValueChange = { fats = it },
                    label = { Text("Жиры (г на 100 г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Углеводы (г на 100 г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = servingSize,
                    onValueChange = { servingSize = it },
                    label = { Text("Размер порции (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.trim().isBlank()) return@TextButton

                    val p = proteins.toDoubleOrNull() ?: 0.0
                    val f = fats.toDoubleOrNull() ?: 0.0
                    val c = carbs.toDoubleOrNull() ?: 0.0
                    val s = servingSize.toDoubleOrNull() ?: 100.0

                    val newFood = Food(
                        name = name.trim().ifBlank { "Без названия" },
                        macros = Macros(p, f, c),
                        servingSize = s,
                        category = category,
                        isCustom = true
                    )

                    onFoodCreated(newFood)   // ← передаём наверх, там сохранится
                    onDismiss()               // ← закрываем диалог
                }
            ) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}