package com.questlife.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen() {
    var selectedFilter by remember { mutableStateOf<ItemType?>(null) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }

    val items = remember { getSampleItems() }

    val filteredItems = if (selectedFilter == null) items else items.filter { it.type == selectedFilter }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Инвентарь") })
        }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            ItemTypeFilter(
                selectedType = selectedFilter,
                onTypeSelected = { selectedFilter = if (selectedFilter == it) null else it }
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredItems) { item ->
                    ItemCard(item = item, onClick = { selectedItem = item })
                }
            }
        }
    }

    selectedItem?.let { item ->
        ItemDetailsDialog(
            item = item,
            onDismiss = { selectedItem = null },
            onEquip = { selectedItem = null },
            onSell = { selectedItem = null }
        )
    }
}

// ==================== ФИЛЬТР ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemTypeFilter(
    selectedType: ItemType?,
    onTypeSelected: (ItemType) -> Unit
) {
    val types = listOf(
        Triple(ItemType.WEAPON, "⚔️", "Оружие"),
        Triple(ItemType.ARMOR, "🛡️", "Броня"),
        Triple(ItemType.ACCESSORY, "💍", "Аксессуары"),
        Triple(ItemType.CONSUMABLE, "🧪", "Зелья")
    )

    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        types.forEach { (type, emoji, label) ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelected(type) },
                label = { Text("$emoji $label") },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ==================== КАРТОЧКА ПРЕДМЕТА ====================
@Composable
fun ItemCard(item: Item, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(getRarityColor(item.rarity).copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.iconEmoji, style = MaterialTheme.typography.displaySmall)
            }

            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Surface(
                shape = RoundedCornerShape(4.dp),
                color = getRarityColor(item.rarity).copy(alpha = 0.2f)
            ) {
                Text(
                    text = getRarityText(item.rarity),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = getRarityColor(item.rarity),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// ==================== ДИАЛОГ ДЕТАЛЕЙ ====================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDetailsDialog(
    item: Item,
    onDismiss: () -> Unit,
    onEquip: () -> Unit,
    onSell: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = item.iconEmoji, style = MaterialTheme.typography.displayMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Text(item.name)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = getRarityColor(item.rarity).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = getRarityText(item.rarity),
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = getRarityColor(item.rarity),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Text(text = item.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

                if (hasStatBonus(item.statsBonus)) {
                    Divider()
                    Text("Бонусы:", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    if (item.statsBonus.strength > 0) StatBonus("💪", "Сила", "+${item.statsBonus.strength}")
                    if (item.statsBonus.agility > 0) StatBonus("⚡", "Ловкость", "+${item.statsBonus.agility}")
                    if (item.statsBonus.intelligence > 0) StatBonus("🧠", "Интеллект", "+${item.statsBonus.intelligence}")
                    if (item.statsBonus.vitality > 0) StatBonus("❤️", "Живучесть", "+${item.statsBonus.vitality}")
                }

                Divider()
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Цена продажи:", style = MaterialTheme.typography.bodyMedium)
                    Text("💰 ${item.price / 2}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        },
        confirmButton = {
            if (item.type != ItemType.CONSUMABLE) Button(onClick = onEquip) { Text("Экипировать") }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onSell) { Text("Продать") }
                TextButton(onClick = onDismiss) { Text("Закрыть") }
            }
        }
    )
}

@Composable
fun StatBonus(emoji: String, name: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji)
            Spacer(modifier = Modifier.width(8.dp))
            Text(name, style = MaterialTheme.typography.bodyMedium)
        }
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
    }
}

fun hasStatBonus(stats: CharacterStats): Boolean =
    stats.strength > 0 || stats.agility > 0 || stats.intelligence > 0 || stats.vitality > 0

// ==================== ЦВЕТ И ТЕКСТ РЕДКОСТИ ====================
fun getRarityColor(rarity: ItemRarity): Color = when (rarity) {
    ItemRarity.COMMON -> Color.Gray
    ItemRarity.UNCOMMON -> Color(0xFF4CAF50)
    ItemRarity.RARE -> Color(0xFF2196F3)
    ItemRarity.EPIC -> Color(0xFF9C27B0)
    ItemRarity.LEGENDARY -> Color(0xFFFFC107)
}

fun getRarityText(rarity: ItemRarity): String = when (rarity) {
    ItemRarity.COMMON -> "Обычный"
    ItemRarity.UNCOMMON -> "Необычный"
    ItemRarity.RARE -> "Редкий"
    ItemRarity.EPIC -> "Эпический"
    ItemRarity.LEGENDARY -> "Легендарный"
}

// ==================== ПРИМЕР ПРЕДМЕТОВ ====================
fun getSampleItems(): List<Item> = listOf(
    Item(name = "Деревянный меч", description = "Простой тренировочный меч", type = ItemType.WEAPON, rarity = ItemRarity.COMMON, price = 50, statsBonus = CharacterStats(strength = 2), iconEmoji = "🗡️"),
    Item(name = "Кожаная броня", description = "Легкая броня", type = ItemType.ARMOR, rarity = ItemRarity.COMMON, price = 100, statsBonus = CharacterStats(vitality = 3), iconEmoji = "🦺"),
    Item(name = "Стальной меч", description = "Качественный меч", type = ItemType.WEAPON, rarity = ItemRarity.UNCOMMON, price = 200, statsBonus = CharacterStats(strength = 5), iconEmoji = "⚔️"),
    Item(name = "Амулет мудрости", description = "Древний амулет", type = ItemType.ACCESSORY, rarity = ItemRarity.RARE, price = 300, statsBonus = CharacterStats(intelligence = 8), iconEmoji = "📿"),
    Item(name = "Зелье здоровья", description = "Восстанавливает 50 HP", type = ItemType.CONSUMABLE, rarity = ItemRarity.COMMON, price = 50, iconEmoji = "🧪"),
    Item(name = "Сапоги проворства", description = "Увеличивают скорость", type = ItemType.ACCESSORY, rarity = ItemRarity.UNCOMMON, price = 150, statsBonus = CharacterStats(agility = 4), iconEmoji = "👟"),
    Item(name = "Рыцарские доспехи", description = "Тяжелые доспехи", type = ItemType.ARMOR, rarity = ItemRarity.EPIC, price = 500, statsBonus = CharacterStats(vitality = 10, strength = 5), iconEmoji = "🛡️"),
    Item(name = "Легендарный клинок", description = "Легендарное оружие", type = ItemType.WEAPON, rarity = ItemRarity.LEGENDARY, price = 1000, statsBonus = CharacterStats(strength = 15, agility = 10), iconEmoji = "🗡️")
)