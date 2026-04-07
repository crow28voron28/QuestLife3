package com.questlife.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
fun ShopScreen() {
    var playerGold by remember { mutableStateOf(250) }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var showPurchaseDialog by remember { mutableStateOf(false) }
    
    val shopItems = remember { getShopItems() }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Магазин") },
                actions = {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "💰",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = playerGold.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Приветственная карточка
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🏪",
                        style = MaterialTheme.typography.displaySmall
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            "Добро пожаловать в магазин!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Покупайте снаряжение за заработанное золото",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
            
            // Список предметов в магазине
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shopItems) { item ->
                    ShopItemCard(
                        item = item,
                        playerGold = playerGold,
                        onBuyClick = {
                            selectedItem = item
                            showPurchaseDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Диалог покупки
    if (showPurchaseDialog && selectedItem != null) {
        PurchaseDialog(
            item = selectedItem!!,
            playerGold = playerGold,
            onConfirm = {
                playerGold -= selectedItem!!.price
                showPurchaseDialog = false
                selectedItem = null
            },
            onDismiss = {
                showPurchaseDialog = false
                selectedItem = null
            }
        )
    }
}

@Composable
fun ShopItemCard(
    item: Item,
    playerGold: Int,
    onBuyClick: () -> Unit
) {
    val canAfford = playerGold >= item.price
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Иконка и информация
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            getRarityColor(item.rarity).copy(alpha = 0.1f),
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.iconEmoji,
                        style = MaterialTheme.typography.displayMedium
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
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
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = item.description.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                    
                    // Бонусы
                    if (hasStatBonus(item.statsBonus)) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (item.statsBonus.strength > 0) {
                                StatBadge("💪", "+${item.statsBonus.strength}")
                            }
                            if (item.statsBonus.agility > 0) {
                                StatBadge("⚡", "+${item.statsBonus.agility}")
                            }
                            if (item.statsBonus.intelligence > 0) {
                                StatBadge("🧠", "+${item.statsBonus.intelligence}")
                            }
                            if (item.statsBonus.vitality > 0) {
                                StatBadge("❤️", "+${item.statsBonus.vitality}")
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Цена и кнопка покупки
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "💰",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = item.price.toString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Button(
                    onClick = onBuyClick,
                    enabled = canAfford,
                    modifier = Modifier.width(100.dp)
                ) {
                    Text(if (canAfford) "Купить" else "Дорого")
                }
            }
        }
    }
}

@Composable
fun StatBadge(emoji: String, value: String) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.labelSmall
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
fun PurchaseDialog(
    item: Item,
    playerGold: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Text(
                text = item.iconEmoji,
                style = MaterialTheme.typography.displayMedium
            )
        },
        title = {
            Text("Купить ${item.name}?")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = item.description.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Divider()
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Цена:")
                    Text(
                        "💰 ${item.price}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("У вас:")
                    Text(
                        "💰 $playerGold",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Останется:")
                    Text(
                        "💰 ${playerGold - item.price}",
                        fontWeight = FontWeight.Bold,
                        color = if (playerGold - item.price >= 0) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Купить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}

fun getShopItems(): List<Item> {
    return listOf(
        Item(
            name = "Деревянный меч",
            description = "Простой тренировочный меч",
            type = ItemType.WEAPON,
            rarity = ItemRarity.COMMON,
            price = 50,
            statsBonus = CharacterStats(strength = 2),
            iconEmoji = "🗡️"
        ),
        Item(
            name = "Кожаная броня",
            description = "Легкая защита из прочной кожи",
            type = ItemType.ARMOR,
            rarity = ItemRarity.COMMON,
            price = 100,
            statsBonus = CharacterStats(vitality = 3),
            iconEmoji = "🦺"
        ),
        Item(
            name = "Зелье здоровья",
            description = "Восстанавливает 50 HP",
            type = ItemType.CONSUMABLE,
            rarity = ItemRarity.COMMON,
            price = 50,
            iconEmoji = "🧪"
        ),
        Item(
            name = "Стальной меч",
            description = "Качественно выкованный меч",
            type = ItemType.WEAPON,
            rarity = ItemRarity.UNCOMMON,
            price = 200,
            statsBonus = CharacterStats(strength = 5),
            iconEmoji = "⚔️"
        ),
        Item(
            name = "Кольчуга",
            description = "Прочная кольчужная броня",
            type = ItemType.ARMOR,
            rarity = ItemRarity.UNCOMMON,
            price = 250,
            statsBonus = CharacterStats(vitality = 6),
            iconEmoji = "🛡️"
        ),
        Item(
            name = "Сапоги проворства",
            description = "Увеличивают скорость передвижения",
            type = ItemType.ACCESSORY,
            rarity = ItemRarity.UNCOMMON,
            price = 150,
            statsBonus = CharacterStats(agility = 4),
            iconEmoji = "👟"
        ),
        Item(
            name = "Амулет мудрости",
            description = "Древний амулет, повышающий ясность ума",
            type = ItemType.ACCESSORY,
            rarity = ItemRarity.RARE,
            price = 300,
            statsBonus = CharacterStats(intelligence = 8),
            iconEmoji = "📿"
        ),
        Item(
            name = "Рыцарские доспехи",
            description = "Тяжелые доспехи настоящего рыцаря",
            type = ItemType.ARMOR,
            rarity = ItemRarity.EPIC,
            price = 500,
            statsBonus = CharacterStats(vitality = 10, strength = 5),
            iconEmoji = "🛡️"
        ),
        Item(
            name = "Легендарный клинок",
            description = "Легендарное оружие героев",
            type = ItemType.WEAPON,
            rarity = ItemRarity.LEGENDARY,
            price = 1000,
            statsBonus = CharacterStats(strength = 15, agility = 10),
            iconEmoji = "⚔️"
        )
    )
}
