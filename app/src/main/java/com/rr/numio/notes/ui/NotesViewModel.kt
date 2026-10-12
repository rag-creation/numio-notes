package com.rr.numio.notes.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Html
import com.mohamedrejeb.richeditor.model.RichTextState
import com.rr.numio.notes.data.ImportedNote
import com.rr.numio.notes.data.JoplinImport
import com.rr.numio.notes.data.KeepImport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.rr.numio.notes.data.AppSettings
import com.rr.numio.notes.data.Drawing
import com.rr.numio.notes.data.DrawingCodec
import com.rr.numio.notes.data.Note
import com.rr.numio.notes.data.NotesDatabase
import com.rr.numio.notes.data.TodoCodec
import com.rr.numio.notes.data.TodoItem
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotesViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = NotesDatabase.get(app).noteDao()

    private val settings = AppSettings(app)

    val notes: StateFlow<List<Note>> = dao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // Personal first, then the rest A–Z: the ones you made plus any a note is in
    val notebooks: StateFlow<List<String>> = combine(notes, settings.notebooks) { list, saved ->
        listOf(DEFAULT_NOTEBOOK) + (saved + list.map { it.notebook })
            .filter { it.isNotBlank() && it != DEFAULT_NOTEBOOK }
            .distinct()
            .sortedBy { it.lowercase() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), listOf(DEFAULT_NOTEBOOK))

    // Notebook a new note starts in (the one you're looking at on the list)
    var newNoteNotebook: String = DEFAULT_NOTEBOOK

    fun addNotebook(name: String) {
        val clean = name.trim()
        if (clean.isEmpty() || clean == DEFAULT_NOTEBOOK) return
        viewModelScope.launch { settings.addNotebook(clean) }
    }

    fun moveNote(note: Note, notebook: String) {
        viewModelScope.launch { dao.upsert(note.copy(notebook = notebook)) }
    }

    fun renameNotebook(from: String, to: String) {
        val clean = to.trim()
        if (from == DEFAULT_NOTEBOOK || clean.isEmpty() || clean == from) return
        viewModelScope.launch {
            dao.moveNotebook(from, clean)
            settings.removeNotebook(from)
            if (clean != DEFAULT_NOTEBOOK) settings.addNotebook(clean)
        }
    }

    // Deleting a notebook keeps its notes: they go back to Personal
    fun deleteNotebook(name: String) {
        if (name == DEFAULT_NOTEBOOK) return
        viewModelScope.launch {
            dao.moveNotebook(name, DEFAULT_NOTEBOOK)
            settings.removeNotebook(name)
        }
    }

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

    // Called when leaving the drawing screen. An empty new drawing is not saved.
    fun saveDrawing(id: Long, title: String, drawing: Drawing, pinned: Boolean, notebook: String) {
        if (id == 0L && title.isBlank() && drawing.strokes.isEmpty()) return
        viewModelScope.launch {
            dao.upsert(
                Note(
                    id = id,
                    title = title.trim(),
                    contentHtml = DrawingCodec.encode(drawing),
                    preview = "",
                    notebook = notebook,
                    pinned = pinned,
                    updatedAt = System.currentTimeMillis(),
                    isDrawing = true,
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

    // --- Import: Joplin (.jex), Google Keep (Takeout), Samsung Notes (.txt), Markdown ---
    private val _importState = MutableStateFlow<String?>(null)
    val importState: StateFlow<String?> = _importState
    fun clearImportState() { _importState.value = null }

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            _importState.value = "Importing…"
            val result = runCatching {
                val read = withContext(Dispatchers.IO) {
                    val notes = mutableListOf<ImportedNote>()
                    var encrypted = 0
                    for (uri in uris) {
                        val name = fileName(app, uri).lowercase()
                        app.contentResolver.openInputStream(uri)?.use { input ->
                            when {
                                name.endsWith(".md") || name.endsWith(".markdown") ->
                                    notes += JoplinImport.readMarkdownFile(fileName(app, uri), input.readBytes().toString(Charsets.UTF_8))
                                name.endsWith(".txt") ->
                                    notes += JoplinImport.readTextFile(fileName(app, uri), input.readBytes().toString(Charsets.UTF_8))
                                name.endsWith(".json") ->
                                    KeepImport.readJson(input.readBytes().toString(Charsets.UTF_8))?.let { notes += it }
                                name.endsWith(".zip") ->
                                    notes += KeepImport.readZip(input.buffered())
                                else -> {
                                    val batch = JoplinImport.readJex(input.buffered())
                                    notes += batch.notes
                                    encrypted += batch.skippedEncrypted
                                }
                            }
                        }
                    }
                    notes to encrypted
                }
                val (imported, encrypted) = read
                // Notes with no title and no text are left out
                val rows = imported.filter { !it.isEmpty }.map { n ->
                    when {
                        n.checklist != null -> Note(
                            title = n.title,
                            contentHtml = TodoCodec.encode(n.checklist),
                            preview = n.checklist.joinToString(", ") { it.text }.take(200),
                            notebook = n.notebook,
                            pinned = n.pinned,
                            updatedAt = n.updatedAt,
                            isTodo = true,
                        )
                        else -> {
                            val html = when {
                                n.plainText != null -> plainToHtml(n.plainText)
                                n.markdown.isNotBlank() -> RichTextState().setMarkdown(n.markdown).toHtml()
                                else -> ""
                            }
                            Note(
                                title = n.title,
                                contentHtml = html,
                                preview = htmlToPlain(html).take(200),
                                notebook = n.notebook,
                                pinned = n.pinned,
                                updatedAt = n.updatedAt,
                            )
                        }
                    }
                }
                dao.insertAll(rows)
                rows.map { it.notebook }.distinct().forEach { if (it != DEFAULT_NOTEBOOK) settings.addNotebook(it) }
                val books = rows.map { it.notebook }.distinct().size
                buildString {
                    append(if (rows.size == 1) "Imported 1 note" else "Imported ${rows.size} notes")
                    if (books > 0) append(if (books == 1) " into 1 notebook" else " into $books notebooks")
                    if (encrypted > 0) append(". Skipped $encrypted encrypted notes (turn off encryption before exporting)")
                    append(".")
                }
            }
            _importState.value = result.getOrElse { "Couldn't read that file. It needs to be a Takeout .zip, .txt, .md or .jex file." }
        }
    }

    // Plain text -> simple HTML paragraphs (keeps line breaks, no Markdown guessing)
    private fun plainToHtml(text: String): String =
        text.trim().split("\n").joinToString("") { line ->
            if (line.isBlank()) "<p><br></p>" else "<p>" + android.text.TextUtils.htmlEncode(line) + "</p>"
        }

    private fun fileName(app: Application, uri: Uri): String =
        app.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        } ?: uri.lastPathSegment.orEmpty()

    fun deleteMany(list: List<Note>) {
        viewModelScope.launch { dao.deleteAll(list) }
    }

    // Undo: the same notes come back with the same ids and dates
    fun restoreMany(list: List<Note>) {
        viewModelScope.launch { dao.upsertAll(list) }
    }

    fun setPinnedMany(list: List<Note>, pinned: Boolean) {
        viewModelScope.launch { dao.upsertAll(list.map { it.copy(pinned = pinned) }) }
    }

    fun moveMany(list: List<Note>, notebook: String) {
        viewModelScope.launch { dao.upsertAll(list.map { it.copy(notebook = notebook) }) }
    }

    fun delete(note: Note) {
        viewModelScope.launch { dao.delete(note) }
    }

    private fun htmlToPlain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}

const val DEFAULT_NOTEBOOK = "Personal"
