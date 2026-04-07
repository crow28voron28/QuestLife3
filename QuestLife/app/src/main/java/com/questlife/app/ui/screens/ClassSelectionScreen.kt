package com.questlife.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questlife.app.models.CharacterClass
import com.questlife.app.models.ClassCategory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassSelectionScreen(
    onClassSelected: (CharacterClass) -> Unit,
    modifier: Modifier = Modifier
) {
    val characterClasses = CharacterClass.getAll()
    var selectedCategory by remember { mutableStateOf<ClassCategory?>(null) }
    
    val filteredClasses = if (selectedCategory == null) {
        characterClasses
    } else {
        characterClasses.filter { it.category == selectedCategory }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Выберите класс персонажа") },
                navigationIcon = {}
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Ваш игровой класс",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Text(
                text = "Класс влияет на специальные квесты и способности",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Фильтры по категориям
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("Все") }
                )
                FilterChip(
                    selected = selectedCategory == ClassCategory.RPG_CLASSIC,
                    onClick = { selectedCategory = ClassCategory.RPG_CLASSIC },
                    label = { Text("RPG") }
                )
                FilterChip(
                    selected = selectedCategory == ClassCategory.FANTASY,
                    onClick = { selectedCategory = ClassCategory.FANTASY },
                    label = { Text("Фэнтези") }
                )
                FilterChip(
                    selected = selectedCategory == ClassCategory.SCI_FI,
                    onClick = { selectedCategory = ClassCategory.SCI_FI },
                    label = { Text("Sci-Fi") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredClasses) { charClass ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onClassSelected(charClass) },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = charClass.icon,
                                fontSize = 24.sp,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Column {
                                Text(
                                    text = charClass.displayName,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = charClass.category.displayName,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
