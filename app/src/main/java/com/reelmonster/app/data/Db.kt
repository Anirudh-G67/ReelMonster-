package com.reelmonster.app.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

object Source {
    const val REEL = "INSTAGRAM_REEL"
    const val SHORT = "YOUTUBE_SHORT"
    fun label(s: String) = if (s == REEL) "Reel" else "Short"
}

object Mode {
    const val MANUAL = "MANUAL"
    const val AUTO = "AUTO"
}

@Entity(tableName = "view_events", indices = [Index("timestamp")])
data class ViewEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val source: String,
    val mode: String
)

@Dao
interface EventDao {
    @Insert
    suspend fun insert(e: ViewEvent): Long

    @Query("SELECT * FROM view_events WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp")
    fun observeBetween(from: Long, to: Long): Flow<List<ViewEvent>>

    @Query("SELECT * FROM view_events WHERE timestamp >= :from ORDER BY timestamp")
    fun observeSince(from: Long): Flow<List<ViewEvent>>

    @Query("SELECT * FROM view_events ORDER BY timestamp")
    suspend fun all(): List<ViewEvent>

    @Query(
        "DELETE FROM view_events WHERE id = (SELECT id FROM view_events " +
            "WHERE timestamp >= :from AND timestamp < :to ORDER BY timestamp DESC, id DESC LIMIT 1)"
    )
    suspend fun deleteLast(from: Long, to: Long): Int

    @Query("DELETE FROM view_events")
    suspend fun clear()
}

@Database(entities = [ViewEvent::class], version = 1, exportSchema = false)
abstract class AppDb : RoomDatabase() {
    abstract fun events(): EventDao

    companion object {
        @Volatile private var inst: AppDb? = null
        fun get(ctx: Context): AppDb = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "reelmonster.db")
                .build().also { inst = it }
        }
    }
}

object Repo {
    suspend fun record(ctx: Context, source: String, mode: String) {
        AppDb.get(ctx).events().insert(ViewEvent(timestamp = System.currentTimeMillis(), source = source, mode = mode))
    }

    /** Removes the most recent record of the current local day. */
    suspend fun undo(ctx: Context): Boolean {
        val d = Day.now()
        return AppDb.get(ctx).events().deleteLast(d.start, d.end) > 0
    }
}
