package com.mohithash.repcoach.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val dayName: String,
    val startedAt: Long = System.currentTimeMillis(),
    val endedAt: Long = 0,
    val notes: String = "",
    /** domain.Debrief JSON once the coach has reviewed it. */
    val debrief: String = "",
)

@Entity(tableName = "sets")
data class SetLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exercise: String,
    val setIndex: Int,
    val weight: Double,
    val reps: Int,
    val timestamp: Long = System.currentTimeMillis(),
)

data class ExerciseBest(val exercise: String, val weight: Double, val reps: Int)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startedAt DESC") fun all(): Flow<List<Session>>
    @Query("SELECT * FROM sessions WHERE endedAt > 0 ORDER BY startedAt DESC LIMIT :n") suspend fun recent(n: Int): List<Session>
    @Insert suspend fun insert(s: Session): Long
    @Update suspend fun update(s: Session)
    @Query("DELETE FROM sessions WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface SetDao {
    @Query("SELECT * FROM sets WHERE sessionId = :sid ORDER BY exercise, setIndex") fun forSession(sid: Long): Flow<List<SetLog>>
    @Query("SELECT * FROM sets WHERE sessionId = :sid ORDER BY exercise, setIndex") suspend fun listForSession(sid: Long): List<SetLog>
    @Query("SELECT * FROM sets WHERE exercise = :ex AND sessionId != :exceptSession ORDER BY timestamp DESC LIMIT 6") suspend fun lastFor(ex: String, exceptSession: Long): List<SetLog>
    @Query("SELECT exercise, MAX(weight) AS weight, reps FROM sets GROUP BY exercise ORDER BY exercise") fun bests(): Flow<List<ExerciseBest>>
    @Query("SELECT * FROM sets ORDER BY timestamp DESC") fun all(): Flow<List<SetLog>>
    @Insert suspend fun insert(s: SetLog)
    @Query("DELETE FROM sets WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM sets WHERE sessionId = :sid") suspend fun deleteSession(sid: Long)
}

@Database(entities = [Session::class, SetLog::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() { abstract fun sessions(): SessionDao; abstract fun sets(): SetDao }
