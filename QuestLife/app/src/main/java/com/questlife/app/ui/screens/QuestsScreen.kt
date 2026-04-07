package com.questlife.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questlife.app.data.MegaQuestDatabase
import com.questlife.app.data.getXpRequiredForLevel
import com.questlife.app.data.CompletedQuestEntry
import com.questlife.app.models.Character
import com.questlife.app.models.Quest
import com.questlife.app.models.QuestDifficulty
import com.questlife.app.models.QuestType
import com.questlife.app.ui.theme.PixelFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.BorderStroke
import kotlin.math.PI
import java.time.LocalDate
import java.time.LocalDateTime

// Extension для поворота в градусах
fun Float.degrees(): Float = this * PI.toFloat() / 180f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestsScreen(
    character: Character?,
    onSetupCharacter: () -> Unit,
    onCharacterUpdate: (Character) -> Unit,
    onAddToCalendar: (Quest) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var visibleDailyQuests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var visibleWeeklyQuests by remember { mutableStateOf<List<Quest>>(emptyList()) }
    var showLevelUpDialog by remember { mutableStateOf(false) }
    var levelUpData by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var floatingRewards by remember { mutableStateOf<List<FloatingReward>>(emptyList()) }
    
    // История выполненных квестов за день
    var completedQuestsHistory by remember { mutableStateOf<List<CompletedQuestEntry>>(emptyList()) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var showAddQuestDialog by remember { mutableStateOf(false) }
    
    // Загрузка истории из MegaQuestDatabase при старте
    LaunchedEffect(Unit) {
        // Проверяем и сбрасываем историю если новый день
        MegaQuestDatabase.checkAndResetDailyHistory()
        // Загружаем историю из базы
        completedQuestsHistory = MegaQuestDatabase.getCompletedQuestHistory()
    }

    LaunchedEffect(character) {
        if (character != null) {
            // Квесты теперь создаются пользователем самостоятельно
            visibleDailyQuests = emptyList()
            visibleWeeklyQuests = emptyList()
        } else {
            visibleDailyQuests = emptyList()
            visibleWeeklyQuests = emptyList()
        }
    }

    fun completeQuest(quest: Quest) {
        if (character == null) return
        
        scope.launch {
            val questIdHash = quest.id.hashCode()
            val newRewards = floatingRewards + listOf(
                FloatingReward("+${quest.xpReward} XP", Color(0xFFFFD700), questIdHash),
                FloatingReward("+${quest.goldReward} G", Color(0xFFC0C0C0), questIdHash + 10000)
            )
            floatingRewards = newRewards

            val newXp = character.experience + quest.xpReward
            val newGold = character.gold + quest.goldReward
            var newLevel = character.level
            var leveledUp = false

            while (newXp >= getXpRequiredForLevel(newLevel + 1)) {
                newLevel++
                leveledUp = true
            }

            val updatedCharacter = character.copy(
                experience = newXp,
                gold = newGold,
                level = newLevel
            )
            onCharacterUpdate(updatedCharacter)
            
            // Добавляем в историю выполненных квестов
            val completedEntry = CompletedQuestEntry(
                questTitle = quest.title,
                xpReward = quest.xpReward,
                goldReward = quest.goldReward,
                completedAt = LocalDateTime.now(),
                questType = quest.type
            )
            completedQuestsHistory = completedQuestsHistory + completedEntry
            // Сохраняем в базу данных
            MegaQuestDatabase.saveCompletedQuestHistory(completedEntry)
            
            // Если это недельный квест - помечаем его как выполненный
            if (quest.type == QuestType.WEEKLY) {
                MegaQuestDatabase.markWeeklyQuestCompleted(quest.title)
            }

            visibleDailyQuests = visibleDailyQuests.filter { it.id != quest.id }
            // Недельные квесты тоже убираем после выполнения
            visibleWeeklyQuests = visibleWeeklyQuests.filter { it.id != quest.id }
            
            delay(1500)
            floatingRewards = floatingRewards.filter { it.id != questIdHash && it.id != questIdHash + 10000 }

            if (leveledUp) {
                levelUpData = Pair(character.level, newLevel)
                showLevelUpDialog = true
            }
        }
    }
    
    fun addCustomQuest(quest: Quest) {
        if (character == null) return
        
        // Проверяем лимиты награды (максимум 100 XP и 50 золота)
        val cappedXp = minOf(quest.xpReward, 100)
        val cappedGold = minOf(quest.goldReward, 50)
        
        val cappedQuest = quest.copy(
            xpReward = cappedXp,
            goldReward = cappedGold,
            type = QuestType.CUSTOM
        )
        
        // Добавляем к ежедневным квестам
        visibleDailyQuests = visibleDailyQuests + cappedQuest
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1a1a2e))) {
        if (character == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    "⚠️ ПЕРСОНАЖ НЕ СОЗДАН",
                    color = Color.Red,
                    fontFamily = PixelFontFamily,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSetupCharacter,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe94560)),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.size(220.dp, 50.dp)
                ) {
                    Text("СОЗДАТЬ ГЕРОЯ", fontFamily = PixelFontFamily, color = Color.White, fontSize = 16.sp)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "📜 ДНЕВНИК ГЕРОЯ",
                    color = Color(0xFFe94560),
                    fontSize = 28.sp,
                    fontFamily = PixelFontFamily,
                    modifier = Modifier.padding(vertical = 16.dp),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineSmall.copy(
                        shadow = Shadow(color = Color.Black, blurRadius = 4f, offset = Offset(2f, 2f))
                    )
                )

                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF16213e)),
                    border = BorderStroke(2.dp, Color(0xFFe94560)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Уровень: ${character.level}", color = Color(0xFFffd700), fontFamily = PixelFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text("Золото: ${character.gold}", color = Color(0xFFc0c0c0), fontFamily = PixelFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = character.experience.toFloat() / getXpRequiredForLevel(character.level + 1),
                            modifier = Modifier.fillMaxWidth().height(12.dp),
                            color = Color(0xFFe94560),
                            trackColor = Color(0xFF0f3460)
                        )
                        Text(
                            "XP: ${character.experience} / ${getXpRequiredForLevel(character.level + 1)}",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontFamily = PixelFontFamily,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Пол: ${character.gender}", fontSize = 11.sp, fontFamily = PixelFontFamily) },
                                colors = AssistChipDefaults.assistChipColors(containerColor = Color(0xFF0f3460))
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Кнопка истории квестов
                    Button(
                        onClick = { showHistoryDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0f3460)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(45.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ИСТОРИЯ", fontFamily = PixelFontFamily, fontSize = 10.sp, color = Color.White)
                    }
                    
                    // Кнопка добавления квеста
                    Button(
                        onClick = { showAddQuestDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4caf50)),
                        shape = RoundedCornerShape(4.dp),
                        modifier = Modifier.weight(1f).height(45.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("КВЕСТ", fontFamily = PixelFontFamily, fontSize = 10.sp, color = Color.White)
                    }
                }

                if (visibleDailyQuests.isEmpty() && visibleWeeklyQuests.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "✅ Все квесты выполнены!\nЗаходи завтра за новыми.",
                            color = Color.Gray,
                            fontFamily = PixelFontFamily,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                        if (visibleDailyQuests.isNotEmpty()) {
                            item {
                                Text("⚔️ ЕЖЕДНЕВНЫЕ КВЕСТЫ", color = Color(0xFFe94560), fontFamily = PixelFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 16.sp)
                            }
                            items(visibleDailyQuests, key = { it.id }) { quest ->
                                QuestCard(
                                    quest = quest, 
                                    onClick = { completeQuest(quest) },
                                    onAddToCalendar = { onAddToCalendar(it) }
                                )
                            }
                        }
                        
                        // Недельные боссы - отдельной секцией с ограничением
                        if (visibleWeeklyQuests.isNotEmpty()) {
                            item {
                                Spacer(modifier = Modifier.height(20.dp))
                                Text("👑 НЕДЕЛЬНЫЕ БОССЫ", color = Color(0xFF9c27b0), fontFamily = PixelFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, fontSize = 18.sp)
                                Text("(доступно до конца недели)", color = Color(0xFF9c27b0).copy(alpha = 0.7f), fontFamily = PixelFontFamily, fontSize = 10.sp)
                            }
                            items(visibleWeeklyQuests, key = { it.id }) { quest ->
                                WeeklyBossCard(quest = quest, onClick = { completeQuest(quest) })
                            }
                        }
                        
                        items(floatingRewards) { reward ->
                            Box(modifier = Modifier.fillMaxWidth().height(40.dp), contentAlignment = Alignment.Center) {
                                Text(text = reward.text, color = reward.color, fontSize = 20.sp, fontFamily = PixelFontFamily, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showLevelUpDialog && levelUpData != null) {
            LevelUpDialog(oldLevel = levelUpData!!.first, newLevel = levelUpData!!.second, onDismiss = { showLevelUpDialog = false })
        }
        
        // Диалог истории квестов
        if (showHistoryDialog) {
            QuestHistoryDialog(
                history = completedQuestsHistory,
                onDismiss = { showHistoryDialog = false }
            )
        }
        
        // Диалог добавления квеста
        if (showAddQuestDialog) {
            AddCustomQuestDialog(
                onDismiss = { showAddQuestDialog = false },
                onQuestAdded = { quest ->
                    addCustomQuest(quest)
                    showAddQuestDialog = false
                }
            )
        }
    }
}

@Composable
fun QuestCard(quest: Quest, onClick: () -> Unit, onAddToCalendar: (Quest) -> Unit = {}) {
    val difficultyColor = when (quest.difficulty) {
        QuestDifficulty.TRIVIAL -> Color(0xFF8bc34a)
        QuestDifficulty.EASY -> Color(0xFF4caf50)
        QuestDifficulty.MEDIUM -> Color(0xFFFF9800)
        QuestDifficulty.HARD -> Color(0xFFf44336)
        QuestDifficulty.EPIC -> Color(0xFF9c27b0)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0f3460)),
        border = BorderStroke(1.dp, difficultyColor),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = quest.title, color = Color.White, fontFamily = PixelFontFamily, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "${quest.description} | Награда: ${quest.xpReward} XP, ${quest.goldReward} G", color = Color(0xFFa0a0a0), fontFamily = PixelFontFamily, fontSize = 10.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Сложность: ${quest.difficulty}", color = difficultyColor, fontFamily = PixelFontFamily, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                }
                IconButton(onClick = { onClick() }) {
                    Icon(imageVector = Icons.Default.Star, contentDescription = "Complete", tint = difficultyColor, modifier = Modifier.size(32.dp).rotate(15f))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { onAddToCalendar(quest) }) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "В календарь",
                        tint = Color(0xFFa0a0a0),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("В календарь", color = Color(0xFFa0a0a0), fontFamily = PixelFontFamily, fontSize = 10.sp)
                }
            }
        }
    }
}

// Карточка для недельного босса - с более эпичным оформлением
@Composable
fun WeeklyBossCard(quest: Quest, onClick: () -> Unit) {
    val bossColor = Color(0xFF9c27b0)
    
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1a0a2e)),
        border = BorderStroke(3.dp, bossColor),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                Text(text = "👑 БОСС НЕДЕЛИ 👑", color = bossColor, fontFamily = PixelFontFamily, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = quest.title, color = Color(0xFFffd700), fontFamily = PixelFontFamily, fontSize = 16.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = quest.description, color = Color(0xFFd0d0d0), fontFamily = PixelFontFamily, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly, 
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "⚔️ ${quest.xpReward} XP", color = Color(0xFFFFD700), fontFamily = PixelFontFamily, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Text(text = "💰 ${quest.goldReward} G", color = Color(0xFFC0C0C0), fontFamily = PixelFontFamily, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = "Сложность: ${quest.difficulty}", color = bossColor, fontFamily = PixelFontFamily, fontSize = 10.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun LevelUpDialog(oldLevel: Int, newLevel: Int, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎉 НОВЫЙ УРОВЕНЬ!", color = Color(0xFFFFD700), fontFamily = PixelFontFamily, textAlign = TextAlign.Center) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Уровень $oldLevel → $newLevel", color = Color.White, fontFamily = PixelFontFamily, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Твои силы растут! Открываются новые возможности.", color = Color.Gray, fontFamily = PixelFontFamily, textAlign = TextAlign.Center)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe94560)), shape = RoundedCornerShape(4.dp)) {
                Text("ПРОДОЛЖИТЬ", fontFamily = PixelFontFamily, color = Color.White)
            }
        },
        containerColor = Color(0xFF16213e),
        shape = RoundedCornerShape(12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestHistoryDialog(
    history: List<CompletedQuestEntry>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "📜 ИСТОРИЯ КВЕСТОВ", 
                color = Color(0xFFFFD700), 
                fontFamily = PixelFontFamily, 
                textAlign = TextAlign.Center,
                fontSize = 18.sp
            ) 
        },
        text = {
            if (history.isEmpty()) {
                Text(
                    "Пока нет выполненных квестов за сегодня",
                    color = Color.Gray,
                    fontFamily = PixelFontFamily,
                    textAlign = TextAlign.Center
                )
            } else {
                // Группируем по типу квеста
                val dailyQuests = history.filter { it.questType == QuestType.DAILY || it.questType == QuestType.CUSTOM }
                val weeklyQuests = history.filter { it.questType == QuestType.WEEKLY }
                
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (dailyQuests.isNotEmpty()) {
                        item {
                            Text(
                                "⚔️ ЕЖЕДНЕВНЫЕ/ПОЛЬЗОВАТЕЛЬСКИЕ",
                                color = Color(0xFFe94560),
                                fontFamily = PixelFontFamily,
                                fontSize = 10.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(dailyQuests.reversed()) { entry ->
                            QuestHistoryItem(entry)
                        }
                    }
                    
                    if (weeklyQuests.isNotEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "👑 НЕДЕЛЬНЫЕ БОССЫ",
                                color = Color(0xFF9c27b0),
                                fontFamily = PixelFontFamily,
                                fontSize = 10.sp,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        items(weeklyQuests.reversed()) { entry ->
                            QuestHistoryItem(entry, isWeekly = true)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss, 
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFe94560)), 
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("ЗАКРЫТЬ", fontFamily = PixelFontFamily, color = Color.White)
            }
        },
        containerColor = Color(0xFF16213e),
        shape = RoundedCornerShape(12.dp)
    )
}

@Composable
fun QuestHistoryItem(entry: CompletedQuestEntry, isWeekly: Boolean = false) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = if (isWeekly) Color(0xFF1a0a2e) else Color(0xFF0f3460)),
        border = BorderStroke(1.dp, if (isWeekly) Color(0xFF9c27b0) else Color.Transparent),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "✅ ${entry.questTitle}",
                color = if (isWeekly) Color(0xFFffd700) else Color.White,
                fontFamily = PixelFontFamily,
                fontSize = 12.sp,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "+${entry.xpReward} XP | +${entry.goldReward} G",
                    color = Color(0xFFffd700),
                    fontFamily = PixelFontFamily,
                    fontSize = 10.sp
                )
                Text(
                    text = entry.completedAt.toString().substring(11, 16),
                    color = Color.Gray,
                    fontFamily = PixelFontFamily,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomQuestDialog(
    onDismiss: () -> Unit,
    onQuestAdded: (Quest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDifficulty by remember { mutableStateOf(QuestDifficulty.EASY) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { 
            Text(
                "⚔️ СОЗДАТЬ КВЕСТ", 
                color = Color(0xFFe94560), 
                fontFamily = PixelFontFamily,
                fontSize = 18.sp
            ) 
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название квеста", fontFamily = PixelFontFamily, fontSize = 10.sp) },
                    placeholder = { Text("Например: Сделать 20 приседаний", fontFamily = PixelFontFamily, fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = PixelFontFamily, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание", fontFamily = PixelFontFamily, fontSize = 10.sp) },
                    placeholder = { Text("Детали...", fontFamily = PixelFontFamily, fontSize = 10.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = PixelFontFamily, fontSize = 12.sp)
                )

                Column {
                    Text("Сложность", color = Color(0xFFffd700), fontFamily = PixelFontFamily, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    listOf(
                        QuestDifficulty.TRIVIAL to "Тривиальная (10 XP, 5 G)",
                        QuestDifficulty.EASY to "Легкая (25 XP, 10 G)",
                        QuestDifficulty.MEDIUM to "Средняя (50 XP, 25 G)",
                        QuestDifficulty.HARD to "Сложная (100 XP, 50 G)",
                        QuestDifficulty.EPIC to "Эпическая (100 XP, 50 G)*"
                    ).forEach { (difficulty, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDifficulty == difficulty,
                                onClick = { selectedDifficulty = difficulty }
                            )
                            Text(
                                label, 
                                fontFamily = PixelFontFamily, 
                                fontSize = 10.sp,
                                color = if (difficulty == QuestDifficulty.EPIC) Color.Gray else Color.White
                            )
                        }
                    }
                    Text(
                        "* Максимальная награда ограничена 100 XP и 50 G",
                        color = Color.Gray,
                        fontFamily = PixelFontFamily,
                        fontSize = 8.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val (xp, gold) = when (selectedDifficulty) {
                            QuestDifficulty.TRIVIAL -> 10 to 5
                            QuestDifficulty.EASY -> 25 to 10
                            QuestDifficulty.MEDIUM -> 50 to 25
                            QuestDifficulty.HARD -> 100 to 50
                            QuestDifficulty.EPIC -> 100 to 50 // Лимит
                        }
                        
                        onQuestAdded(
                            Quest(
                                title = title,
                                description = description,
                                type = QuestType.CUSTOM,
                                difficulty = selectedDifficulty,
                                xpReward = xp,
                                goldReward = gold
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4caf50)),
                shape = RoundedCornerShape(4.dp)
            ) {
                Text("СОЗДАТЬ", fontFamily = PixelFontFamily, color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("ОТМЕНА", fontFamily = PixelFontFamily, color = Color.White)
            }
        },
        containerColor = Color(0xFF16213e),
        shape = RoundedCornerShape(12.dp)
    )
}

data class FloatingReward(val text: String, val color: Color, val id: Int)
