package com.rr.numio.notes.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [Note::class], version = 4, exportSchema = false)
abstract class NotesDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile
        private var instance: NotesDatabase? = null

        // v2 adds to-do notes. Existing notes are kept.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isTodo INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v3 adds drawing notes
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN isDrawing INTEGER NOT NULL DEFAULT 0")
            }
        }

        // v4 adds the created date (old notes use their last edit) and Trash
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE notes SET createdAt = updatedAt")
                db.execSQL("ALTER TABLE notes ADD COLUMN deletedAt INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun get(context: Context): NotesDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NotesDatabase::class.java,
                    "notes.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4).build().also { instance = it }
            }
    }
}
