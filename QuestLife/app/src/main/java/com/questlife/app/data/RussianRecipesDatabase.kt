package com.questlife.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.questlife.app.models.Recipe
import com.questlife.app.models.RecipeIngredient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * База данных русских рецептов из проекта russian-recipes-parser
 * https://github.com/chipslays/russian-recipes-parser
 */
class RussianRecipesDatabase(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "russian_recipes.db"
        private const val DATABASE_VERSION = 1
        private const val TAG = "RussianRecipesDB"

        private var instance: RussianRecipesDatabase? = null

        fun getInstance(context: Context): RussianRecipesDatabase {
            return instance ?: synchronized(this) {
                instance ?: RussianRecipesDatabase(context.applicationContext).also {
                    instance = it
                }
            }
        }

        /**
         * Копирование базы данных из assets в внутреннее хранилище при первом запуске
         */
        suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
            try {
                val dbFile = context.getDatabasePath(DATABASE_NAME)
                
                if (!dbFile.exists()) {
                    Log.d(TAG, "База данных не найдена, копируем из assets...")
                    dbFile.parentFile?.mkdirs()
                    
                    context.assets.open("russian_recipes.db").use { input ->
                        FileOutputStream(dbFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                    Log.d(TAG, "База данных успешно скопирована")
                } else {
                    Log.d(TAG, "База данных уже существует: ${dbFile.absolutePath}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при инициализации базы данных: ${e.message}", e)
            }
        }
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // База уже существует в assets, создание не требуется
        Log.d(TAG, "onCreate вызван, но база уже должна существовать")
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        Log.d(TAG, "Обновление базы данных с версии $oldVersion до $newVersion")
        // При необходимости можно реализовать миграцию
    }

    /**
     * Получить все ссылки на рецепты (URL)
     */
    fun getAllRecipeLinks(): List<RecipeLink> {
        val links = mutableListOf<RecipeLink>()
        val db = readableDatabase
        
        val query = "SELECT id, link, lastmod FROM links ORDER BY id"
        val cursor = db.rawQuery(query, null)
        
        try {
            if (cursor.moveToFirst()) {
                do {
                    val link = RecipeLink(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("link")),
                        lastModified = cursor.getLong(cursor.getColumnIndexOrThrow("lastmod"))
                    )
                    links.add(link)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        
        Log.d(TAG, "Загружено ${links.size} ссылок на рецепты")
        return links
    }

    /**
     * Поиск рецептов по названию (ограниченный поиск по URL)
     */
    fun searchRecipesByQuery(query: String): List<RecipeLink> {
        if (query.isBlank()) return emptyList()
        
        val links = mutableListOf<RecipeLink>()
        val db = readableDatabase
        
        // Поиск по подстроке в URL
        val selection = "link LIKE ?"
        val selectionArgs = arrayOf("%$query%")
        
        val cursor = db.rawQuery(
            "SELECT id, link, lastmod FROM links WHERE $selection LIMIT 50",
            selectionArgs
        )
        
        try {
            if (cursor.moveToFirst()) {
                do {
                    val link = RecipeLink(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("link")),
                        lastModified = cursor.getLong(cursor.getColumnIndexOrThrow("lastmod"))
                    )
                    links.add(link)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        
        Log.d(TAG, "Найдено ${links.size} рецептов по запросу '$query'")
        return links
    }

    /**
     * Получить количество рецептов в базе
     */
    fun getRecipeCount(): Int {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT COUNT(*) FROM links", null)
        
        return try {
            if (cursor.moveToFirst()) {
                cursor.getInt(0)
            } else {
                0
            }
        } finally {
            cursor.close()
        }
    }

    /**
     * Получить случайные рецепты
     */
    fun getRandomRecipes(limit: Int = 10): List<RecipeLink> {
        val links = mutableListOf<RecipeLink>()
        val db = readableDatabase
        
        val cursor = db.rawQuery(
            "SELECT id, link, lastmod FROM links ORDER BY RANDOM() LIMIT ?",
            arrayOf(limit.toString())
        )
        
        try {
            if (cursor.moveToFirst()) {
                do {
                    val link = RecipeLink(
                        id = cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                        url = cursor.getString(cursor.getColumnIndexOrThrow("link")),
                        lastModified = cursor.getLong(cursor.getColumnIndexOrThrow("lastmod"))
                    )
                    links.add(link)
                } while (cursor.moveToNext())
            }
        } finally {
            cursor.close()
        }
        
        return links
    }
}

/**
 * Модель ссылки на рецепт
 */
data class RecipeLink(
    val id: Long,
    val url: String,
    val lastModified: Long
)
