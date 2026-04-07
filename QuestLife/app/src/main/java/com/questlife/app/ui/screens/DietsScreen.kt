package com.questlife.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.questlife.app.models.Diet
import com.questlife.app.models.Recipe
import com.questlife.app.models.Food
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.questlife.app.api.ApiClient
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DietsData {
    fun loadFoodsFromAssets(context: Context): List<Food> {
        return try {
            val inputStream = context.assets.open("foods.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val json = String(buffer, Charsets.UTF_8)

            val type = object : TypeToken<List<Food>>() {}.type
            Gson().fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DietsScreen(onBackClick: () -> Unit, onDietClick: (Diet) -> Unit) {
    val context = LocalContext.current
    var diets by remember { mutableStateOf<List<Diet>>(emptyList()) }
    var selectedDiet by remember { mutableStateOf<Diet?>(null) }
    var allRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var allFoods by remember { mutableStateOf<List<Food>>(emptyList()) }
    
    // Загружаем только продукты из assets, диеты и рецепты берем из TheMealDB
    LaunchedEffect(Unit) {
        allFoods = DietsData.loadFoodsFromAssets(context)
        // Создаем пустой список диет (можно добавить загрузку из сети если нужно)
        diets = emptyList()
    }
    
    // Функция для загрузки рецептов из TheMealDB
    val scope = rememberCoroutineScope()
    fun loadRecipesFromTheMealDb(query: String = "") {
        scope.launch {
            try {
                val response = ApiClient.theMealDbApi.searchRecipes(query.ifEmpty { "chicken" })
                val meals = response.meals ?: emptyList()
                allRecipes = meals.map { meal -> meal.toRecipe() }
            } catch (e: Exception) {
                e.printStackTrace()
                allRecipes = emptyList()
            }
        }
    }
    
    // Загружаем примеры рецептов при старте
    LaunchedEffect(Unit) {
        loadRecipesFromTheMealDb("chicken")
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Диеты") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(diets) { diet ->
                DietCard(diet = diet, onClick = { selectedDiet = diet })
            }
        }
    }
    
    // Диалог с деталями диеты
    selectedDiet?.let { diet ->
        DietDetailDialog(
            diet = diet,
            allRecipes = allRecipes,
            allFoods = allFoods,
            onDismiss = { selectedDiet = null }
        )
    }
}

@Composable
fun DietCard(diet: Diet, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = diet.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = diet.description ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "${diet.targetCalories} ккал",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Text(
                    "Б: ${diet.proteinRatio}% | Ж: ${diet.fatRatio}% | У: ${diet.carbRatio}%",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            
            if (diet.allowedFoodIds.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Разрешено:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        diet.allowedFoodIds.take(5).forEach { foodId ->
                            AssistChip(
                                onClick = { },
                                label = { Text(foodId, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                        if (diet.allowedFoodIds.size > 5) {
                            Text("+${diet.allowedFoodIds.size - 5}", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            
            if (diet.recipeExamples.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Примеры рецептов:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        diet.recipeExamples.take(3).forEach { recipeId ->
                            AssistChip(
                                onClick = { },
                                label = { Text(recipeId, style = MaterialTheme.typography.labelSmall) },
                                leadingIcon = {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DietDetailDialog(diet: Diet, allRecipes: List<Recipe>, allFoods: List<Food>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(diet.name, style = MaterialTheme.typography.titleLarge) },
        text = {
            LazyColumn {
                // Описание
                if (!diet.description.isNullOrBlank()) {
                    item {
                        Text("Описание:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(diet.description!!, style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Целевые показатели
                item {
                    Text("Целевые показатели:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        MacroChip(label = "Ккал", value = "${diet.targetCalories}")
                        MacroChip(label = "Б", value = "${diet.proteinRatio}%")
                        MacroChip(label = "Ж", value = "${diet.fatRatio}%")
                        MacroChip(label = "У", value = "${diet.carbRatio}%")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Разрешенные продукты
                if (diet.allowedFoodIds.isNotEmpty()) {
                    item {
                        Text("Разрешенные продукты:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val allowedFoods = allFoods.filter { it.id in diet.allowedFoodIds }
                    items(allowedFoods.size) { index ->
                        val food = allowedFoods[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(food.name)
                            Text("${food.macros.calories.toInt()} ккал / 100г")
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
                
                // Запрещенные продукты
                if (diet.forbiddenFoodIds.isNotEmpty()) {
                    item {
                        Text("Запрещенные продукты:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val forbiddenFoods = allFoods.filter { it.id in diet.forbiddenFoodIds }
                    items(forbiddenFoods.size) { index ->
                        val food = forbiddenFoods[index]
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(food.name, color = MaterialTheme.colorScheme.error)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
                
                // Примеры рецептов
                if (diet.recipeExamples.isNotEmpty()) {
                    item {
                        Text("Примеры рецептов:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    val exampleRecipes = allRecipes.filter { diet.recipeExamples.contains(it.id.toString()) }
                    items(exampleRecipes.size) { index ->
                        val recipe = exampleRecipes[index]
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(recipe.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("${recipe.calories.toInt()} ккал", style = MaterialTheme.typography.bodySmall)
                                    Text("Б:${recipe.protein} Ж:${recipe.fat} У:${recipe.carbohydrates}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
                
                // Советы
                if (diet.tips.isNotEmpty()) {
                    item {
                        Text("Советы:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(diet.tips) { index: Int, tip: String ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = tip)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        }
    )
}
