package com.rr.numio.notes.ui.list

import android.widget.Toast
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
import androidx.compose.material3.FloatingActionButton
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
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val notes by viewModel.notes.collectAsState()
    // 2 columns (staggered) or 1 column list, switched by the layout button
    var columns by remember { mutableIntStateOf(2) }
    var menuOpen by remember { mutableStateOf(false) }
    BackHandler(enabled = menuOpen) { menuOpen = false }

    Box(
        Modifier
            .fillMaxSize()
            .background(NumioBackground)
            .statusBarsPadding()
    ) {
        Column(Modifier.fillMaxSize()) {
            TopBar(
                onSearch = { /* search comes later */ },
                onLayout = { columns = if (columns == 2) 1 else 2 },
                onSettings = onOpenSettings,
            )

            if (notes.isEmpty()) {
                EmptyState()
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(columns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalItemSpacing = 10.dp,
                ) {
                    items(notes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            onClick = { onOpenNote(note) },
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

        Column(
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
                    MenuPill(R.drawable.ic_mic, "Voice") {
                        Toast.makeText(context, "Voice notes are coming soon", Toast.LENGTH_SHORT).show()
                    }
                    MenuPill(R.drawable.ic_camera, "Camera") {
                        Toast.makeText(context, "Photo notes are coming soon", Toast.LENGTH_SHORT).show()
                    }
                    MenuPill(R.drawable.ic_todo, "New to-do") { menuOpen = false; onNewTodo() }
                    MenuPill(R.drawable.ic_note, "New note", primary = true) { menuOpen = false; onNewNote() }
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
    }
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
private fun TopBar(onSearch: () -> Unit, onLayout: () -> Unit, onSettings: () -> Unit) {
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

@Composable
private fun NoteCard(note: Note, onClick: () -> Unit, onToggleItem: (Long) -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NumioCard)
            .clickable(onClick = onClick)
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
        if (note.isTodo) {
            TodoPreview(note, onToggleItem)
        } else if (note.preview.isNotBlank()) {
            Text(note.preview, color = NumioTextSoft, fontSize = 13.sp, lineHeight = 19.sp, maxLines = 6, overflow = TextOverflow.Ellipsis)
        }
        Text("${formatDate(note.updatedAt)} · ${note.notebook}", color = NumioTextFaint, fontSize = 11.sp)
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
