package com.questlife.app.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.questlife.app.api.ApiClient
import com.questlife.app.data.RussianRecipesDatabase
import com.questlife.app.data.RecipeLink
import com.questlife.app.models.Meal
import com.questlife.app.models.Recipe
import com.questlife.app.repository.Repositories
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.ConnectException
import java.net.UnknownHostException

/**
 * Преобразование Meal из TheMealDB в локальную модель Recipe
 */
fun Meal.toRecipe(): Recipe {
    return Recipe(
        id = idMeal.toIntOrNull() ?: 0,
        name = strMeal,
        ingredients = getIngredientsWithMeasures().map { 
            com.questlife.app.models.Ingredient(foodId = it, amount = 1f, unit = "") 
        },
        instructions = strInstructions.split("\r\n", "\n").filter { it.isNotBlank() },
        prepTimeMinutes = 10,
        cookTimeMinutes = 30,
        calories = 0.0,
        protein = 0.0,
        fat = 0.0,
        carbohydrates = 0.0,
        tags = strTags?.split(",")?.map { it.trim() } ?: emptyList(),
        imageUrl = strMealThumb,
        description = strArea
    )
}

/**
 * Преобразование RecipeLink из русской базы в Recipe
 * Извлекает информацию из URL
 */
fun RecipeLink.toRecipeStub(): Recipe {
    // Извлекаем название рецепта из URL
    // Пример URL: https://vkuso.ru/recipe/110086-kurinye-bedra-s-syrom-i-pomidorami-na-kabachkax/
    val recipeName = extractRecipeNameFromUrl(url)
    
    return Recipe(
        id = id.toInt(),
        name = recipeName,
        description = "Русский рецепт",
        ingredients = emptyList(),
        instructions = listOf("Полный рецепт доступен по ссылке: $url"),
        prepTimeMinutes = 30,
        cookTimeMinutes = 30,
        calories = 0.0,
        protein = 0.0,
        fat = 0.0,
        carbohydrates = 0.0,
        tags = listOf("Русская кухня"),
        imageUrl = "" // Картинки нет в базе ссылок
    )
}

/**
 * Извлечение названия рецепта из URL
 * Пример: https://vkuso.ru/recipe/110086-kurinye-bedra-s-syrom -> "Kurinye bedra s syrom"
 */
fun extractRecipeNameFromUrl(url: String): String {
    return try {
        // Получаем последнюю часть URL перед слэшем
        val lastPart = url.trimEnd('/').substringAfterLast('/')
        
        // Удаляем ID в начале (цифры и дефис после них)
        val withoutId = lastPart.replace(Regex("^\\d+-"), "")
        
        // Заменяем дефисы на пробелы
        val withSpaces = withoutId.replace('-', ' ')
        
        // Убираем расширение .html если есть
        val withoutExtension = withSpaces.removeSuffix(".html")
        
        // Делаем первую букву заглавной
        if (withoutExtension.isNotEmpty()) {
            withoutExtension[0].uppercaseChar() + withoutExtension.substring(1)
        } else {
            "Рецепт"
        }
    } catch (e: Exception) {
        "Рецепт"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipesScreen(onBackClick: () -> Unit, onRecipeClick: (Recipe) -> Unit) {
    var apiRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var russianRecipes by remember { mutableStateOf<List<Recipe>>(emptyList()) }
    var selectedRecipe by remember { mutableStateOf<Recipe?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // Инициализация русской базы данных при запуске
    LaunchedEffect(Unit) {
        RussianRecipesDatabase.initialize(context)
        // Загружаем случайные рецепты из TheMealDB при старте вместо русской базы
        isLoading = true
        errorMessage = null
        try {
            Log.d("RecipesScreen", "Loading random recipes from TheMealDB...")
            // Загружаем несколько случайных рецептов
            val allMeals = mutableListOf<Meal>()
            for (i in 1..5) {
                try {
                    val response = ApiClient.theMealDbApi.getRandomMeals()
                    if (response.isSuccessful) {
                        response.body()?.meals?.let { meals ->
                            allMeals.addAll(meals)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("RecipesScreen", "Failed to load random meal $i: ${e.message}")
                }
            }
            
            if (allMeals.isEmpty()) {
                errorMessage = "Не удалось загрузить рецепты. Проверьте интернет."
                apiRecipes = emptyList()
            } else {
                apiRecipes = allMeals.map { it.toRecipe() }
                russianRecipes = emptyList()
                Log.d("RecipesScreen", "Loaded ${apiRecipes.size} recipes from TheMealDB")
            }
        } catch (e: Exception) {
            Log.e("RecipesScreen", "Error loading random recipes", e)
            errorMessage = "Ошибка: ${e.message}"
            apiRecipes = emptyList()
        } finally {
            isLoading = false
        }
    }
    
    // Функция поиска рецептов в TheMealDB
    fun searchRecipesOnline(query: String) {
        if (query.isBlank()) {
            apiRecipes = emptyList()
            return
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                Log.d("RecipesScreen", "Searching online for: $query")
                val response = ApiClient.theMealDbApi.searchRecipes(query)
                Log.d("RecipesScreen", "Response meals: ${response.meals?.size ?: 0}")
                val meals = response.meals ?: emptyList()
                if (meals.isEmpty()) {
                    errorMessage = "Рецепты не найдены в TheMealDB"
                    apiRecipes = emptyList()
                } else {
                    apiRecipes = meals.map { meal -> 
                        Log.d("RecipesScreen", "Converting meal: ${meal.strMeal}")
                        meal.toRecipe() 
                    }
                }
            } catch (e: UnknownHostException) {
                Log.e("RecipesScreen", "No internet connection", e)
                errorMessage = "Нет подключения к интернету. Проверьте соединение."
                apiRecipes = emptyList()
            } catch (e: ConnectException) {
                Log.e("RecipesScreen", "Connection failed", e)
                errorMessage = "Не удалось подключиться к серверу. Попробуйте позже."
                apiRecipes = emptyList()
            } catch (e: Exception) {
                Log.e("RecipesScreen", "Error searching recipes", e)
                errorMessage = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                apiRecipes = emptyList()
            } finally {
                isLoading = false
            }
        }
    }
    
    // Функция поиска в русской базе
    fun searchRussianRecipes(query: String) {
        if (query.isBlank()) {
            russianRecipes = emptyList()
            return
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                Log.d("RecipesScreen", "Searching Russian DB for: $query")
                val recipeRepo = Repositories.getRecipeRepository()
                val links = recipeRepo.searchRussianRecipes(query)
                Log.d("RecipesScreen", "Found ${links.size} russian recipes")
                if (links.isEmpty()) {
                    errorMessage = "Рецепты не найдены в русской базе"
                    russianRecipes = emptyList()
                } else {
                    russianRecipes = links.map { it.toRecipeStub() }
                    apiRecipes = emptyList() // Очищаем онлайн результаты
                }
            } catch (e: Exception) {
                Log.e("RecipesScreen", "Error searching russian recipes", e)
                errorMessage = "Ошибка поиска в русской базе: ${e.message}"
                russianRecipes = emptyList()
            } finally {
                isLoading = false
            }
        }
    }
    
    // Поиск только в TheMealDB (онлайн)
    fun searchAllRecipes(query: String) {
        if (query.isBlank()) {
            apiRecipes = emptyList()
            russianRecipes = emptyList()
            return
        }
        
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                // Ищем только онлайн в TheMealDB
                Log.d("RecipesScreen", "Searching online for: $query")
                val response = ApiClient.theMealDbApi.searchRecipes(query)
                val meals = response.meals ?: emptyList()
                
                if (meals.isNotEmpty()) {
                    apiRecipes = meals.map { it.toRecipe() }
                    russianRecipes = emptyList()
                    Log.d("RecipesScreen", "Found ${apiRecipes.size} recipes online")
                } else {
                    errorMessage = "Рецепты не найдены в TheMealDB"
                    apiRecipes = emptyList()
                    russianRecipes = emptyList()
                }
            } catch (e: UnknownHostException) {
                Log.e("RecipesScreen", "No internet connection", e)
                errorMessage = "Нет подключения к интернету. Проверьте соединение."
                apiRecipes = emptyList()
                russianRecipes = emptyList()
            } catch (e: Exception) {
                Log.e("RecipesScreen", "Error searching recipes", e)
                errorMessage = "Ошибка: ${e.message ?: "Неизвестная ошибка"}"
                apiRecipes = emptyList()
                russianRecipes = emptyList()
            } finally {
                isLoading = false
            }
        }
    }
    
    // Debounce для поиска при вводе (отключен, поиск по кнопке)
    // LaunchedEffect(searchQuery) {
    //     delay(500)
    //     searchDebounce = searchQuery
    // }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Рецепты") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            // Поле поиска с кнопками
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Например: Курица или Chicken") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true
                )

                Button(
                    onClick = { searchAllRecipes(searchQuery) },
                    enabled = searchQuery.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Найти")
                }
                
                Button(
                    onClick = { searchRecipesOnline(searchQuery) },
                    enabled = searchQuery.isNotBlank() && !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text("Онлайн")
                }
            }
            
            // Индикатор загрузки
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            
            // Сообщение об ошибке
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = errorMessage!!,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            
            // Отображение источника рецептов
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (apiRecipes.isNotEmpty()) {
                    Text(
                        text = "🌐 TheMealDB: ${apiRecipes.size} рецептов",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            // Список рецептов
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val displayRecipes = apiRecipes
                
                if (displayRecipes.isEmpty() && !isLoading) {
                    item {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (searchQuery.isNotBlank()) {
                                        "Рецепты не найдены. Попробуйте другой запрос!"
                                    } else {
                                        "Введите название рецепта для поиска\nили посмотрите случайные рецепты выше"
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (searchQuery.isBlank() && errorMessage != null) {
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(displayRecipes) { recipe ->
                        RecipeCard(recipe = recipe, onClick = { selectedRecipe = recipe })
                    }
                }
            }
        }
    }
    
    // Диалог с деталями рецепта
    selectedRecipe?.let { recipe ->
        RecipeDetailDialog(recipe = recipe, onDismiss = { selectedRecipe = null })
    }
}

@Composable
fun RecipeCard(recipe: Recipe, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Изображение рецепта или заглушка
            if (recipe.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = recipe.imageUrl,
                    contentDescription = recipe.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    error = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery),
                    placeholder = androidx.compose.ui.res.painterResource(android.R.drawable.ic_menu_gallery)
                )
            } else {
                // Заглушка для рецептов без картинки
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Нет изображения",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = recipe.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${recipe.prepTimeMinutes + recipe.cookTimeMinutes} мин",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    
                    if (recipe.calories > 0) {
                        Text(
                            "${recipe.calories} ккал",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                if (recipe.protein > 0 || recipe.fat > 0 || recipe.carbohydrates > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MacroChip(label = "Б", value = "${recipe.protein}г")
                        MacroChip(label = "Ж", value = "${recipe.fat}г")
                        MacroChip(label = "У", value = "${recipe.carbohydrates}г")
                    }
                }
                
                if (recipe.tags.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        recipe.tags.take(3).forEach { tag ->
                            AssistChip(
                                onClick = { },
                                label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecipeDetailDialog(recipe: Recipe, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Column {
                Text(recipe.name, style = MaterialTheme.typography.titleLarge)
                // Изображение в диалоге
                if (recipe.imageUrl.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    AsyncImage(
                        model = recipe.imageUrl,
                        contentDescription = recipe.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                }
            }
        },
        text = {
            LazyColumn {
                // Ингредиенты
                if (recipe.ingredients.isNotEmpty()) {
                    item {
                        Text("Ингредиенты:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    items(recipe.ingredients) { ingredient ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(ingredient.foodId)
                            if (ingredient.amount > 0 && ingredient.unit.isNotEmpty()) {
                                Text("${ingredient.amount} ${ingredient.unit}")
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
                
                // Инструкция
                if (recipe.instructions.isNotEmpty()) {
                    item {
                        Text("Приготовление:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(recipe.instructions) { index: Int, step: String ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text("${index + 1}. ")
                            Text(text = step)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
                
                // КБЖУ (только если есть данные)
                if (recipe.calories > 0 || recipe.protein > 0 || recipe.fat > 0 || recipe.carbohydrates > 0) {
                    item {
                        Text("Пищевая ценность:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (recipe.calories > 0) {
                                MacroChip(label = "Ккал", value = "${recipe.calories}")
                            }
                            if (recipe.protein > 0) {
                                MacroChip(label = "Б", value = "${recipe.protein}г")
                            }
                            if (recipe.fat > 0) {
                                MacroChip(label = "Ж", value = "${recipe.fat}г")
                            }
                            if (recipe.carbohydrates > 0) {
                                MacroChip(label = "У", value = "${recipe.carbohydrates}г")
                            }
                        }
                    }
                }
                
                // Теги
                if (recipe.tags.isNotEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Теги:", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            recipe.tags.forEach { tag ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Закрыть")
            }
        },
        dismissButton = {
            if (recipe.imageUrl.isNotEmpty()) {
                OutlinedButton(
                    onClick = { 
                        // Можно добавить открытие изображения в полном размере
                    }
                ) {
                    Text("Фото")
                }
            }
        }
    )
}

@Composable
fun MacroChip(label: String, value: String) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelSmall)
            Text(value, style = MaterialTheme.typography.labelSmall)
        }
    }
}
