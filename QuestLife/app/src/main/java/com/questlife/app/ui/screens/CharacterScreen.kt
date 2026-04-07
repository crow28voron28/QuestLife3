package com.questlife.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questlife.app.models.*
import com.questlife.app.ui.theme.PixelFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterScreen(character: Character?) {
    // Если персонаж не создан, используем заглушку
    val displayCharacter = character ?: Character(
        name = "Новый Герой",
        level = 1,
        experience = 0,
        experienceToNextLevel = 200,
        health = 100,
        maxHealth = 100,
        gold = 0,
        stats = CharacterStats(strength = 5, agility = 5, intelligence = 5, vitality = 5),
        equippedItems = EquippedItems()
    )

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Персонаж") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Пиксельный человек с экипировкой
            PixelCharacterView(
                character = displayCharacter,
                modifier = Modifier.size(200.dp)
            )

            Text(
                displayCharacter.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AssistChip(
                    onClick = {},
                    label = { Text("${displayCharacter.profession.icon} ${displayCharacter.profession.displayName}") }
                )
                AssistChip(
                    onClick = {},
                    label = { Text("${displayCharacter.characterClass.icon} ${displayCharacter.characterClass.displayName}") }
                )
            }

            Text(
                "Уровень ${displayCharacter.level}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Здоровье
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Здоровье", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { displayCharacter.health.toFloat() / displayCharacter.maxHealth },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${displayCharacter.health}/${displayCharacter.maxHealth} HP", modifier = Modifier.align(Alignment.End))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Опыт
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Опыт", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { displayCharacter.experience.toFloat() / getXpRequiredForLevel(displayCharacter.level + 1) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${displayCharacter.experience}/${getXpRequiredForLevel(displayCharacter.level + 1)} XP", modifier = Modifier.align(Alignment.End))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Золото
            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰 ${displayCharacter.gold}", style = MaterialTheme.typography.headlineSmall)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Золото", style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Характеристики
            Text("Характеристики", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            StatsRow("Сила", displayCharacter.stats.strength)
            StatsRow("Ловкость", displayCharacter.stats.agility)
            StatsRow("Интеллект", displayCharacter.stats.intelligence)
            StatsRow("Живучесть", displayCharacter.stats.vitality)

            Spacer(modifier = Modifier.height(24.dp))

            // Ячейки экипировки
            Text("Экипировка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            EquipmentGrid(equippedItems = displayCharacter.equippedItems)
            
            if (character == null) {
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    "Выберите профессию и класс на экране квестов",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Пиксельный человек с визуализацией экипировки
@Composable
fun PixelCharacterView(character: Character, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xFF1a1a2e), RoundedCornerShape(16.dp))
            .border(2.dp, Color(0xFFe94560), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Голова (с шлемом если есть)
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                // Базовая голова
                Canvas(modifier = Modifier.size(50.dp)) {
                    // Лицо
                    drawRect(
                        color = Color(0xFFFFDBAC),
                        topLeft = Offset(10f, 10f),
                        size = androidx.compose.ui.geometry.Size(30f, 35f)
                    )
                    // Глаза
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(18f, 20f),
                        size = androidx.compose.ui.geometry.Size(4f, 4f)
                    )
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(28f, 20f),
                        size = androidx.compose.ui.geometry.Size(4f, 4f)
                    )
                    // Рот
                    drawRect(
                        color = Color(0xFFcc6666),
                        topLeft = Offset(20f, 32f),
                        size = androidx.compose.ui.geometry.Size(10f, 3f)
                    )
                }
                
                // Шлем если есть броня
                if (character.equippedItems.armor != null) {
                    Text(
                        text = "🪖",
                        fontSize = 40.sp,
                        modifier = Modifier.offset(y = (-10).dp)
                    )
                }
            }
            
            // Тело с оружием
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Оружие в руке
                if (character.equippedItems.weapon != null) {
                    Text(
                        text = character.equippedItems.weapon!!.iconEmoji,
                        fontSize = 36.sp
                    )
                }
                
                // Тело
                Box(
                    modifier = Modifier
                        .width(50.dp)
                        .height(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Туловище
                        drawRect(
                            color = if (character.equippedItems.armor != null) 
                                getRarityColor(character.equippedItems.armor!!.rarity) 
                            else Color(0xFF4a90d9),
                            topLeft = Offset(10f, 5f),
                            size = androidx.compose.ui.geometry.Size(30f, 50f)
                        )
                        // Руки
                        drawRect(
                            color = Color(0xFFFFDBAC),
                            topLeft = Offset(5f, 10f),
                            size = androidx.compose.ui.geometry.Size(5f, 35f)
                        )
                        drawRect(
                            color = Color(0xFFFFDBAC),
                            topLeft = Offset(40f, 10f),
                            size = androidx.compose.ui.geometry.Size(5f, 35f)
                        )
                    }
                }
                
                // Оружие в другой руке или щит
                if (character.equippedItems.weapon == null && character.equippedItems.armor != null) {
                    Text(
                        text = "🛡️",
                        fontSize = 32.sp
                    )
                }
            }
            
            // Ноги
            Canvas(modifier = Modifier.size(50.dp, 40.dp)) {
                // Ноги
                drawRect(
                    color = Color(0xFF3366cc),
                    topLeft = Offset(12f, 0f),
                    size = androidx.compose.ui.geometry.Size(10f, 35f)
                )
                drawRect(
                    color = Color(0xFF3366cc),
                    topLeft = Offset(28f, 0f),
                    size = androidx.compose.ui.geometry.Size(10f, 35f)
                )
            }
            
            // Аксессуар
            if (character.equippedItems.accessory != null) {
                Text(
                    text = character.equippedItems.accessory!!.iconEmoji,
                    fontSize = 20.sp,
                    modifier = Modifier.offset(x = 40.dp, y = (-80).dp)
                )
            }
        }
    }
}

// Сетка экипировки с ячейками
@Composable
fun EquipmentGrid(equippedItems: EquippedItems) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EquipmentSlotWithLabel("Оружие", equippedItems.weapon, "⚔️")
        EquipmentSlotWithLabel("Броня", equippedItems.armor, "🛡️")
        EquipmentSlotWithLabel("Аксессуар", equippedItems.accessory, "💍")
    }
}

@Composable
fun EquipmentSlotWithLabel(slotName: String, item: Item?, defaultIcon: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            modifier = Modifier.size(80.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (item != null) 
                    getRarityColor(item.rarity).copy(alpha = 0.3f) 
                else MaterialTheme.colorScheme.surfaceVariant
            ),
            border = if (item != null) 
                androidx.compose.foundation.BorderStroke(2.dp, getRarityColor(item.rarity)) 
            else null
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    item?.iconEmoji ?: defaultIcon, 
                    style = MaterialTheme.typography.headlineMedium
                )
                if (item != null) {
                    Text(
                        item.name,
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            slotName,
            style = MaterialTheme.typography.labelSmall,
            fontFamily = PixelFontFamily
        )
    }
}

@Composable
private fun StatsRow(label: String, value: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(value.toString(), fontWeight = FontWeight.Bold)
    }
}

// Функция цвета редкости (для использования в CharacterScreen)
fun getRarityColor(rarity: ItemRarity): Color = when (rarity) {
    ItemRarity.COMMON -> Color.Gray
    ItemRarity.UNCOMMON -> Color(0xFF4CAF50)
    ItemRarity.RARE -> Color(0xFF2196F3)
    ItemRarity.EPIC -> Color(0xFF9C27B0)
    ItemRarity.LEGENDARY -> Color(0xFFFFC107)
}