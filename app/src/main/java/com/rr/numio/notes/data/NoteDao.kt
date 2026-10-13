package com.rr.numio.notes.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    // Notes not in Trash (sorting happens in the ViewModel)
    @Query("SELECT * FROM notes WHERE deletedAt = 0")
    fun observeAll(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt > 0 ORDER BY deletedAt DESC")
    fun observeTrash(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE deletedAt = 0")
    suspend fun getAll(): List<Note>

    // Trash keeps notes for 30 days
    @Query("DELETE FROM notes WHERE deletedAt > 0 AND deletedAt < :before")
    suspend fun purgeTrash(before: Long)

    @Query("DELETE FROM notes WHERE deletedAt > 0")
    suspend fun emptyTrash()

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Upsert
    suspend fun upsert(note: Note): Long

    @Delete
    suspend fun delete(note: Note)

    @Insert
    suspend fun insertAll(notes: List<Note>)

    @Upsert
    suspend fun upsertAll(notes: List<Note>)

    @Delete
    suspend fun deleteAll(notes: List<Note>)

    @Query("UPDATE notes SET notebook = :to WHERE notebook = :from")
    suspend fun moveNotebook(from: String, to: String)
}
