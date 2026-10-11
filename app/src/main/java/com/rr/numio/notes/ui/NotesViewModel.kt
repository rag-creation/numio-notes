package com.rr.numio.notes.ui

import android.app.Application
import android.text.Html
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.numio.notes.data.Note
import com.rr.numio.notes.data.NotesDatabase
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

    fun delete(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }

    private fun htmlToPlain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}
