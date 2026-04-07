package com.questlife.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.questlife.app.data.FoodDatabase
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodDialog(
    foods: List<Food>,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onFoodAdded: (MealEntry) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedFood by remember { mutableStateOf<Food?>(null) }
    var amount by remember { mutableStateOf("100") }
    var selectedMealType by remember { mutableStateOf(MealType.BREAKFAST) }
    
    val searchResults = remember(searchQuery, foods) {
        if (searchQuery.isEmpty()) {
            foods.take(537) // Показываем все продукты из базы
        } else {
            foods.filter { it.name.contains(searchQuery, ignoreCase = true) }.take(537)
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f),
        title = { Text("Добавить прием пищи") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Выбор типа приема пищи
                Text("Тип приема пищи", style = MaterialTheme.typography.labelLarge)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MealType.values().forEach { mealType ->
                        val (icon, name) = when (mealType) {
                            MealType.BREAKFAST -> "🌅" to "Завтрак"
                            MealType.LUNCH -> "☀️" to "Обед"
                            MealType.DINNER -> "🌙" to "Ужин"
                            MealType.SNACK -> "🍎" to "Перекус"
                        }
                        
                        FilterChip(
                            selected = selectedMealType == mealType,
                            onClick = { selectedMealType = mealType },
                            label = { Text("$icon") }
                        )
                    }
                }
                
                // Поиск продукта
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Поиск продукта") },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Список продуктов
                if (selectedFood == null) {
                    Text("Выберите продукт:", style = MaterialTheme.typography.labelMedium)
                    
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Загрузка продуктов...")
                            }
                        }
                    } else if (foods.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Нет продуктов в базе")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(searchResults) { food ->
                                FoodSearchItem(
                                    food = food,
                                    onClick = { selectedFood = food }
                                )
                            }
                            
                            if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Ничего не найдено по запросу \"$searchQuery\"")
                                    }
                                }
                            }
                            
                            if (searchResults.size == 537 && searchQuery.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxWidth().padding(top = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Показано все $searchResults.size продуктов из базы",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            } else if (searchResults.size >= 537 && searchQuery.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxWidth().padding(top = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Найдено более 537 продуктов. Уточните поиск.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Детали выбранного продукта
                    SelectedFoodDetails(
                        food = selectedFood!!,
                        amount = amount,
                        onAmountChange = { amount = it },
                        onClear = { selectedFood = null }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    selectedFood?.let { food ->
                        val amountValue = amount.toDoubleOrNull() ?: 100.0
                        onFoodAdded(
                            MealEntry(
                                food = food,
                                amount = amountValue,
                                amountUnit = food.servingUnit,
                                mealType = selectedMealType
                            )
                        )
                    }
                },
                enabled = selectedFood != null && amount.toDoubleOrNull() != null
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

@Composable
fun FoodSearchItem(food: Food, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    food.name.orEmpty(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${food.servingSize.toInt()} ${food.servingUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                Text(
                    "${food.macros.calories.toInt()} ккал",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Б:${food.macros.proteins.toInt()} Ж:${food.macros.fats.toInt()} У:${food.macros.carbohydrates.toInt()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SelectedFoodDetails(
    food: Food,
    amount: String,
    onAmountChange: (String) -> Unit,
    onClear: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    food.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, "Отменить выбор")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = amount,
                onValueChange = onAmountChange,
                label = { Text("Количество (${food.servingUnit})") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Расчет макросов для указанной порции
            val amountValue = amount.toDoubleOrNull() ?: food.servingSize
            val multiplier = amountValue / food.servingSize
            val calculatedMacros = Macros(
                proteins = food.macros.proteins * multiplier,
                fats = food.macros.fats * multiplier,
                carbohydrates = food.macros.carbohydrates * multiplier
            )
            
            Text("Пищевая ценность:", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                NutrientColumn("Калории", "${calculatedMacros.calories.toInt()}", "ккал")
                NutrientColumn("Белки", "${calculatedMacros.proteins.toInt()}", "г")
                NutrientColumn("Жиры", "${calculatedMacros.fats.toInt()}", "г")
                NutrientColumn("Углеводы", "${calculatedMacros.carbohydrates.toInt()}", "г")
            }
        }
    }
}

@Composable
fun NutrientColumn(label: String, value: String, unit: String) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCustomFoodDialog(
    onDismiss: () -> Unit,
    onFoodCreated: (Food) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("100") }
    var proteins by remember { mutableStateOf("") }
    var fats by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(FoodCategory.OTHER) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxHeight(0.9f),
        title = { Text("Создать свое блюдо") },
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
                    label = { Text("Название блюда *") },
                    placeholder = { Text("Например: Овсянка с бананом") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (опционально)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )
                
                OutlinedTextField(
                    value = servingSize,
                    onValueChange = { servingSize = it },
                    label = { Text("Размер порции (г) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Divider()
                
                Text(
                    "Пищевая ценность на ${servingSize.toDoubleOrNull()?.toInt() ?: 100}г:",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                
                OutlinedTextField(
                    value = proteins,
                    onValueChange = { proteins = it },
                    label = { Text("Белки (г) *") },
                    leadingIcon = { Text("🥩") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = fats,
                    onValueChange = { fats = it },
                    label = { Text("Жиры (г) *") },
                    leadingIcon = { Text("🥑") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text("Углеводы (г) *") },
                    leadingIcon = { Text("🍞") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Расчет калорий
                val calculatedCalories = run {
                    val p = proteins.toDoubleOrNull() ?: 0.0
                    val f = fats.toDoubleOrNull() ?: 0.0
                    val c = carbs.toDoubleOrNull() ?: 0.0
                    (p * 4 + f * 9 + c * 4).toInt()
                }
                
                if (calculatedCalories > 0) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            "Калорийность: $calculatedCalories ккал",
                            modifier = Modifier.padding(12.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Divider()
                
                Text("Категория:", style = MaterialTheme.typography.labelLarge)
                
                val categories = listOf(
                    FoodCategory.BREAKFAST to "🌅 Завтрак",
                    FoodCategory.LUNCH to "☀️ Обед",
                    FoodCategory.DINNER to "🌙 Ужин",
                    FoodCategory.SNACK to "🍎 Перекус",
                    FoodCategory.OTHER to "📝 Другое"
                )
                
                categories.forEach { (category, label) ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        RadioButton(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category }
                        )
                        TextButton(
                            onClick = { selectedCategory = category },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val food = Food(
                        name = name,
                        description = description,
                        macros = Macros(
                            proteins = proteins.toDoubleOrNull() ?: 0.0,
                            fats = fats.toDoubleOrNull() ?: 0.0,
                            carbohydrates = carbs.toDoubleOrNull() ?: 0.0
                        ),
                        servingSize = servingSize.toDoubleOrNull() ?: 100.0,
                        category = selectedCategory,
                        isCustom = true
                    )
                    onFoodCreated(food)
                },
                enabled = name.isNotBlank() && 
                         servingSize.toDoubleOrNull() != null &&
                         proteins.toDoubleOrNull() != null &&
                         fats.toDoubleOrNull() != null &&
                         carbs.toDoubleOrNull() != null
            ) {
                Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
