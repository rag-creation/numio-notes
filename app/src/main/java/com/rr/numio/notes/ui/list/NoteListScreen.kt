package com.rr.numio.notes.ui.list

import android.text.Html
import com.rr.numio.notes.data.SortChoice
import com.rr.numio.notes.ui.drawing.DrawingThumbnail
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import com.rr.numio.notes.ui.DEFAULT_NOTEBOOK
import com.rr.numio.notes.ui.notebooks.NameDialog
import com.rr.numio.notes.ui.notebooks.NotebookActionsSheet
import com.rr.numio.notes.ui.notebooks.NotebookPickerSheet
import android.widget.Toast
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.rr.numio.notes.ui.theme.NumioDivider
import com.rr.numio.notes.ui.theme.LocalNotesPalette
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import com.rr.numio.notes.data.TodoCodec
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.notes.R
import com.rr.numio.notes.data.Note
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.theme.NumioAccentInk
import com.rr.numio.notes.ui.theme.NumioTextBody
import com.rr.numio.notes.ui.theme.NumioCheckBorder
import com.rr.numio.notes.ui.theme.NumioAccentSoft
import com.rr.numio.notes.ui.theme.NumioTrack
import com.rr.numio.notes.ui.theme.NumioRaised
import com.rr.numio.notes.ui.theme.NumioOnAccent
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioButton
import com.rr.numio.notes.ui.theme.NumioCard
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioTextSoft
import com.rr.numio.notes.ui.theme.NumioYellow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun NoteListScreen(
    viewModel: NotesViewModel,
    onOpenNote: (Note) -> Unit,
    onNewNote: () -> Unit,
    onNewTodo: () -> Unit,
    onNewDrawing: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    // 2 columns (staggered) or 1 column list, switched by the layout button
    var columns by remember { mutableIntStateOf(2) }
    var menuOpen by remember { mutableStateOf(false) }
    // Notebooks: null = All
    val notebooks by viewModel.notebooks.collectAsState()
    var selectedNotebook by remember { mutableStateOf<String?>(null) }
    if (selectedNotebook != null && selectedNotebook !in notebooks) selectedNotebook = null
    var chipActions by remember { mutableStateOf<String?>(null) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var creatingNotebook by remember { mutableStateOf(false) }
    BackHandler(enabled = menuOpen) { menuOpen = false }

    // Search: null = closed, otherwise the text typed so far
    var query by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = query != null && !menuOpen) { query = null }
    // Plain text of every note, worked out once per change, so search looks at the whole note
    val searchText = remember(notes) {
        notes.associate { n ->
            val body = if (n.isDrawing) "drawing"
            else if (n.isTodo) TodoCodec.decode(n.contentHtml).joinToString(" ") { it.text }
            else Html.fromHtml(n.contentHtml, Html.FROM_HTML_MODE_COMPACT).toString()
            n.id to (n.title + " " + body)
        }
    }
    val q = query?.trim().orEmpty()
    val inNotebook = if (query != null || selectedNotebook == null) notes else notes.filter { it.notebook == selectedNotebook }
    val shown = if (q.isEmpty()) inNotebook else inNotebook.filter { searchText[it.id]?.contains(q, ignoreCase = true) == true }
    // Selection: long-press a note to start, tap more to add or remove
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }
    val selecting = selectedIds.isNotEmpty()
    // Notes that were deleted can't stay selected
    if (selecting && selectedIds.any { id -> notes.none { it.id == id } }) {
        selectedIds = selectedIds.filter { id -> notes.any { it.id == id } }.toSet()
    }
    val selectedNotes = notes.filter { it.id in selectedIds }
    var moving by remember { mutableStateOf(false) }
    var sorting by remember { mutableStateOf(false) }
    val sort by viewModel.sort.collectAsState()
    BackHandler(enabled = selecting) { selectedIds = emptySet() }
    fun toggleSelect(note: Note) {
        selectedIds = if (note.id in selectedIds) selectedIds - note.id else selectedIds + note.id
    }

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun deleteWithUndo(list: List<Note>) {
        if (list.isEmpty()) return
        viewModel.deleteMany(list)
        selectedIds = emptySet()
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val message = if (list.size == 1) {
                val n = list.first()
                "\"" + n.title.ifBlank { if (n.isTodo) "To-do list" else if (n.isDrawing) "Drawing" else "Note" } + "\" moved to Trash"
            } else "${list.size} notes moved to Trash"
            val result = snackbar.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Long)
            if (result == SnackbarResult.ActionPerformed) viewModel.restoreMany(list)
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(NumioBackground)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            if (selecting) {
                SelectionBar(
                    count = selectedIds.size,
                    allPinned = selectedNotes.isNotEmpty() && selectedNotes.all { it.pinned },
                    onClose = { selectedIds = emptySet() },
                    onSelectAll = { selectedIds = shown.map { it.id }.toSet() },
                    onPin = {
                        viewModel.setPinnedMany(selectedNotes, !selectedNotes.all { it.pinned })
                        selectedIds = emptySet()
                    },
                    onMove = { moving = true },
                    onDelete = { deleteWithUndo(selectedNotes) },
                )
            } else if (query == null) {
                TopBar(
                    onSearch = { query = "" },
                    onSort = { sorting = true },
                    onLayout = { columns = if (columns == 2) 1 else 2 },
                    onSettings = onOpenSettings,
                )
                NotebookChips(
                    notebooks = notebooks,
                    counts = notes.groupingBy { it.notebook }.eachCount(),
                    total = notes.size,
                    selected = selectedNotebook,
                    onSelect = { selectedNotebook = it },
                    onLongPress = { chipActions = it },
                    onNew = { creatingNotebook = true },
                )
            } else {
                SearchBar(
                    value = query.orEmpty(),
                    onChange = { query = it },
                    onClose = { query = null },
                )
                if (q.isNotEmpty() && shown.isNotEmpty()) {
                    Text(
                        if (shown.size == 1) "1 note" else "${shown.size} notes",
                        color = NumioTextFaint,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 20.dp, bottom = 4.dp),
                    )
                }
            }

            if (notes.isEmpty()) {
                EmptyState()
            } else if (shown.isEmpty() && query == null) {
                EmptyNotebook(selectedNotebook.orEmpty())
            } else if (shown.isEmpty()) {
                NoResults(q)
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                ) {
                    items(shown, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            selected = note.id in selectedIds,
                            onClick = { if (selecting) toggleSelect(note) else onOpenNote(note) },
                            onLongClick = { toggleSelect(note) },
                            onToggleItem = { itemId -> viewModel.toggleTodoItem(note, itemId) },
                        )
                    }
                }
            }
        }

        // Dim the list while the + menu is open; tap outside to close
        AnimatedVisibility(visible = menuOpen, enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0xB8000000))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { menuOpen = false }
            )
        }

        if (!selecting) Column(
            Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(20.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnimatedVisibility(
                visible = menuOpen,
                enter = fadeIn() + slideInVertically { it / 4 },
                exit = fadeOut() + slideOutVertically { it / 4 },
            ) {
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    MenuPill(R.drawable.ic_pen, "Drawing") {
                        menuOpen = false
                        viewModel.newNoteNotebook = selectedNotebook ?: DEFAULT_NOTEBOOK
                        onNewDrawing()
                    }
                    MenuPill(R.drawable.ic_todo, "New to-do") {
                        menuOpen = false
                        viewModel.newNoteNotebook = selectedNotebook ?: DEFAULT_NOTEBOOK
                        onNewTodo()
                    }
                    MenuPill(R.drawable.ic_note, "New note", primary = true) {
                        menuOpen = false
                        viewModel.newNoteNotebook = selectedNotebook ?: DEFAULT_NOTEBOOK
                        onNewNote()
                    }
                }
            }
            FloatingActionButton(
                onClick = { menuOpen = !menuOpen },
                containerColor = if (menuOpen) NumioTrack else NumioYellow,
                contentColor = if (menuOpen) NumioAccentInk else NumioOnAccent,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.size(60.dp),
            ) {
                Icon(
                    painterResource(if (menuOpen) R.drawable.ic_close else R.drawable.ic_add),
                    contentDescription = if (menuOpen) "Close menu" else "New",
                    modifier = Modifier.size(if (menuOpen) 24.dp else 26.dp),
                )
            }
        }

        // Undo bar, above the + button
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 96.dp, bottom = 16.dp),
        ) { data ->
            Snackbar(
                snackbarData = data,
                containerColor = NumioRaised,
                contentColor = NumioText,
                actionColor = NumioAccentInk,
                shape = RoundedCornerShape(16.dp),
            )
        }
    }

    if (sorting) {
        SortSheet(sort = sort, onChange = { viewModel.setSort(it) }, onDismiss = { sorting = false })
    }

    if (moving) {
        val common = selectedNotes.map { it.notebook }.distinct().singleOrNull().orEmpty()
        NotebookPickerSheet(
            notebooks = notebooks,
            current = common,
            onPick = { nb -> viewModel.moveMany(selectedNotes, nb); moving = false; selectedIds = emptySet() },
            onCreate = { nb ->
                viewModel.addNotebook(nb); viewModel.moveMany(selectedNotes, nb); moving = false; selectedIds = emptySet()
            },
            onDismiss = { moving = false },
        )
    }

    chipActions?.let { nb ->
        NotebookActionsSheet(
            name = nb,
            noteCount = notes.count { it.notebook == nb },
            onRename = { chipActions = null; renaming = nb },
            onDelete = {
                chipActions = null
                viewModel.deleteNotebook(nb)
                if (selectedNotebook == nb) selectedNotebook = null
            },
            onDismiss = { chipActions = null },
        )
    }

    renaming?.let { nb ->
        NameDialog(
            title = "Rename notebook",
            initial = nb,
            confirm = "Rename",
            taken = notebooks,
            onConfirm = { newName ->
                viewModel.renameNotebook(nb, newName)
                if (selectedNotebook == nb) selectedNotebook = newName
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }

    if (creatingNotebook) {
        NameDialog(
            title = "New notebook",
            initial = "",
            confirm = "Create",
            taken = notebooks,
            onConfirm = { nb -> viewModel.addNotebook(nb); selectedNotebook = nb; creatingNotebook = false },
            onDismiss = { creatingNotebook = false },
        )
    }
}

// All · Personal · Numio · … · +   (long-press a notebook to rename or delete it)
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NotebookChips(
    notebooks: List<String>,
    counts: Map<String, Int>,
    total: Int,
    selected: String?,
    onSelect: (String?) -> Unit,
    onLongPress: (String) -> Unit,
    onNew: () -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item(key = "all") { Chip("All · $total", selected == null, onClick = { onSelect(null) }) }
        items(notebooks, key = { "nb_$it" }) { nb ->
            val n = counts[nb] ?: 0
            Chip(
                if (n > 0) "$nb · $n" else nb,
                selected == nb,
                onClick = { onSelect(nb) },
                onLongClick = if (nb == DEFAULT_NOTEBOOK) null else ({ onLongPress(nb) }),
            )
        }
        item(key = "new") {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .border(1.dp, NumioDivider, CircleShape)
                    .clickable(onClick = onNew)
                    .semantics { contentDescription = "New notebook" },
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_add), null, tint = NumioTextSoft, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit, onLongClick: (() -> Unit)? = null) {
    Box(
        Modifier
            .height(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .then(
                if (selected) Modifier.background(NumioYellow)
                else Modifier.border(1.dp, NumioDivider, RoundedCornerShape(18.dp))
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = if (selected) NumioOnAccent else NumioTextBody,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun EmptyNotebook(name: String) {
    Column(
        Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("$name is empty", color = NumioText, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text("Tap + and new notes go here", color = NumioTextFaint, fontSize = 14.sp)
    }
}

// Top bar while notes are selected: ✕ · 3 selected · select all · pin · move · delete
@Composable
private fun SelectionBar(
    count: Int,
    allPinned: Boolean,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    onPin: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 12.dp, top = 20.dp, bottom = 12.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_close), "Cancel selection", tint = NumioText, modifier = Modifier.size(22.dp))
        }
        Text(
            "$count selected",
            color = NumioText,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f).padding(start = 4.dp),
        )
        BarAction(R.drawable.ic_select_all, "Select all", NumioText, onSelectAll)
        BarAction(R.drawable.ic_pin, if (allPinned) "Unpin" else "Pin", if (allPinned) NumioAccentInk else NumioText, onPin)
        BarAction(R.drawable.ic_folder, "Move to notebook", NumioText, onMove)
        BarAction(R.drawable.ic_delete, "Delete", Color(0xFFE5534B), onDelete)
    }
}

@Composable
private fun BarAction(icon: Int, label: String, tint: Color, onClick: () -> Unit) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp),
        colors = IconButtonDefaults.iconButtonColors(containerColor = NumioButton),
    ) {
        Icon(painterResource(icon), label, tint = tint, modifier = Modifier.size(20.dp))
    }
    Spacer(Modifier.width(6.dp))
}

// One option in the + menu: icon in a small circle, then the label
@Composable
private fun MenuPill(icon: Int, label: String, primary: Boolean = false, onClick: () -> Unit) {
    Row(
        Modifier
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(if (primary) NumioYellow else NumioRaised)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (primary) Color(0x1F000000) else NumioAccentSoft),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(icon),
                null,
                tint = if (primary) NumioOnAccent else NumioAccentInk,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            color = if (primary) NumioOnAccent else NumioText,
            fontSize = 15.sp,
            fontWeight = if (primary) FontWeight.ExtraBold else FontWeight.Bold,
        )
    }
}

@Composable
private fun TopBar(onSearch: () -> Unit, onSort: () -> Unit, onLayout: () -> Unit, onSettings: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("Notes", color = NumioText, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Spacer(Modifier.height(2.dp))
            // Numio glow underline
            Box(
                Modifier
                    .width(44.dp)
                    .height(3.dp)
                    .shadow(8.dp, RoundedCornerShape(2.dp), ambientColor = NumioYellow, spotColor = NumioYellow)
                    .background(NumioYellow, RoundedCornerShape(2.dp))
            )
        }
        RoundButton("Search", onSearch) { SearchGlyph() }
        Spacer(Modifier.width(4.dp))
        RoundButton("Sort", onSort) { SortGlyph() }
        Spacer(Modifier.width(4.dp))
        RoundButton("Layout", onLayout) { LayoutGlyph() }
        Spacer(Modifier.width(4.dp))
        // The N. gear has its own cream and yellow, so it keeps a dark badge on light themes
        val palette = LocalNotesPalette.current
        RoundButton("Settings", onSettings, background = if (palette.isLight) Color(0xFF1C1C1C) else NumioButton) {
            Icon(painterResource(R.drawable.ic_settings), null, tint = Color.Unspecified, modifier = Modifier.size(21.dp))
        }
    }
}

@Composable
private fun RoundButton(
    label: String,
    onClick: () -> Unit,
    background: Color = NumioButton,
    content: @Composable () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(44.dp).semantics { contentDescription = label },
        colors = IconButtonDefaults.iconButtonColors(containerColor = background),
    ) { content() }
}

// Search: ring and neck in the text colour, thick handle in the accent
@Composable
private fun SearchGlyph() {
    val ink = NumioText
    val accent = NumioYellow
    Canvas(Modifier.size(22.dp)) {
        val u = size.width / 24f
        drawCircle(ink, radius = 6f * u, center = Offset(9.5f * u, 9.5f * u), style = Stroke(2.2f * u))
        drawLine(ink, Offset(13.8f * u, 13.8f * u), Offset(15.4f * u, 15.4f * u), strokeWidth = 1.3f * u)
        drawLine(accent, Offset(16.6f * u, 16.6f * u), Offset(20.2f * u, 20.2f * u), strokeWidth = 3.4f * u, cap = StrokeCap.Round)
    }
}

// Sort: three lines getting shorter, the shortest in the accent
@Composable
private fun SortGlyph() {
    val ink = NumioText
    val accent = NumioYellow
    Canvas(Modifier.size(20.dp)) {
        val u = size.width / 24f
        drawLine(ink, Offset(4f * u, 6f * u), Offset(20f * u, 6f * u), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
        drawLine(ink, Offset(4f * u, 12f * u), Offset(15f * u, 12f * u), strokeWidth = 2.2f * u, cap = StrokeCap.Round)
        drawLine(accent, Offset(4f * u, 18f * u), Offset(10f * u, 18f * u), strokeWidth = 2.6f * u, cap = StrokeCap.Round)
    }
}

// Sort options, remembered
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSheet(sort: SortChoice, onChange: (SortChoice) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NumioCard,
        contentColor = NumioText,
        dragHandle = {
            Box(Modifier.padding(top = 10.dp, bottom = 6.dp).size(width = 36.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(NumioDivider))
        },
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
            Text(
                "Sort notes",
                color = NumioText,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            listOf("edited" to "Last edited", "created" to "Date created", "title" to "Title A–Z").forEach { (id, label) ->
                val on = sort.by == id
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onChange(sort.copy(by = id)) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) NumioYellow else NumioDivider, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(NumioYellow))
                    }
                    Spacer(Modifier.width(16.dp))
                    Text(label, color = NumioText, fontSize = 16.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Medium)
                }
            }
            Box(Modifier.padding(horizontal = 16.dp, vertical = 6.dp).fillMaxWidth().height(1.dp).background(NumioDivider))
            CheckRow("Reverse order", sort.reverse) { onChange(sort.copy(reverse = !sort.reverse)) }
            CheckRow("Unfinished to-do lists on top", sort.todosFirst) { onChange(sort.copy(todosFirst = !sort.todosFirst)) }
            Text(
                "Pinned notes always stay at the top.",
                color = NumioTextFaint,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, top = 6.dp),
            )
        }
    }
}

@Composable
private fun CheckRow(label: String, checked: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (checked) NumioYellow else Color.Transparent)
                .border(2.dp, if (checked) NumioYellow else NumioDivider, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) Icon(painterResource(R.drawable.ic_check), null, tint = NumioOnAccent, modifier = Modifier.size(13.dp))
        }
        Spacer(Modifier.width(16.dp))
        Text(label, color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Medium)
    }
}

// Layout: two tall boxes in the text colour, two small ones in the accent
@Composable
private fun LayoutGlyph() {
    val ink = NumioText
    val accent = NumioYellow
    Canvas(Modifier.size(21.dp)) {
        val u = size.width / 24f
        fun box(x: Float, y: Float, w: Float, h: Float, c: Color) = drawRoundRect(
            c, topLeft = Offset(x * u, y * u), size = Size(w * u, h * u),
            cornerRadius = CornerRadius(2f * u), style = Stroke(2f * u),
        )
        box(4f, 4f, 7f, 9f, ink)
        box(13f, 4f, 7f, 5f, accent)
        box(4f, 15f, 7f, 5f, accent)
        box(13f, 11f, 7f, 9f, ink)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NoteCard(
    note: Note,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleItem: (Long) -> Unit,
) {
    Box {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NumioCard)
            .border(2.dp, if (selected) NumioYellow else Color.Transparent, RoundedCornerShape(20.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (note.pinned) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(painterResource(R.drawable.ic_pin), null, tint = NumioAccentInk, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(6.dp))
                Text("PINNED", color = NumioAccentInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
            }
        }
        if (note.title.isNotBlank()) {
            Text(note.title, color = NumioText, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        if (note.isDrawing) {
            DrawingThumbnail(note.contentHtml)
        } else if (note.isTodo) {
            TodoPreview(note, onToggleItem)
        } else if (note.preview.isNotBlank()) {
            Text(note.preview, color = NumioTextSoft, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
        Text("${formatDate(note.updatedAt)} · ${note.notebook}", color = NumioTextFaint, fontSize = 11.sp)
    }
    // Yellow tick in the corner of a selected note
    if (selected) {
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(NumioYellow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_check), "Selected", tint = NumioOnAccent, modifier = Modifier.size(14.dp))
        }
    }
    }
}

// First few items of a to-do note, tickable right on the card, plus progress
@Composable
private fun TodoPreview(note: Note, onToggleItem: (Long) -> Unit) {
    val items = remember(note.contentHtml) { TodoCodec.decode(note.contentHtml) }
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        items.take(5).forEach { item ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleItem(item.id) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (item.done) {
                    Box(
                        Modifier.size(16.dp).clip(RoundedCornerShape(5.dp)).background(NumioYellow),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(painterResource(R.drawable.ic_check), null, tint = NumioOnAccent, modifier = Modifier.size(11.dp))
                    }
                } else {
                    Box(Modifier.size(15.dp).border(1.5.dp, NumioCheckBorder, RoundedCornerShape(5.dp)))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    item.text,
                    color = if (item.done) NumioTextFaint else NumioTextBody,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (item.done) TextDecoration.LineThrough else null,
                )
            }
        }
        if (items.size > 5) {
            Text("+${items.size - 5} more", color = NumioTextFaint, fontSize = 12.sp)
        }
        val done = items.count { it.done }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(NumioTrack)) {
            Box(Modifier.fillMaxWidth(done.toFloat() / items.size).height(4.dp).background(NumioYellow))
        }
    }
}

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, top = 20.dp, bottom = 12.dp)
            .height(52.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(NumioButton)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(painterResource(R.drawable.ic_back), "Close search", tint = NumioText, modifier = Modifier.size(22.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text("Search notes", color = NumioTextFaint, fontSize = 16.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = NumioText, fontSize = 16.sp),
                cursorBrush = SolidColor(NumioYellow),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier.fillMaxWidth().focusRequester(focus),
            )
        }
        if (value.isNotEmpty()) {
            IconButton(onClick = { onChange("") }) {
                Icon(painterResource(R.drawable.ic_close), "Clear", tint = NumioTextSoft, modifier = Modifier.size(18.dp))
            }
        } else {
            Box(Modifier.padding(end = 14.dp)) { SearchGlyph() }
        }
    }
}

@Composable
private fun NoResults(q: String) {
    Column(
        Modifier.fillMaxSize().padding(start = 32.dp, end = 32.dp, bottom = 160.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (q.isEmpty()) {
            Text("Type to search your notes", color = NumioTextFaint, fontSize = 14.sp)
        } else {
            Text("Nothing for \"$q\"", color = NumioText, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text("Try another word", color = NumioTextFaint, fontSize = 14.sp)
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxSize().padding(bottom = 120.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(NumioYellow))
        Spacer(Modifier.height(16.dp))
        Text("No notes yet", color = NumioText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Tap + to write your first one", color = NumioTextFaint, fontSize = 14.sp)
    }
}

private fun formatDate(millis: Long): String {
    val now = System.currentTimeMillis()
    val day = 24 * 60 * 60 * 1000L
    return when {
        now - millis < day && sameDay(now, millis) -> "Today"
        now - millis < 2 * day && sameDay(now - day, millis) -> "Yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(millis))
    }
}

private fun sameDay(a: Long, b: Long): Boolean {
    val f = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return f.format(Date(a)) == f.format(Date(b))
}
