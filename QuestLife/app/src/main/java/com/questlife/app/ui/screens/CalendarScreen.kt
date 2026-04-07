package com.questlife.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questlife.app.models.CalendarTask
import com.questlife.app.models.QuestCategory
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onBackClick: () -> Unit,
    tasks: List<CalendarTask>,
    onTaskAdded: (CalendarTask) -> Unit,
    onTaskCompleted: (String) -> Unit,
    onTaskDeleted: (String) -> Unit
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Календарь квестов") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddTaskDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Добавить квест")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFF1a1a2e))
        ) {
            // Выбор даты
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { 
                    selectedDate = selectedDate.minusDays(1) 
                }) {
                    Text("<", fontSize = 24.sp, color = Color.White)
                }
                
                Text(
                    text = selectedDate.format(dateFormatter),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                IconButton(onClick = { 
                    selectedDate = selectedDate.plusDays(1) 
                }) {
                    Text(">", fontSize = 24.sp, color = Color.White)
                }
            }
            
            // Список задач на выбранный день
            val tasksForDay = tasks.filter { 
                it.dateTime.toLocalDate() == selectedDate 
            }.sortedBy { it.dateTime }
            
            if (tasksForDay.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Нет квестов на этот день\nНажмите + чтобы добавить",
                        color = Color.Gray,
                        fontSize = 16.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(tasksForDay) { task ->
                        TaskCard(
                            task = task,
                            onToggleComplete = { onTaskCompleted(task.id) },
                            onDelete = { onTaskDeleted(task.id) }
                        )
                    }
                }
            }
        }
    }
    
    if (showAddTaskDialog) {
        AddCalendarTaskDialog(
            onDismiss = { showAddTaskDialog = false },
            onTaskAdded = { newTask ->
                onTaskAdded(newTask)
                showAddTaskDialog = false
            }
        )
    }
}

@Composable
fun TaskCard(
    task: CalendarTask,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggleComplete() },
        colors = CardDefaults.cardColors(
            containerColor = if (task.isCompleted) Color(0xFF2a2a3e) else Color(0xFF3a3a5e)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (task.isCompleted) Color.Gray else Color.White,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Text(
                    text = task.dateTime.format(timeFormatter),
                    fontSize = 14.sp,
                    color = Color.LightGray
                )
                
                if (task.reminderEnabled && task.reminderTime != null) {
                    Text(
                        text = "⏰ ${task.reminderTime.format(timeFormatter)}",
                        fontSize = 12.sp,
                        color = Color(0xFFFFD700)
                    )
                }
            }
            
            Row {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onToggleComplete() },
                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF4CAF50))
                )
                
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Удалить",
                        tint = Color.Red
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCalendarTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdded: (CalendarTask) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDateTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var hasReminder by remember { mutableStateOf(false) }
    var reminderTime by remember { mutableStateOf<LocalDateTime?>(null) }
    var selectedCategory by remember { mutableStateOf(QuestCategory.HEALTH) }
    
    val dateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добавить квест в календарь") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название квеста") },
                    placeholder = { Text("Например: Тренировка") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (опционально)") },
                    placeholder = { Text("Детали квеста...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Column {
                    Text("Дата и время выполнения", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedDateTime?.format(dateTimeFormatter) ?: "Не выбрано")
                        Button(onClick = { 
                            selectedDateTime = LocalDateTime.now().plusDays(1).withHour(10).withMinute(0)
                        }) {
                            Text("Выбрать")
                        }
                    }
                }
                
                if (selectedDateTime != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Установить напоминание")
                        Switch(
                            checked = hasReminder,
                            onCheckedChange = { hasReminder = it }
                        )
                    }
                    
                    if (hasReminder) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Время напоминания:")
                            Button(onClick = { 
                                reminderTime = selectedDateTime?.minusMinutes(30) 
                            }) {
                                Text("За 30 мин до")
                            }
                        }
                    }
                }
                
                Column {
                    Text("Категория", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val categories = listOf(
                        QuestCategory.HEALTH to "Здоровье",
                        QuestCategory.PHYSICAL to "Физическое",
                        QuestCategory.MENTAL to "Ментальное",
                        QuestCategory.SOCIAL to "Социальное",
                        QuestCategory.CREATIVE to "Творческое"
                    )
                    
                    categories.forEach { (category, label) ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category }
                            )
                            TextButton(onClick = { selectedCategory = category }) {
                                Text(label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && selectedDateTime != null) {
                        onTaskAdded(
                            CalendarTask(
                                title = title,
                                description = description,
                                dateTime = selectedDateTime!!,
                                isCompleted = false,
                                reminderEnabled = hasReminder,
                                reminderTime = if (hasReminder) reminderTime else null,
                                category = selectedCategory
                            )
                        )
                    }
                },
                enabled = title.isNotBlank() && selectedDateTime != null
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
