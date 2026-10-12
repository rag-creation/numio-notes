package com.rr.numio.notes.ui

import android.app.Application
import android.text.Html
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.numio.notes.data.Note
import com.rr.numio.notes.data.NotesDatabase
import com.rr.numio.notes.data.TodoCodec
import com.rr.numio.notes.data.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = NotesDatabase.get(app).noteDao()

    val notes: StateFlow<List<Note>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    suspend fun load(id: Long): Note? = if (id == 0L) null else dao.getById(id)

    // Called when leaving the editor. Empty new notes are not saved.
    fun save(id: Long, title: String, html: String, pinned: Boolean, notebook: String) {
        val plain = htmlToPlain(html)
        if (id == 0L && title.isBlank() && plain.isBlank()) return
        viewModelScope.launch {
            dao.upsert(
                Note(
                    id = id,
                    title = title.trim(),
                    contentHtml = html,
                    preview = plain.take(200),
                    notebook = notebook,
                    pinned = pinned,
                    updatedAt = System.currentTimeMillis(),
                )
            )
        }
    }

    // Called when leaving the to-do screen. Empty new lists are not saved.
    fun saveTodo(id: Long, title: String, items: List<TodoItem>, pinned: Boolean, notebook: String) {
        val kept = items.filter { it.text.isNotBlank() }
        if (id == 0L && title.isBlank() && kept.isEmpty()) return
        viewModelScope.launch {
            dao.upsert(
                Note(
                    id = id,
                    title = title.trim(),
                    contentHtml = TodoCodec.encode(kept),
                    preview = kept.joinToString(", ") { it.text }.take(200),
                    notebook = notebook,
                    pinned = pinned,
                    updatedAt = System.currentTimeMillis(),
                    isTodo = true,
                )
            )
        }
    }

    // Ticking an item straight from the list card
    fun toggleTodoItem(note: Note, itemId: Long) {
        val items = TodoCodec.decode(note.contentHtml).map {
            if (it.id == itemId) it.copy(done = !it.done) else it
        }
        viewModelScope.launch {
            dao.upsert(note.copy(contentHtml = TodoCodec.encode(items), updatedAt = System.currentTimeMillis()))
        }
    }

    fun setPinned(note: Note, pinned: Boolean) {
        viewModelScope.launch { dao.upsert(note.copy(pinned = pinned)) }
    }

    // Undo a delete: put the same note back (same id, same date)
    fun restore(note: Note) {
        viewModelScope.launch { dao.upsert(note) }
    }

    fun delete(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }

    private fun htmlToPlain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}
