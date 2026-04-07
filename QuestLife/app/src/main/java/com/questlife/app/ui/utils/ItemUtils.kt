package com.questlife.app.ui.utils

import androidx.compose.ui.graphics.Color
import com.questlife.app.models.ItemRarity

// ==================== ЦВЕТ И ТЕКСТ РЕДКОСТИ ====================
fun getRarityText(rarity: ItemRarity): String = when (rarity) {
    ItemRarity.COMMON -> "Обычный"
    ItemRarity.UNCOMMON -> "Необычный"
    ItemRarity.RARE -> "Редкий"
    ItemRarity.EPIC -> "Эпический"
    ItemRarity.LEGENDARY -> "Легендарный"
}

fun getRarityColor(rarity: ItemRarity): Color = when (rarity) {
    ItemRarity.COMMON -> Color.Gray
    ItemRarity.UNCOMMON -> Color(0xFF00FF00)
    ItemRarity.RARE -> Color(0xFF0080FF)
    ItemRarity.EPIC -> Color(0xFFA020F0)
    ItemRarity.LEGENDARY -> Color(0xFFFFD700)
}
