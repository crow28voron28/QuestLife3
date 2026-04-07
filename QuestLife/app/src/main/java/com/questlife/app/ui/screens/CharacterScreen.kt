package com.questlife.app.ui.screens

import androidx.compose.foundation.layout.*
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
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterScreen(character: Character?) {
    // Если персонаж не создан, используем заглушку
    val displayCharacter = character ?: Character(
        name = "Новый Герой",
        level = 1,
        experience = 0,
        experienceToNextLevel = 100,
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
            // Аватар - иконка класса
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(displayCharacter.characterClass.icon, style = MaterialTheme.typography.displayLarge)
            }

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
                        progress = { displayCharacter.experience.toFloat() / displayCharacter.experienceToNextLevel },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${displayCharacter.experience}/${displayCharacter.experienceToNextLevel} XP", modifier = Modifier.align(Alignment.End))
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

            Spacer(modifier = Modifier.height(32.dp))

            // Экипировка (заглушка)
            Text("Экипировка", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                EquipmentSlot("Оружие", displayCharacter.equippedItems.weapon)
                EquipmentSlot("Броня", displayCharacter.equippedItems.armor)
                EquipmentSlot("Аксессуар", displayCharacter.equippedItems.accessory)
            }
            
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

@Composable
private fun EquipmentSlot(slotName: String, item: Item?) {
    Card(
        modifier = Modifier.size(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(item?.iconEmoji ?: "❓", style = MaterialTheme.typography.headlineMedium)
            Text(slotName, style = MaterialTheme.typography.labelSmall)
        }
    }
}