package com.questlife.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import java.util.UUID

// ============= ENTITY КЛАССЫ ДЛЯ ROOM =============

@Entity(tableName = "workout_programs")
data class WorkoutProgramEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val description: String = "",
    val goal: String = "",
    val durationWeeks: Int = 4,
    val daysPerWeek: Int = 3,
    val workoutsJson: String = "", // JSON сериализация списка Workout
    val targetCompletions: Int = 10,
    val completedCount: Int = 0,
    val isCustom: Boolean = false, // true если программа была изменена пользователем
    val originalId: String? = null // ID оригинальной программы если это копия
)

@Entity(tableName = "completed_workouts")
data class CompletedWorkoutEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val programId: String,
    val programName: String,
    val workoutName: String,
    val completedAt: String, // ISO строка LocalDateTime
    val durationMinutes: Int = 0,
    val exercisesJson: String = "", // JSON сериализация списка CompletedExercise
    val totalVolume: Double = 0.0,
    val weight: Double? = null, // Вес тела после тренировки
    val calories: Int? = null // Калории за тренировку
)

@Entity(tableName = "muscle_statistics")
data class MuscleStatisticsEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val muscleGroup: String,
    val workoutDate: String, // ISO строка LocalDate
    val exercisesCount: Int = 0,
    val totalSets: Int = 0,
    val totalReps: Int = 0,
    val totalVolume: Double = 0.0
)

@Entity(tableName = "set_history")
data class SetHistoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val exerciseId: String,
    val exerciseName: String,
    val programId: String,
    val programName: String,
    val workoutName: String,
    val sets: Int,
    val reps: Int,
    val weight: Double,
    val volume: Double,
    val force: Double = 0.0, // Сила (тоннаж) для поиска похожих упражнений
    val completedAt: String // ISO строка LocalDateTime
)

@Dao
interface FitnessDao {
    // Программы тренировок
    @Query("SELECT * FROM workout_programs ORDER BY completedCount DESC")
    fun getAllPrograms(): Flow<List<WorkoutProgramEntity>>
    
    @Query("DELETE FROM workout_programs")
    suspend fun deleteAllPrograms()
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: WorkoutProgramEntity)
    
    @Update
    suspend fun updateProgram(program: WorkoutProgramEntity)
    
    @Delete
    suspend fun deleteProgram(program: WorkoutProgramEntity)
    
    @Query("SELECT * FROM workout_programs WHERE id = :programId")
    suspend fun getProgramById(programId: String): WorkoutProgramEntity?
    
    // Завершенные тренировки
    @Query("SELECT * FROM completed_workouts ORDER BY completedAt DESC")
    fun getAllCompletedWorkouts(): Flow<List<CompletedWorkoutEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCompletedWorkout(workout: CompletedWorkoutEntity)
    
    @Query("SELECT * FROM completed_workouts WHERE programId = :programId ORDER BY completedAt DESC")
    fun getWorkoutsByProgramId(programId: String): Flow<List<CompletedWorkoutEntity>>
    
    @Query("SELECT * FROM completed_workouts WHERE completedAt >= :startDate AND completedAt <= :endDate ORDER BY completedAt DESC")
    fun getWorkoutsByDateRange(startDate: String, endDate: String): Flow<List<CompletedWorkoutEntity>>
    
    // Статистика по мышцам
    @Query("SELECT * FROM muscle_statistics ORDER BY workoutDate DESC")
    fun getAllMuscleStatistics(): Flow<List<MuscleStatisticsEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMuscleStatistics(stats: MuscleStatisticsEntity)
    
    @Query("SELECT * FROM muscle_statistics WHERE muscleGroup = :muscleGroup ORDER BY workoutDate DESC LIMIT 10")
    fun getMuscleStatsByGroup(muscleGroup: String): Flow<List<MuscleStatisticsEntity>>
    
    // Агрегированная статистика по мышцам за период
    @Query("""
        SELECT muscleGroup, 
               SUM(exercisesCount) as totalExercises,
               SUM(totalSets) as totalSets,
               SUM(totalReps) as totalReps,
               SUM(totalVolume) as totalVolume
        FROM muscle_statistics 
        WHERE workoutDate >= :startDate AND workoutDate <= :endDate
        GROUP BY muscleGroup
        ORDER BY totalVolume DESC
    """)
    fun getMuscleStatsByDateRange(startDate: String, endDate: String): Flow<List<MuscleAggregatedStats>>
    
    // История подходов
    @Query("SELECT * FROM set_history ORDER BY completedAt DESC")
    fun getAllSetHistory(): Flow<List<SetHistoryEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetHistory(history: SetHistoryEntity)
    
    @Query("SELECT * FROM set_history WHERE exerciseId = :exerciseId ORDER BY completedAt DESC")
    fun getSetHistoryByExercise(exerciseId: String): Flow<List<SetHistoryEntity>>
    
    @Query("SELECT * FROM set_history WHERE exerciseId = :exerciseId AND programId = :programId ORDER BY completedAt DESC")
    fun getSetHistoryByExerciseAndProgram(exerciseId: String, programId: String): Flow<List<SetHistoryEntity>>
}

data class MuscleAggregatedStats(
    val muscleGroup: String,
    val totalExercises: Int,
    val totalSets: Int,
    val totalReps: Int,
    val totalVolume: Double
)

@Database(
    entities = [
        WorkoutProgramEntity::class,
        CompletedWorkoutEntity::class,
        MuscleStatisticsEntity::class,
        SetHistoryEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class FitnessDatabase : RoomDatabase() {
    abstract fun fitnessDao(): FitnessDao
    
    companion object {
        @Volatile private var INSTANCE: FitnessDatabase? = null
        
        fun getDatabase(context: Context): FitnessDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FitnessDatabase::class.java,
                    "fitness_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
