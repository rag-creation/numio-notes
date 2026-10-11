package com.rr.numio.notes.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String = "",
    // Rich text is stored as HTML (Compose Rich Editor can read and write it).
    // For to-do notes this holds the items instead (see TodoCodec).
    val contentHtml: String = "",
    // Plain text copy for the card preview and for search later
    val preview: String = "",
    val notebook: String = "Personal",
    val pinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(defaultValue = "0") val isTodo: Boolean = false,
)

data class TodoItem(val id: Long, val text: String, val done: Boolean)

// To-do items are stored one per line: "1<TAB>text" (done) or "0<TAB>text"
object TodoCodec {
    fun encode(items: List<TodoItem>): String =
        items.filter { it.text.isNotBlank() }
            .joinToString("\n") { (if (it.done) "1" else "0") + "\t" + it.text.replace("\n", " ").replace("\t", " ") }

    fun decode(raw: String): List<TodoItem> =
        raw.lines().filter { it.isNotBlank() }.mapIndexed { i, line ->
            val done = line.startsWith("1\t")
            TodoItem(id = i.toLong() + 1, text = line.substringAfter("\t"), done = done)
        }
}
