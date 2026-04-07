package com.questlife.app.data

import com.questlife.app.models.*
import java.time.LocalDate
import android.content.Context
import android.content.SharedPreferences

// Extension функция для удобного доступа к формуле опыта
fun getXpRequiredForLevel(level: Int): Int = MegaQuestDatabase.getXpRequiredForLevel(level)

object MegaQuestDatabase {
    
    // SharedPreferences для хранения состояния (будет инициализировано из контекста)
    private var prefs: SharedPreferences? = null
    
    fun init(context: Context) {
        prefs = context.getSharedPreferences("mega_quest_prefs", Context.MODE_PRIVATE)
        loadCompletedWeeklyQuests()
    }
    
    private const val PREF_COMPLETED_WEEKLY = "completed_weekly_quests"
    private const val PREF_LAST_RESET_WEEK = "last_reset_week"
    private const val PREF_COMPLETED_QUESTS_HISTORY = "completed_quests_history"
    
    // Хранилище для отслеживания выполненных недельных квестов
    private var completedWeeklyQuestIds = mutableSetOf<String>()
    private var lastResetDate = LocalDate.now()
    
    // История выполненных квестов за сегодня
    private var completedQuestsHistory = mutableListOf<CompletedQuestEntry>()

    // Базовые квесты (общие для всех)
    private val commonQuests = listOf(
        QuestTemplate("План на день", "Составьте список дел на сегодня", QuestDifficulty.EASY, 15, 10),
        QuestTemplate("Зарядка", "10 минут утренней зарядки", QuestDifficulty.EASY, 10, 5),
        QuestTemplate("Чтение", "Прочитайте 10 страниц книги", QuestDifficulty.EASY, 20, 10),
        QuestTemplate("Медитация", "5 минут медитации", QuestDifficulty.EASY, 15, 8),
        QuestTemplate("Прогулка", "30 минут на свежем воздухе", QuestDifficulty.EASY, 20, 12),
        QuestTemplate("Вода", "Выпейте 8 стаканов воды", QuestDifficulty.EASY, 10, 5),
        QuestTemplate("Уборка", "Приберите рабочее место", QuestDifficulty.EASY, 15, 8),
        QuestTemplate("Дневник", "Запишите 3 достижения дня", QuestDifficulty.EASY, 12, 6),
        QuestTemplate("Обучение", "30 минут изучения нового навыка", QuestDifficulty.MEDIUM, 25, 15),
        QuestTemplate("Контакты", "Позвоните другу или родственнику", QuestDifficulty.EASY, 10, 5)
    )

    // Квесты для СТУДЕНТА (50 штук)
    private val studentQuests = listOf(
        QuestTemplate("Конспект лекции", "Сделайте конспект последней лекции", QuestDifficulty.MEDIUM, 20, 12, Profession.STUDENT),
        QuestTemplate("Домашнее задание", "Выполните домашнее задание", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Подготовка к экзамену", "Повторите материал для экзамена", QuestDifficulty.HARD, 30, 20, Profession.STUDENT),
        QuestTemplate("Библиотека", "Посетите библиотеку и возьмите книгу", QuestDifficulty.EASY, 15, 10, Profession.STUDENT),
        QuestTemplate("Групповой проект", "Работа над групповым проектом", QuestDifficulty.HARD, 35, 25, Profession.STUDENT),
        QuestTemplate("Научная статья", "Прочитайте научную статью по специальности", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Лабораторная работа", "Подготовьтесь к лабораторной работе", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Консультация преподавателя", "Посетите консультацию преподавателя", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Тестирование", "Пройдите онлайн-тест по предмету", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Презентация", "Подготовьте презентацию для семинара", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Курсовая работа", "Напишите главу курсовой работы", QuestDifficulty.HARD, 40, 30, Profession.STUDENT),
        QuestTemplate("Семинар", "Активно участвуйте в семинаре", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Дополнительная литература", "Изучите дополнительный материал", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Практические упражнения", "Решите 10 практических задач", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Видеолекция", "Посмотрите видеолекцию по теме", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Конспектирование", "Сделайте краткий конспект главы учебника", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Обсуждение", "Обсудите тему с одногруппниками", QuestDifficulty.EASY, 15, 10, Profession.STUDENT),
        QuestTemplate("Повторение", "Повторите пройденный материал", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Новый термин", "Выучите 5 новых терминов", QuestDifficulty.EASY, 15, 10, Profession.STUDENT),
        QuestTemplate("Исследование", "Проведите мини-исследование по теме", QuestDifficulty.HARD, 35, 25, Profession.STUDENT),
        QuestTemplate("Реферат", "Напишите реферат на заданную тему", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Контрольная работа", "Подготовьтесь к контрольной работе", QuestDifficulty.HARD, 35, 25, Profession.STUDENT),
        QuestTemplate("Доклад", "Подготовьте доклад для выступления", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Анализ случая", "Проанализируйте кейс из практики", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Мозговой штурм", "Проведите мозговой штурм по проекту", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Флеш-карточки", "Создайте флеш-карточки для запоминания", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Онлайн-курс", "Пройдите модуль онлайн-курса", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Вебинар", "Посетите образовательный вебинар", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Практика", "Отработайте практический навык", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Самопроверка", "Проведите самопроверку знаний", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Эссе", "Напишите эссе на заданную тему", QuestDifficulty.HARD, 35, 25, Profession.STUDENT),
        QuestTemplate("График", "Составьте график обучения на неделю", QuestDifficulty.EASY, 15, 10, Profession.STUDENT),
        QuestTemplate("Цели", "Определите учебные цели на месяц", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Портфолио", "Добавьте работу в портфолио", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Стипендия", "Подготовьте документы на стипендию", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Конференция", "Узнайте о студенческой конференции", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Олимпиада", "Подготовьтесь к олимпиаде", QuestDifficulty.HARD, 40, 30, Profession.STUDENT),
        QuestTemplate("Стажировка", "Найдите информацию о стажировках", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Резюме", "Обновите свое резюме", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Профориентация", "Пройдите тест по профориентации", QuestDifficulty.EASY, 15, 10, Profession.STUDENT),
        QuestTemplate("Ментор", "Найдите ментора в своей области", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Нетворкинг", "Познакомьтесь со студентом старшего курса", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Проект", "Начните новый учебный проект", QuestDifficulty.HARD, 35, 25, Profession.STUDENT),
        QuestTemplate("Английский", "Изучите 10 профессиональных терминов на английском", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT),
        QuestTemplate("Статья", "Найдите интересную статью по специальности", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Видео", "Смотрите образовательное видео", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Тест", "Пройдите пробный тест экзамена", QuestDifficulty.MEDIUM, 30, 20, Profession.STUDENT),
        QuestTemplate("Расписание", "Оптимизируйте свое расписание", QuestDifficulty.EASY, 15, 10, Profession.STUDENT),
        QuestTemplate("Мотивация", "Напишите мотивационное письмо себе", QuestDifficulty.EASY, 20, 12, Profession.STUDENT),
        QuestTemplate("Итоги", "Подведите итоги учебной недели", QuestDifficulty.MEDIUM, 25, 15, Profession.STUDENT)
    )

    // Квесты для МЕНЕДЖЕРА (50 штук)
    private val managerQuests = listOf(
        QuestTemplate("Планерка", "Проведите утреннюю планерку", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Отчет", "Составьте еженедельный отчет", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Встреча", "Проведите встречу с командой", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("KPI", "Проанализируйте KPI сотрудников", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Делегирование", "Делегируйте 3 задачи сотрудникам", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Бюджет", "Скорректируйте бюджет проекта", QuestDifficulty.HARD, 40, 30, Profession.MANAGER),
        QuestTemplate("Обратная связь", "Дайте обратную связь сотруднику", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Стратегия", "Разработайте стратегию на квартал", QuestDifficulty.HARD, 45, 35, Profession.MANAGER),
        QuestTemplate("Риск-менеджмент", "Оцените риски проекта", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Мотивация", "Придумайте способ мотивации команды", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Документация", "Обновите проектную документацию", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Переговоры", "Проведите переговоры с партнером", QuestDifficulty.HARD, 40, 30, Profession.MANAGER),
        QuestTemplate("Анализ", "Проанализируйте эффективность процессов", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("План развития", "Составьте план развития сотрудника", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Совещание", "Организуйте межотдельческое совещание", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Цели", "Поставьте цели команде на неделю", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Контроль", "Проконтролируйте выполнение задач", QuestDifficulty.EASY, 20, 12, Profession.MANAGER),
        QuestTemplate("Оптимизация", "Найдите способ оптимизации процесса", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Обучение", "Организуйте обучение для команды", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Инновации", "Предложите новую идею для улучшения", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Коучинг", "Проведите коучинг-сессию", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Приоритеты", "Определите приоритеты задач", QuestDifficulty.EASY, 20, 12, Profession.MANAGER),
        QuestTemplate("Коммуникация", "Наладьте коммуникацию между отделами", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Ресурсы", "Распределите ресурсы проекта", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Дедлайн", "Контролируйте соблюдение дедлайнов", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Клиент", "Встретьтесь с ключевым клиентом", QuestDifficulty.HARD, 40, 30, Profession.MANAGER),
        QuestTemplate("Конфликт", "Разрешите конфликт в команде", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Презентация", "Подготовьте презентацию для руководства", QuestDifficulty.HARD, 40, 30, Profession.MANAGER),
        QuestTemplate("Аудит", "Проведите аудит текущего проекта", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("План Б", "Разработайте план Б для проекта", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Нетворкинг", "Посетите отраслевое мероприятие", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Менторство", "Наставничайте младшего менеджера", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Автоматизация", "Найдите процесс для автоматизации", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Брендинг", "Улучшите имидж команды", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Тайм-менеджмент", "Оптимизируйте свое расписание", QuestDifficulty.EASY, 20, 12, Profession.MANAGER),
        QuestTemplate("Обучение сотрудника", "Научите сотрудника новому навыку", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Анализ рынка", "Изучите действия конкурентов", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Инструменты", "Внедрите новый инструмент управления", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Команда", "Проведите тимбилдинг", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Отчетность", "Подготовьте месячный отчет", QuestDifficulty.HARD, 40, 30, Profession.MANAGER),
        QuestTemplate("Прогноз", "Сделайте прогноз на следующий период", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Улучшение", "Внедрите одно улучшение в процесс", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Компетенции", "Оцените компетенции команды", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Видение", "Сформулируйте видение проекта", QuestDifficulty.HARD, 35, 25, Profession.MANAGER),
        QuestTemplate("Эффективность", "Измерьте эффективность команды", QuestDifficulty.MEDIUM, 30, 20, Profession.MANAGER),
        QuestTemplate("Лидерство", "Проявите лидерские качества", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Решение", "Примите сложное управленческое решение", QuestDifficulty.HARD, 40, 30, Profession.MANAGER),
        QuestTemplate("Развитие", "Составьте план своего развития", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER),
        QuestTemplate("Успех", "Отпразднуйте успех команды", QuestDifficulty.EASY, 20, 12, Profession.MANAGER),
        QuestTemplate("Рефлексия", "Проведите рефлексию недели", QuestDifficulty.MEDIUM, 25, 15, Profession.MANAGER)
    )

    // Квесты для ПРОГРАММИСТА (50 штук)
    private val programmerQuests = listOf(
        QuestTemplate("Код-ревью", "Проведите код-ревью коллеги", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Алгоритм", "Решите задачу на алгоритмы", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Рефакторинг", "Улучшите существующий код", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Документация", "Напишите документацию к модулю", QuestDifficulty.MEDIUM, 25, 15, Profession.PROGRAMMER),
        QuestTemplate("Баг", "Исправьте критический баг", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Тесты", "Напишите юнит-тесты", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Фреймворк", "Изучите новую функцию фреймворка", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Оптимизация", "Оптимизируйте производительность кода", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("API", "Разработайте новый API endpoint", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("База данных", "Оптимизируйте SQL-запрос", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Git", "Сделайте чистый commit с хорошим описанием", QuestDifficulty.EASY, 20, 12, Profession.PROGRAMMER),
        QuestTemplate("Архитектура", "Спроектируйте архитектуру модуля", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Безопасность", "Проверьте код на уязвимости", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Микросервис", "Разработайте микросервис", QuestDifficulty.HARD, 45, 35, Profession.PROGRAMMER),
        QuestTemplate("CI/CD", "Настройте пайплайн сборки", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Деплой", "Разверните приложение на сервере", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Мониторинг", "Настройте мониторинг приложения", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Логирование", "Улучшите систему логирования", QuestDifficulty.MEDIUM, 25, 15, Profession.PROGRAMMER),
        QuestTemplate("Паттерн", "Примените паттерн проектирования", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Библиотека", "Изучите новую библиотеку", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Хакатон", "Участвуйте в хакатоне или коде-кате", QuestDifficulty.EPIC, 50, 40, Profession.PROGRAMMER),
        QuestTemplate("Стек", "Освойте новый инструмент в стеке", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Прототип", "Создайте прототип функции", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Интеграция", "Интегрируйте сторонний сервис", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Кэш", "Реализуйте кэширование данных", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Асинхронность", "Реализуйте асинхронную операцию", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Валидация", "Добавьте валидацию входных данных", QuestDifficulty.MEDIUM, 25, 15, Profession.PROGRAMMER),
        QuestTemplate("Интерфейс", "Спроектируйте пользовательский интерфейс", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Миграция", "Выполните миграцию базы данных", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Контейнер", "Создайте Docker-контейнер", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Очереди", "Реализуйте очередь сообщений", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Аутентификация", "Настройте систему аутентификации", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Шифрование", "Реализуйте шифрование данных", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Поиск", "Реализуйте полнотекстовый поиск", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Уведомления", "Настройте систему уведомлений", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Файлы", "Реализуйте загрузку файлов", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Платежи", "Интегрируйте платежную систему", QuestDifficulty.HARD, 45, 35, Profession.PROGRAMMER),
        QuestTemplate("Аналитика", "Добавьте аналитику событий", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Локализация", "Добавьте поддержку языков", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Доступность", "Улучшите доступность интерфейса", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Производительность", "Профилируйте приложение", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Мобильная версия", "Адаптируйте под мобильные устройства", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("SEO", "Оптимизируйте для поисковиков", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Кодстайл", "Приведите код к единому стилю", QuestDifficulty.MEDIUM, 25, 15, Profession.PROGRAMMER),
        QuestTemplate("Туториал", "Пройдите туториал по новой технологии", QuestDifficulty.HARD, 35, 25, Profession.PROGRAMMER),
        QuestTemplate("Блог", "Напишите техническую статью", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Open Source", "Сделайте вклад в open source проект", QuestDifficulty.HARD, 45, 35, Profession.PROGRAMMER),
        QuestTemplate("Митап", "Посетите технический митап", QuestDifficulty.MEDIUM, 30, 20, Profession.PROGRAMMER),
        QuestTemplate("Сертификация", "Подготовьтесь к сертификации", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER),
        QuestTemplate("Пет-проект", "Работайте над личным проектом", QuestDifficulty.HARD, 40, 30, Profession.PROGRAMMER)
    )

    // Объединяем все профессиональные квесты
    private val professionQuestsMap = mapOf(
        Profession.STUDENT to studentQuests,
        Profession.MANAGER to managerQuests,
        Profession.PROGRAMMER to programmerQuests
    )

    // Квесты для всех RPG/Sci-Fi классов (по 10 на класс)
    private val classQuestsMap = mapOf(
        CharacterClass.WARRIOR to listOf(
            QuestTemplate("Тренировка силы", "Сделайте силовую тренировку", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Боевая стойка", "Отработайте боевую стойку", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Выносливость", "Пробежка 5 км", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Защита", "Изучите приемы защиты", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Атака", "Отработайте удары", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Тактика", "Изучите тактику боя", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Оружие", "Тренировка с оружием", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Реакция", "Упражнения на реакцию", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Дух воина", "Медитация воина", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.WARRIOR),
            QuestTemplate("Победа", "Достигните личной победы", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.WARRIOR)
        ),
        CharacterClass.MAGE to listOf(
            QuestTemplate("Изучение заклинаний", "Изучите новое заклинание", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Медитация маны", "Медитируйте для восстановления маны", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Книга знаний", "Прочитайте магический трактат", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Контроль стихии", "Практикуйте контроль стихии", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Ритуал", "Проведите магический ритуал", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Зельеварение", "Сварите магическое зелье", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Предвидение", "Практикуйте ясновидение", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Магический круг", "Нарисуйте магический круг", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Телекинез", "Тренировка телекинеза", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MAGE),
            QuestTemplate("Мудрость мага", "Размышляйте о тайнах мироздания", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.MAGE)
        ),
        CharacterClass.ARCHER to listOf(
            QuestTemplate("Меткость", "Тренировка меткости", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Глазомер", "Упражнения на глазомер", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Луки", "Обслуживание лука", QuestDifficulty.EASY, 15, 10, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Стрельба в движении", "Отработайте стрельбу в движении", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Скорость", "Быстрая перезарядка", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Дальний выстрел", "Попадание в дальнюю цель", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Скрытность", "Тренировка скрытности", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Охота", "Подготовка к охоте", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Ветер", "Учет направления ветра", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.ARCHER),
            QuestTemplate("Соколиный глаз", "Развитие остроты зрения", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.ARCHER)
        ),
        CharacterClass.KNIGHT to listOf(
            QuestTemplate("Честь рыцаря", "Совершите благородный поступок", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Броня", "Уход за доспехами", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Клятва", "Подтвердите свою клятву", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Фехтование", "Тренировка фехтования", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Защита слабых", "Помогите кому-нибудь", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Верховая езда", "Тренировка верховой езды", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Тактика боя", "Изучите рыцарскую тактику", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Кодекс", "Изучите рыцарский кодекс", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Щит", "Тренировка со щитом", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.KNIGHT),
            QuestTemplate("Доблесть", "Проявите доблесть", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.KNIGHT)
        ),
        CharacterClass.THIEF to listOf(
            QuestTemplate("Скрытность", "Тренировка скрытности", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Ловкость", "Упражнения на ловкость", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Взлом", "Изучите основы взлома", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Карманник", "Тренировка карманной техники", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Тени", "Передвижение в тени", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Яды", "Изучение ядов", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Кинжалы", "Метание кинжалов", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Наблюдение", "Следите за целью", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Побег", "Тренировка побега", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.THIEF),
            QuestTemplate("Тишина", "Бесшумное передвижение", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.THIEF)
        ),
        CharacterClass.PRIEST to listOf(
            QuestTemplate("Молитва", "Утренняя молитва", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Исцеление", "Помогите больному", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Проповедь", "Подготовьте проповедь", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Ритуал очищения", "Проведите ритуал очищения", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Милосердие", "Проявите милосердие", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Священная книга", "Изучите священный текст", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Благословение", "Благословите кого-то", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Пост", "Соблюдайте пост", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Духовное наставление", "Дайте духовный совет", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PRIEST),
            QuestTemplate("Вера", "Укрепите свою веру", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.PRIEST)
        ),
        CharacterClass.NECROMANCER to listOf(
            QuestTemplate("Темный ритуал", "Проведите темный ритуал", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Кости", "Изучите древние кости", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Нежить", "Призовите нежить", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Темная магия", "Изучите заклинание темной магии", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Кладбище", "Посетите кладбище ночью", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Души", "Изучите природу душ", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Проклятие", "Снимите проклятие", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Гримуар", "Запишите заклинание в гримуар", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Тьма", "Слейте с тьмой", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.NECROMANCER),
            QuestTemplate("Вечность", "Размышляйте о вечности", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.NECROMANCER)
        ),
        CharacterClass.DRUID to listOf(
            QuestTemplate("Единство с природой", "Проведите время на природе", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Целительство", "Исцелите растение", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Животные", "Покормите животных", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Стихии", "Призовите силу стихий", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Лунный свет", "Медитация при лунном свете", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Древесная магия", "Общение с деревьями", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Трансформация", "Представьте себя животным", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Семена", "Посадите семена", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Баланс", "Восстановите баланс", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DRUID),
            QuestTemplate("Мудрость леса", "Слушайте лес", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.DRUID)
        ),
        CharacterClass.PALADIN to listOf(
            QuestTemplate("Священная клятва", "Подтвердите священную клятву", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Свет", "Распространяйте свет", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Защита", "Защитите слабого", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Молитва света", "Молитесь о свете", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Правосудие", "Вершите правосудие", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Святое оружие", "Освятите оружие", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Доброта", "Совершите доброе дело", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Вера", "Укрепите веру в сердце", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Аура", "Создайте защитную ауру", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.PALADIN),
            QuestTemplate("Честь", "Живите по чести", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.PALADIN)
        ),
        CharacterClass.BARD to listOf(
            QuestTemplate("Песня", "Спойте песню", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.BARD),
            QuestTemplate("Инструмент", "Играйте на инструменте", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BARD),
            QuestTemplate("Стихи", "Напишите стихи", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BARD),
            QuestTemplate("Выступление", "Выступите перед публикой", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.BARD),
            QuestTemplate("Музыкальная история", "Расскажите историю через музыку", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BARD),
            QuestTemplate("Вдохновение", "Вдохновите кого-то", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.BARD),
            QuestTemplate("Новая мелодия", "Сочините новую мелодию", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.BARD),
            QuestTemplate("Легенда", "Узнайте древнюю легенду", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BARD),
            QuestTemplate("Ритм", "Отработайте ритм", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.BARD),
            QuestTemplate("Слава", "Прославьте героя", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BARD)
        ),
        CharacterClass.SORCERER to listOf(
            QuestTemplate("Внутренняя сила", "Активируйте внутреннюю силу", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Магический дар", "Используйте врожденный дар", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Контроль", "Контролируйте магический поток", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Интуиция", "Следуйте интуиции", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Хаос", "Укротите хаос", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Энергия", "Накопите магическую энергию", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Трансмутация", "Проведите трансмутацию", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Видения", "Запишите видения", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Поток", "Войдите в магический поток", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.SORCERER),
            QuestTemplate("Мастерство", "Достигните мастерства", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.SORCERER)
        ),
        CharacterClass.MONK to listOf(
            QuestTemplate("Медитация", "Глубокая медитация", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.MONK),
            QuestTemplate("Боевые искусства", "Тренировка боевых искусств", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MONK),
            QuestTemplate("Дисциплина", "Соблюдайте дисциплину", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MONK),
            QuestTemplate("Дыхание", "Дыхательные упражнения", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.MONK),
            QuestTemplate("Баланс", "Достигните внутреннего баланса", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MONK),
            QuestTemplate("Ци", "Контролируйте ци", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MONK),
            QuestTemplate("Аскеза", "Практикуйте аскезу", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MONK),
            QuestTemplate("Мудрость", "Изучите мудрость предков", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MONK),
            QuestTemplate("Покой", "Достигните душевного покоя", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.MONK),
            QuestTemplate("Путь", "Следуйте своим путем", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MONK)
        ),
        CharacterClass.DEMON_HUNTER to listOf(
            QuestTemplate("Охота", "Выследите цель", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Темная сила", "Используйте темную силу", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Оружие охотника", "Подготовьте оружие", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Следопыт", "Изучите следы", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Бой с тьмой", "Сразитесь с тьмой внутри", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Месть", "Помните о мести", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Ловушки", "Установите ловушки", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Зрение", "Активируйте демоническое зрение", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Прыжок", "Отработайте прыжок", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DEMON_HUNTER),
            QuestTemplate("Истребление", "Уничтожьте зло", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.DEMON_HUNTER)
        ),
        CharacterClass.ALCHEMIST to listOf(
            QuestTemplate("Зелье", "Сварите целебное зелье", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Ингредиенты", "Соберите ингредиенты", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Рецепт", "Изучите новый рецепт", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Трансмутация", "Проведите алхимическую трансмутацию", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Яд", "Создайте противоядие", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Эликсир", "Приготовьте эликсир силы", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Лаборатория", "Приберите в лаборатории", QuestDifficulty.EASY, 20, 12, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Эксперимент", "Проведите эксперимент", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Философский камень", "Исследуйте легенду о камне", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.ALCHEMIST),
            QuestTemplate("Баланс", "Найдите баланс элементов", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.ALCHEMIST)
        ),
        CharacterClass.CYBERNETICIST to listOf(
            QuestTemplate("Киберимплант", "Изучите киберимпланты", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Нейроинтерфейс", "Настройте нейроинтерфейс", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Апгрейд", "Сделайте апгрейд системы", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Код", "Напишите код для импланта", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Синтез", "Синтезируйте человека и машину", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Данные", "Проанализируйте данные", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Прототип", "Создайте прототип устройства", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Сеть", "Подключитесь к сети", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Безопасность", "Обеспечьте кибербезопасность", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.CYBERNETICIST),
            QuestTemplate("Будущее", "Спроектируйте будущее", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.CYBERNETICIST)
        ),
        CharacterClass.SPACE_MARINE to listOf(
            QuestTemplate("Боевая готовность", "Приведите снаряжение в готовность", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Оружие", "Проверьте оружие", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Тактика", "Изучите космическую тактику", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Броня", "Обслуживание брони", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Миссия", "Подготовьтесь к миссии", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Выживание", "Тренировка выживания", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Команда", "Скоординируйтесь с командой", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Десант", "Отработайте десантирование", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Честь", "Защищайте честь", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.SPACE_MARINE),
            QuestTemplate("Победа", "Достигните победы", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.SPACE_MARINE)
        ),
        CharacterClass.HACKER to listOf(
            QuestTemplate("Взлом", "Взломайте систему (легально!)", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Код", "Напишите эксплойт", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Анонимность", "Обеспечьте анонимность", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Шифрование", "Взломайте шифр", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Сеть", "Исследуйте сеть", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Уязвимость", "Найдите уязвимость", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Троян", "Создайте тестового трояна", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Данные", "Извлеките данные", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.HACKER),
            QuestTemplate("След", "Заметите следы", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.HACKER),
            QuestTemplate("Матрица", "Войдите в матрицу", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.HACKER)
        ),
        CharacterClass.DRONE_ENGINEER to listOf(
            QuestTemplate("Дрон", "Соберите дрон", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Полет", "Испытайте полет дрона", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Программирование", "Запрограммируйте дрон", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Камера", "Настройте камеру", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Автопилот", "Настройте автопилот", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Ремонт", "Отремонтируйте дрон", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Маршрут", "Спланируйте маршрут", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Датчики", "Калибруйте датчики", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Рой", "Управляйте роем дронов", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.DRONE_ENGINEER),
            QuestTemplate("Инновация", "Придумайте улучшение", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.DRONE_ENGINEER)
        ),
        CharacterClass.BIOENGINEER to listOf(
            QuestTemplate("Ген", "Изучите генную инженерию", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Лаборатория", "Работа в биолаборатории", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("ДНК", "Проанализируйте ДНК", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Культура", "Вырастите культуру клеток", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Мутация", "Изучите мутации", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Терапия", "Разработайте терапию", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Экосистема", "Создайте мини-экосистему", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Фермент", "Исследуйте ферменты", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Клонирование", "Изучите клонирование", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.BIOENGINEER),
            QuestTemplate("Будущее жизни", "Спроектируйте будущее", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.BIOENGINEER)
        ),
        CharacterClass.MECH_PILOT to listOf(
            QuestTemplate("Мех", "Подготовьте меха к бою", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Кабина", "Настройте кабину пилота", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Синхронизация", "Синхронизируйтесь с мехом", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Оружие меха", "Проверьте системы оружия", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Маневр", "Отработайте маневр", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Броня", "Усильте броню", QuestDifficulty.MEDIUM, 30, 20, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Энергия", "Зарядите энергоядро", QuestDifficulty.EASY, 25, 15, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Тактика", "Изучите тактику боя мехов", QuestDifficulty.HARD, 35, 25, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Миссия", "Выполните боевую миссию", QuestDifficulty.HARD, 40, 30, requiredClass = CharacterClass.MECH_PILOT),
            QuestTemplate("Легенда", "Станьте легендой", QuestDifficulty.EPIC, 50, 40, requiredClass = CharacterClass.MECH_PILOT)
        )
    )

    // Квесты здоровья (8 штук)
    private val healthQuests = listOf(
        QuestTemplate("Пробежка", "Пробежка 30 минут", QuestDifficulty.MEDIUM, 25, 15),
        QuestTemplate("Зарядка", "Утренняя зарядка 10 минут", QuestDifficulty.EASY, 15, 8),
        QuestTemplate("Спортзал", "Посещение спортзала", QuestDifficulty.MEDIUM, 30, 20),
        QuestTemplate("Медитация", "Медитация 10 минут", QuestDifficulty.EASY, 15, 8),
        QuestTemplate("Плавание", "Плавание 30 минут", QuestDifficulty.MEDIUM, 30, 20),
        QuestTemplate("Йога", "Занятие йогой", QuestDifficulty.MEDIUM, 25, 15),
        QuestTemplate("Прогулка", "Пешая прогулка 1 час", QuestDifficulty.EASY, 15, 10),
        QuestTemplate("Растяжка", "Вечерняя растяжка", QuestDifficulty.EASY, 15, 8)
    )

    // Квесты личного развития (7 штук)
    private val personalDevelopmentQuests = listOf(
        QuestTemplate("План", "Составьте план на день", QuestDifficulty.EASY, 10, 5),
        QuestTemplate("Чтение книги", "Прочитайте 20 страниц", QuestDifficulty.MEDIUM, 20, 10),
        QuestTemplate("Навык", "Изучайте новый навык 30 минут", QuestDifficulty.MEDIUM, 25, 15),
        QuestTemplate("Дневник", "Ведите дневник достижений", QuestDifficulty.EASY, 10, 5),
        QuestTemplate("Тайм-менеджмент", "Оптимизируйте расписание", QuestDifficulty.MEDIUM, 20, 10),
        QuestTemplate("Самоанализ", "Проведите самоанализ", QuestDifficulty.MEDIUM, 25, 15),
        QuestTemplate("Цели", "Поставьте цели на неделю", QuestDifficulty.MEDIUM, 20, 10)
    )

    // Бытовые квесты (6 штук)
    private val householdQuests = listOf(
        QuestTemplate("Уборка", "Уборка комнаты", QuestDifficulty.EASY, 15, 10),
        QuestTemplate("Продукты", "Покупка продуктов", QuestDifficulty.EASY, 10, 5),
        QuestTemplate("Готовка", "Приготовление обеда", QuestDifficulty.MEDIUM, 20, 10),
        QuestTemplate("Счета", "Оплата счетов", QuestDifficulty.EASY, 10, 5),
        QuestTemplate("Организация", "Организация пространства", QuestDifficulty.MEDIUM, 25, 15),
        QuestTemplate("Починка", "Починка вещей", QuestDifficulty.MEDIUM, 30, 20)
    )

    // Недельные боссы (2 штуки) - награды в пределах лимита (макс 100 XP, 50 G)
    private val weeklyBosses = listOf(
        QuestTemplate("Большой проект", "Завершите большой проект на этой неделе", QuestDifficulty.EPIC, 100, 50),
        QuestTemplate("Марафон задач", "Выполните 10 важных задач за неделю", QuestDifficulty.HARD, 80, 40)
    )
    
    // Сохранение выполненных недельных квестов в SharedPreferences
    private fun saveCompletedWeeklyQuests() {
        prefs?.edit()?.apply {
            putStringSet(PREF_COMPLETED_WEEKLY, completedWeeklyQuestIds.toSet())
            putString(PREF_LAST_RESET_WEEK, lastResetDate.toString())
            apply()
        }
    }
    
    // Загрузка выполненных недельных квестов из SharedPreferences
    private fun loadCompletedWeeklyQuests() {
        prefs?.let {
            val savedWeek = it.getString(PREF_LAST_RESET_WEEK, null)
            val currentWeek = java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfYear()
            val currentYearWeek = "${LocalDate.now().year}-W${LocalDate.now().get(currentWeek)}"
            
            // Если новая неделя - сбрасываем
            if (savedWeek != currentYearWeek) {
                completedWeeklyQuestIds.clear()
                lastResetDate = LocalDate.now()
            } else {
                // Загружаем сохраненные данные
                completedWeeklyQuestIds = it.getStringSet(PREF_COMPLETED_WEEKLY, emptySet())?.toMutableSet() ?: mutableSetOf()
                lastResetDate = LocalDate.parse(savedWeek)
            }
        }
    }
    
    // Сохранение истории квестов за день
    fun saveCompletedQuestHistory(entry: CompletedQuestEntry) {
        completedQuestsHistory.add(entry)
        prefs?.let {
            val existingJson = it.getString(PREF_COMPLETED_QUESTS_HISTORY, "[]") ?: "[]"
            // В реальном приложении здесь нужна сериализация в JSON
            // Для простоты сохраняем только количество
            it.edit().putInt("${PREF_COMPLETED_QUESTS_HISTORY}_count", completedQuestsHistory.size).apply()
        }
    }
    
    // Получение истории квестов за сегодня
    fun getCompletedQuestHistory(): List<CompletedQuestEntry> {
        return completedQuestsHistory.toList()
    }
    
    // Очистка истории при новом дне
    fun checkAndResetDailyHistory() {
        prefs?.let {
            val lastDate = it.getString("last_history_date", null)
            val today = LocalDate.now().toString()
            if (lastDate != today) {
                completedQuestsHistory.clear()
                it.edit().putString("last_history_date", today).apply()
            }
        }
    }

    // Генерация ежедневных квестов с учётом профессии и класса
    fun generateDailyQuests(
        count: Int = 5,
        profession: Profession = Profession.STUDENT,
        characterClass: CharacterClass = CharacterClass.WARRIOR
    ): List<HealthQuest> {
        val quests = mutableListOf<QuestTemplate>()
        
        // Добавляем квесты по профессии (1-2)
        val professionQuestList = professionQuestsMap[profession] ?: emptyList()
        quests.addAll(professionQuestList.shuffled().take(2))
        
        // Добавляем квесты по классу (1)
        val classQuestList = classQuestsMap[characterClass] ?: emptyList()
        quests.addAll(classQuestList.shuffled().take(1))
        
        // Добавляем общие квесты (здоровье, развитие, быт)
        quests.addAll(healthQuests.shuffled().take(1))
        quests.addAll(personalDevelopmentQuests.shuffled().take(1))
        quests.addAll(householdQuests.shuffled().take(1))
        
        // Перемешиваем и берем нужное количество
        return quests.shuffled().take(count).map { template ->
            Quest(
                title = template.title,
                description = template.description,
                type = QuestType.DAILY,
                difficulty = template.difficulty,
                xpReward = template.xpReward,
                goldReward = template.goldReward
            )
        }
    }

    // Недельные квесты - возвращаем только те, которые еще не выполнены на этой неделе
    fun generateWeeklyQuests(
        profession: Profession = Profession.STUDENT,
        characterClass: CharacterClass = CharacterClass.WARRIOR
    ): List<Quest> {
        // Проверяем, наступила ли новая неделя и сбрасываем если нужно
        val currentWeek = java.time.temporal.WeekFields.of(java.util.Locale.getDefault()).weekOfYear()
        val currentYearWeek = "${LocalDate.now().year}-W${LocalDate.now().get(currentWeek)}"
        
        if (lastResetDate.toString() != currentYearWeek) {
            completedWeeklyQuestIds.clear()
            lastResetDate = LocalDate.now()
        }
        
        // Возвращаем только те квесты, которые еще не были выполнены
        return weeklyBosses
            .filter { !completedWeeklyQuestIds.contains(it.title) }
            .map { template ->
                Quest(
                    title = template.title,
                    description = template.description,
                    type = QuestType.WEEKLY,
                    difficulty = template.difficulty,
                    xpReward = template.xpReward,
                    goldReward = template.goldReward
                )
            }
    }
    
    // Метод для отметки недельного квеста как выполненного
    fun markWeeklyQuestCompleted(questTitle: String) {
        completedWeeklyQuestIds.add(questTitle)
        saveCompletedWeeklyQuests()
    }

    // Получить все квесты для профессии
    fun getQuestsForProfession(profession: Profession): List<QuestTemplate> {
        return professionQuestsMap[profession] ?: emptyList()
    }

    // Получить все квесты для класса
    fun getQuestsForClass(characterClass: CharacterClass): List<QuestTemplate> {
        return classQuestsMap[characterClass] ?: emptyList()
    }

    // Добавить новый квест (пока просто заглушка)
    fun addQuest(quest: HealthQuest) {
        println("Квест добавлен: ${quest.title} (${quest.type})")
        // Здесь в будущем можно сохранить в Room или DataStore
    }

    // Статистика (заглушка)
    fun getTotalQuestsCount(): Int = 
        professionQuestsMap.values.sumOf { it.size } +
        classQuestsMap.values.sumOf { it.size } +
        healthQuests.size +
        personalDevelopmentQuests.size +
        householdQuests.size +
        weeklyBosses.size

    // Формула опыта для уровня (RPG прогрессия - замедленная)
    fun getXpRequiredForLevel(level: Int): Int {
        if (level <= 1) return 200
        return (level * level * 100) + (level * 50)
    }
}

// Шаблон квеста для генерации
data class QuestTemplate(
    val title: String,
    val description: String,
    val difficulty: QuestDifficulty,
    val xpReward: Int,
    val goldReward: Int,
    val requiredProfession: Profession? = null,
    val requiredClass: CharacterClass? = null
)

// Запись в истории выполненных квестов
data class CompletedQuestEntry(
    val questTitle: String,
    val xpReward: Int,
    val goldReward: Int,
    val completedAt: java.time.LocalDateTime,
    val questType: QuestType
)

// Достижения и ачивки
data class AchievementData(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val isUnlocked: Boolean = false,
    val unlockedAt: java.time.LocalDateTime? = null,
    val requirement: Int = 0, // требуемое значение (например, количество квестов)
    val type: com.questlife.app.models.AchievementType
)

// Система инвентаря
data class Inventory(
    val items: List<InventoryItem> = emptyList()
) {
    fun addItem(item: InventoryItem): Inventory {
        return copy(items = items + item)
    }
    
    fun removeItem(itemId: String): Inventory {
        return copy(items = items.filter { it.id != itemId })
    }
    
    fun hasItem(itemId: String): Boolean = items.any { it.id == itemId }
    
    fun getTotalGold(): Int = items.filter { it.item.type == ItemType.CONSUMABLE }.sumOf { it.quantity } * 0 + 
                              items.filter { it.item.type != ItemType.CONSUMABLE }.sumOf { it.item.price }
}

data class InventoryItem(
    val item: Item,
    val quantity: Int = 1,
    val acquiredAt: java.time.LocalDateTime = java.time.LocalDateTime.now()
)
