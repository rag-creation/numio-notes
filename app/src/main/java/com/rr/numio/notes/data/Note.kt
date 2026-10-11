package com.rr.numio.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    // Rich text is stored as HTML (Compose Rich Editor can read and write it)
    val contentHtml: String = "",
    // Plain text copy for the card preview and for search later
    val preview: String = "",
    val notebook: String = "Personal",
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
)
