package com.bepresent.android.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScheduledSessionDao {

    @Query("SELECT * FROM scheduled_sessions")
    fun getAll(): Flow<List<ScheduledSession>>

    @Query("SELECT * FROM scheduled_sessions")
    suspend fun getAllOnce(): List<ScheduledSession>

    @Query("SELECT * FROM scheduled_sessions WHERE enabled = 1")
    suspend fun getEnabled(): List<ScheduledSession>

    @Query("SELECT * FROM scheduled_sessions WHERE id = :id")
    suspend fun getById(id: String): ScheduledSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(session: ScheduledSession)

    @Delete
    suspend fun delete(session: ScheduledSession)
}
