package com.questlife.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.questlife.app.models.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddQuestDialog(
    onDismiss: () -> Unit,
    onQuestAdded: (HealthQuest) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(QuestType.DAILY) }
    var selectedDifficulty by remember { mutableStateOf(QuestDifficulty.EASY) }
    var isDaily by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Создать новый квест") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Название квеста
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название квеста") },
                    placeholder = { Text("Например: Сделать 30 отжиманий") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Описание
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (опционально)") },
                    placeholder = { Text("Детали квеста...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Тип квеста
                Column {
                    Text(
                        "Тип квеста",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val types = listOf(
                        QuestType.DAILY to "📅 Ежедневный",
                        QuestType.WEEKLY to "📆 Недельный",
                        QuestType.MONTHLY to "📅 Месячный",
                        QuestType.CUSTOM to "⚡ Другое"
                    )

                    types.forEach { (type, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            TextButton(onClick = { selectedType = type }) {
                                Text(label)
                            }
                        }
                    }
                }

                // Сложность
                Column {
                    Text(
                        "Сложность",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val difficulties = listOf(
                        QuestDifficulty.TRIVIAL to "Тривиальная",
                        QuestDifficulty.EASY to "Легкая",
                        QuestDifficulty.MEDIUM to "Средняя",
                        QuestDifficulty.HARD to "Сложная",
                        QuestDifficulty.EPIC to "Эпическая"
                    )

                    difficulties.forEach { (difficulty, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedDifficulty == difficulty,
                                onClick = { selectedDifficulty = difficulty }
                            )
                            TextButton(onClick = { selectedDifficulty = difficulty }) {
                                Text(label)
                            }
                        }
                    }
                }

                // Ежедневный квест
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Ежедневный квест")
                    Switch(
                        checked = isDaily,
                        onCheckedChange = { isDaily = it }
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
                            QuestDifficulty.EPIC -> 200 to 100
                        }

                        onQuestAdded(
                            HealthQuest(
                                title = title,
                                description = description,
                                type = selectedType,
                                difficulty = selectedDifficulty,
                                xpReward = xp,
                                goldReward = gold,
                                isCompleted = false
                            )
                        )
                        onDismiss()
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Создать квест")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}