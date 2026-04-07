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
import com.questlife.app.ui.utils.getRarityColor
import com.questlife.app.ui.utils.getRarityText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    character: Character?,
    onUpdateCharacter: (Character) -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf<ItemType?>(null) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showEquipConfirm by remember { mutableStateOf<Item?>(null) }

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
            onEquip = { 
                if (character != null && item.type != ItemType.CONSUMABLE) {
                    showEquipConfirm = item
                }
                selectedItem = null
            },
            onSell = { selectedItem = null }
        )
    }
    
    // Диалог подтверждения экипировки
    showEquipConfirm?.let { item ->
        if (character != null) {
            EquipConfirmationDialog(
                item = item,
                character = character,
                onDismiss = { showEquipConfirm = null },
                onConfirm = {
                    val updatedEquipped = when (item.type) {
                        ItemType.WEAPON -> character.equippedItems.copy(weapon = item)
                        ItemType.ARMOR -> character.equippedItems.copy(armor = item)
                        ItemType.ACCESSORY -> character.equippedItems.copy(accessory = item)
                        ItemType.CONSUMABLE -> character.equippedItems
                    }
                    val updatedCharacter = character.copy(equippedItems = updatedEquipped)
                    onUpdateCharacter(updatedCharacter)
                    showEquipConfirm = null
                }
            )
        }
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
// ==================== ДИАЛОГ ПОДТВЕРЖДЕНИЯ ЭКИПИРОВКИ ====================
@Composable
fun EquipConfirmationDialog(
    item: Item,
    character: Character,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val slotName = when (item.type) {
        ItemType.WEAPON -> "Оружие"
        ItemType.ARMOR -> "Броня"
        ItemType.ACCESSORY -> "Аксессуар"
        ItemType.CONSUMABLE -> return // Не должно произойти
    }
    
    val currentEquipped = when (item.type) {
        ItemType.WEAPON -> character.equippedItems.weapon
        ItemType.ARMOR -> character.equippedItems.armor
        ItemType.ACCESSORY -> character.equippedItems.accessory
        ItemType.CONSUMABLE -> null
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Экипировать предмет") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Вы хотите экипировать:")
                Text("⚔️ ${item.name}", fontWeight = FontWeight.Bold)
                
                if (currentEquipped != null) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Текущий предмет в слоте \"$slotName\":")
                    Text("📦 ${currentEquipped.name}", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Новый предмет заменит текущий.", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Слот \"$slotName\" сейчас пуст.", style = MaterialTheme.typography.bodySmall)
                }
                
                if (hasStatBonus(item.statsBonus)) {
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Бонусы предмета:", fontWeight = FontWeight.Bold)
                    if (item.statsBonus.strength > 0) Text("💪 Сила: +${item.statsBonus.strength}")
                    if (item.statsBonus.agility > 0) Text("⚡ Ловкость: +${item.statsBonus.agility}")
                    if (item.statsBonus.intelligence > 0) Text("🧠 Интеллект: +${item.statsBonus.intelligence}")
                    if (item.statsBonus.vitality > 0) Text("❤️ Живучесть: +${item.statsBonus.vitality}")
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Экипировать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
