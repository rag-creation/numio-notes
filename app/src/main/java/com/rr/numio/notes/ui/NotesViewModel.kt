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
import com.rr.numio.notes.data.Backup
import com.rr.numio.notes.data.SortChoice
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
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

    // --- Sorting ---
    val sort: StateFlow<SortChoice> = settings.sort
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortChoice())

    fun setSort(choice: SortChoice) {
        viewModelScope.launch { settings.setSort(choice) }
    }

    // Pinned always first; then (optionally) unfinished to-do lists; then the chosen order
    val notes: StateFlow<List<Note>> = combine(dao.observeAll(), settings.sort) { list, s ->
        val byChoice: Comparator<Note> = when (s.by) {
            "created" -> compareByDescending { it.createdAt }
            "title" -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.title.ifBlank { it.preview }.trim() }
            else -> compareByDescending { it.updatedAt }
        }
        val ordered = if (s.reverse) byChoice.reversed() else byChoice
        val openTodo: (Note) -> Boolean = { n -> n.isTodo && TodoCodec.decode(n.contentHtml).any { !it.done } }
        list.sortedWith(
            compareByDescending<Note> { it.pinned }
                .thenByDescending { s.todosFirst && openTodo(it) }
                .then(ordered)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // --- Trash ---
    val trash: StateFlow<List<Note>> = dao.observeTrash()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Notes stay in Trash for 30 days
        viewModelScope.launch { dao.purgeTrash(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000) }
    }

    fun restoreFromTrash(list: List<Note>) {
        viewModelScope.launch { dao.upsertAll(list.map { it.copy(deletedAt = 0) }) }
    }

    fun deleteForever(list: List<Note>) {
        viewModelScope.launch { dao.deleteAll(list) }
    }

    fun emptyTrash() {
        viewModelScope.launch { dao.emptyTrash() }
    }

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

    // Saving from an editor: keep the note's created date
    private suspend fun upsertKeeping(note: Note) {
        val old = if (note.id == 0L) null else dao.getById(note.id)
        dao.upsert(note.copy(createdAt = old?.createdAt?.takeIf { it > 0 } ?: note.updatedAt, deletedAt = old?.deletedAt ?: 0))
    }

    // Called when leaving the editor. Empty new notes are not saved.
    fun save(id: Long, title: String, html: String, pinned: Boolean, notebook: String) {
        val plain = htmlToPlain(html)
        if (id == 0L && title.isBlank() && plain.isBlank()) return
        viewModelScope.launch {
            upsertKeeping(
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
            upsertKeeping(
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
            upsertKeeping(
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

    private var backupText: String? = null

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        val app = getApplication<Application>()
        viewModelScope.launch {
            _importState.value = "Importing…"
            val result = runCatching {
                val read = withContext(Dispatchers.IO) {
                    val notes = mutableListOf<ImportedNote>()
                    var encrypted = 0
                    backupText = null
                    for (uri in uris) {
                        val name = fileName(app, uri).lowercase()
                        app.contentResolver.openInputStream(uri)?.use { input ->
                            when {
                                name.endsWith(".md") || name.endsWith(".markdown") ->
                                    notes += JoplinImport.readMarkdownFile(fileName(app, uri), input.readBytes().toString(Charsets.UTF_8))
                                name.endsWith(".txt") ->
                                    notes += JoplinImport.readTextFile(fileName(app, uri), input.readBytes().toString(Charsets.UTF_8))
                                name.endsWith(".numio") || name.endsWith(".json") -> {
                                    val text = input.readBytes().toString(Charsets.UTF_8)
                                    if (Backup.isBackup(text)) backupText = text
                                    else KeepImport.readJson(text)?.let { notes += it }
                                }
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
                backupText?.let { return@runCatching restoreBackup(it) }
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
                            createdAt = n.createdAt ?: n.updatedAt,
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
                                createdAt = n.createdAt ?: n.updatedAt,
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
            _importState.value = result.getOrElse { "Couldn't read that file. It needs to be a .numio backup, a Takeout .zip, or a .txt, .md or .jex file." }
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

    // Deleting moves notes to Trash
    fun deleteMany(list: List<Note>) {
        val now = System.currentTimeMillis()
        viewModelScope.launch { dao.upsertAll(list.map { it.copy(deletedAt = now) }) }
    }

    // Undo: the same notes come back with the same ids and dates
    fun restoreMany(list: List<Note>) {
        viewModelScope.launch { dao.upsertAll(list.map { it.copy(deletedAt = 0) }) }
    }

    fun setPinnedMany(list: List<Note>, pinned: Boolean) {
        viewModelScope.launch { dao.upsertAll(list.map { it.copy(pinned = pinned) }) }
    }

    fun moveMany(list: List<Note>, notebook: String) {
        viewModelScope.launch { dao.upsertAll(list.map { it.copy(notebook = notebook) }) }
    }

    fun delete(note: Note) = deleteMany(listOf(note))

    // --- Backup, restore, export ---
    private val _backupState = MutableStateFlow<String?>(null)
    val backupState: StateFlow<String?> = _backupState
    fun clearBackupState() { _backupState.value = null }

    fun backupTo(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _backupState.value = runCatching {
                val all = dao.getAll()
                val json = Backup.write(all, notebooks.value)
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray()) }
                        ?: error("no stream")
                }
                if (all.size == 1) "Backed up 1 note." else "Backed up ${all.size} notes."
            }.getOrElse { "Couldn't save the backup." }
        }
    }

    // Restore adds notes that aren't already here (same title, date and text), so it's safe to run twice
    private suspend fun restoreBackup(text: String): String {
        val restored = Backup.read(text)
        val existing = dao.getAll().map { Triple(it.title, it.createdAt, it.contentHtml) }.toHashSet()
        val fresh = restored.notes.filter { Triple(it.title, it.createdAt, it.contentHtml) !in existing }
            .map { n ->
                if (n.isTodo || n.isDrawing) n
                else n.copy(preview = htmlToPlain(n.contentHtml).take(200))
            }.map { n ->
                if (n.isTodo) n.copy(preview = TodoCodec.decode(n.contentHtml).joinToString(", ") { it.text }.take(200)) else n
            }
        dao.insertAll(fresh)
        (restored.notebooks + fresh.map { it.notebook }).distinct()
            .forEach { if (it != DEFAULT_NOTEBOOK) settings.addNotebook(it) }
        val skipped = restored.notes.size - fresh.size
        return buildString {
            append(if (fresh.size == 1) "Restored 1 note" else "Restored ${fresh.size} notes")
            if (skipped > 0) append(" ($skipped already here)")
            append(".")
        }
    }

    // All notes as Markdown in a .zip: one folder per notebook, drawings as pictures
    fun exportMarkdownTo(uri: Uri) {
        val app = getApplication<Application>()
        viewModelScope.launch {
            _backupState.value = "Exporting…"
            _backupState.value = runCatching {
                val all = dao.getAll()
                // Markdown is made with the editor itself, on the main thread
                val texts = all.associate { n ->
                    n.id to when {
                        n.isDrawing -> ""
                        n.isTodo -> TodoCodec.decode(n.contentHtml).joinToString("\n") { (if (it.done) "- [x] " else "- [ ] ") + it.text }
                        else -> RichTextState().setHtml(n.contentHtml).toMarkdown()
                    }
                }
                withContext(Dispatchers.IO) {
                    app.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                        ZipOutputStream(out).use { zip ->
                            val used = HashSet<String>()
                            fun uniquePath(folder: String, name: String, ext: String): String {
                                var path = "$folder/$name.$ext"
                                var i = 2
                                while (!used.add(path.lowercase())) { path = "$folder/$name ($i).$ext"; i++ }
                                return path
                            }
                            all.forEach { n ->
                                val folder = safeName(n.notebook, "Notes")
                                val name = safeName(n.title.ifBlank { n.preview.take(40) }, if (n.isDrawing) "Drawing" else "Note")
                                if (n.isDrawing) {
                                    val d = DrawingCodec.decode(n.contentHtml)
                                    if (d.strokes.isNotEmpty()) {
                                        zip.putNextEntry(ZipEntry(uniquePath(folder, name, "png")))
                                        Share.render(d).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, zip)
                                        zip.closeEntry()
                                    }
                                } else {
                                    zip.putNextEntry(ZipEntry(uniquePath(folder, name, "md")))
                                    val head = if (n.title.isNotBlank()) "# ${n.title}\n\n" else ""
                                    zip.write((head + texts[n.id].orEmpty()).toByteArray())
                                    zip.closeEntry()
                                }
                            }
                        }
                    } ?: error("no stream")
                }
                if (all.size == 1) "Exported 1 note." else "Exported ${all.size} notes."
            }.getOrElse { "Couldn't export the notes." }
        }
    }

    private fun safeName(s: String, fallback: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|\\n\\r\\t]"), " ").trim().take(60).ifBlank { fallback }

    private fun htmlToPlain(html: String): String =
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
}

const val DEFAULT_NOTEBOOK = "Personal"
