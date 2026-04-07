package com.questlife.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.util.Log
import androidx.lifecycle.viewmodel.compose.viewModel
import com.questlife.app.NutritionViewModel
import com.questlife.app.data.FoodDatabase
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    viewModel: NutritionViewModel = viewModel(),
    onNavigateToRecipes: () -> Unit = {},
    onNavigateToDiets: () -> Unit = {}
) {
    var showAddFoodDialog by remember { mutableStateOf(false) }
    var showCreateFoodDialog by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<FoodCategory?>(null) }
    
    val dailyNutrition by viewModel.dailyNutrition.collectAsState()
    val foods by viewModel.foods.collectAsState()
    
    // Фильтрация по категории
    val filteredFoods = remember(selectedCategory, foods) {
        if (selectedCategory == null) {
            foods
        } else {
            foods.filter { it.category == selectedCategory }
        }
    }
    
    // Логирование количества продуктов
    LaunchedEffect(foods.size) {
        Log.d("NutritionScreen", "Загружено продуктов: ${foods.size}")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Питание") },
                actions = {
                    IconButton(onClick = { /* Настройки */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Настройки")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.End
            ) {
                SmallFloatingActionButton(
                    onClick = onNavigateToRecipes,
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Icon(Icons.Default.RestaurantMenu, contentDescription = "Рецепты")
                }
                SmallFloatingActionButton(
                    onClick = onNavigateToDiets,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.FitnessCenter, contentDescription = "Диеты")
                }
                SmallFloatingActionButton(
                    onClick = { showCreateFoodDialog = true },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = "Создать продукт")
                }
                FloatingActionButton(onClick = { showAddFoodDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Добавить приём пищи")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Статистика за день
            item { DailyStatsHeader(dailyNutrition) }
            
            // Прогресс БЖУ
            item { MacrosProgressCard(dailyNutrition) }
            
            // Категории продуктов (горизонтальный скролл)
            item {
                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it }
                )
            }
            
            // Заголовок съеденного
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Сегодня съедено", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        "${dailyNutrition.meals.size} приём(ов)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Приёмы пищи по типам
            MealType.values().forEach { mealType ->
                val meals = dailyNutrition.meals.filter { it.mealType == mealType }
                if (meals.isNotEmpty()) {
                    item {
                        MealTypeSection(mealType, meals) { meal ->
                            viewModel.removeMeal(meal)
                        }
                    }
                }
            }

            if (dailyNutrition.meals.isEmpty()) {
                item { EmptyNutritionState() }
            }
            
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddFoodDialog) {
        val isLoading by viewModel.isLoading.collectAsState()
        Log.d("NutritionScreen", "Диалог добавления: продуктов=${foods.size}, загрузка=$isLoading")
        AddFoodDialog(
            foods = foods, // Передаем ВСЕ продукты, а не отфильтрованные
            isLoading = isLoading,
            onDismiss = { showAddFoodDialog = false },
            onFoodAdded = {
                viewModel.addMeal(it)
                showAddFoodDialog = false
            }
        )
    }

    if (showCreateFoodDialog) {
        AddCustomFoodDialog(
            onDismiss = { showCreateFoodDialog = false },
            onFoodCreated = { newFood ->
                val safeFood = newFood.copy(
                    name = newFood.name?.trim()?.ifBlank { "Без названия" } ?: "Без названия"
                )
                FoodDatabase.addCustomFood(safeFood)
                viewModel.refreshFoods()
                showCreateFoodDialog = false
            }
        )
    }
}

// ==================== DailyStatsHeader (новая статистика за день) ====================
@Composable
fun DailyStatsHeader(dailyNutrition: DailyNutrition) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Статистика за сегодня", 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Круговой прогресс калорий
            Box(modifier = Modifier.size(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 
                        (dailyNutrition.totalCalories.toFloat() / dailyNutrition.targetCalories).coerceIn(0f, 1.5f) 
                    },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 16.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = when {
                        dailyNutrition.totalCalories <= dailyNutrition.targetCalories * 0.8 -> Color(0xFF4CAF50)
                        dailyNutrition.totalCalories <= dailyNutrition.targetCalories -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "${dailyNutrition.totalCalories}",
                        style = MaterialTheme.typography.displayLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "из ${dailyNutrition.targetCalories} ккал",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    
                    val remaining = dailyNutrition.targetCalories - dailyNutrition.totalCalories
                    Text(
                        if (remaining >= 0) "Осталось: $remaining" else "Превышение: ${-remaining}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (remaining >= 0) 
                            MaterialTheme.colorScheme.onPrimaryContainer 
                        else 
                            Color(0xFFF44336)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Дополнительная статистика
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                NutritionStatItem("Приёмов пищи", "${dailyNutrition.meals.size}")
                NutritionStatItem("Белки", "${dailyNutrition.totalMacros.proteins.toInt()}г")
                NutritionStatItem("Жиры", "${dailyNutrition.totalMacros.fats.toInt()}г")
                NutritionStatItem("Углеводы", "${dailyNutrition.totalMacros.carbohydrates.toInt()}г")
            }
        }
    }
}

@Composable
fun NutritionStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ==================== CategoryFilterRow (фильтр по категориям) ====================
@Composable
fun CategoryFilterRow(
    selectedCategory: FoodCategory?,
    onCategorySelected: (FoodCategory?) -> Unit
) {
    val categories = listOf(
        null to "Все",
        FoodCategory.PROTEIN to "🥩 Белки",
        FoodCategory.DAIRY to "🥛 Молочное",
        FoodCategory.GRAINS to "🌾 Крупы",
        FoodCategory.VEGETABLES to "🥬 Овощи",
        FoodCategory.FRUITS to "🍎 Фрукты",
        FoodCategory.SWEETS to "🍬 Сладости",
        FoodCategory.BEVERAGES to "🥤 Напитки",
        FoodCategory.OTHER to "📝 Другое"
    )
    
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(categories) { (category, label) ->
            FilterChip(
                selected = selectedCategory == category,
                onClick = { onCategorySelected(category) },
                label = { Text(label) },
                leadingIcon = if (category != null) {
                    @Composable {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else null
            )
        }
    }
}

// ==================== CaloriesSummaryCard ====================
@Composable
fun CaloriesSummaryCard(dailyNutrition: DailyNutrition) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Калории", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { (dailyNutrition.totalCalories.toFloat() / dailyNutrition.targetCalories).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 12.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${dailyNutrition.totalCalories}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        "из ${dailyNutrition.targetCalories}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text("ккал", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

// ==================== MacrosProgressCard ====================
@Composable
fun MacrosProgressCard(dailyNutrition: DailyNutrition) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("БЖУ (макронутриенты)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            MacroProgressBar("Белки", "🥩", dailyNutrition.totalMacros.proteins, dailyNutrition.targetMacros.proteins, "г", Color(0xFFE91E63))
            MacroProgressBar("Жиры", "🥑", dailyNutrition.totalMacros.fats, dailyNutrition.targetMacros.fats, "г", Color(0xFFFFC107))
            MacroProgressBar("Углеводы", "🍞", dailyNutrition.totalMacros.carbohydrates, dailyNutrition.targetMacros.carbohydrates, "г", Color(0xFF2196F3))
        }
    }
}

@Composable
fun MacroProgressBar(name: String, emoji: String, current: Double, target: Double, unit: String, color: Color) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(emoji, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(name, style = MaterialTheme.typography.bodyLarge)
            }
            Text("${current.toInt()} / ${target.toInt()} $unit", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (current / target).toFloat().coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)),
            color = color,
            trackColor = color.copy(alpha = 0.2f)
        )
    }
}

// ==================== MealTypeSection ====================
@Composable
fun MealTypeSection(
    mealType: MealType,
    meals: List<MealEntry>,
    onDeleteMeal: (MealEntry) -> Unit
) {
    val (icon, name) = when (mealType) {
        MealType.BREAKFAST -> "🌅" to "Завтрак"
        MealType.LUNCH -> "☀️" to "Обед"
        MealType.DINNER -> "🌙" to "Ужин"
        MealType.SNACK -> "🍎" to "Перекус"
    }

    val totalCalories = meals.sumOf { (it.food.macros.calories * it.amount / it.food.servingSize).toInt() }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(icon, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Text("${totalCalories} ккал", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            Spacer(modifier = Modifier.height(12.dp))

            meals.forEach { meal ->
                MealEntryCard(meal, onDeleteMeal)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MealEntryCard(meal: MealEntry, onDelete: (MealEntry) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = meal.food.name.ifBlank { "Без названия" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${meal.amount.toInt()} ${meal.amountUnit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Б: ${meal.food.macros.proteins.toInt()}г", style = MaterialTheme.typography.labelSmall)
                    Text("Ж: ${meal.food.macros.fats.toInt()}г", style = MaterialTheme.typography.labelSmall)
                    Text("У: ${meal.food.macros.carbohydrates.toInt()}г", style = MaterialTheme.typography.labelSmall)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "${(meal.food.macros.calories * meal.amount / meal.food.servingSize).toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text("ккал", style = MaterialTheme.typography.labelSmall)
            }

            IconButton(onClick = { onDelete(meal) }) {
                Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun EmptyNutritionState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🍽️", style = MaterialTheme.typography.displayLarge)
        Spacer(modifier = Modifier.height(16.dp))
        Text("Начните отслеживать питание", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Добавьте первый прием пищи!", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}