package com.rr.numio.notes.ui.trash

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.notes.R
import com.rr.numio.notes.data.Note
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.theme.NumioAccentInk
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioCard
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioTextSoft
import kotlin.math.max

private val Danger = Color(0xFFE5534B)
private const val DAY = 24L * 60 * 60 * 1000

@Composable
fun TrashScreen(viewModel: NotesViewModel, onBack: () -> Unit) {
    val trash by viewModel.trash.collectAsState()
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<Note?>(null) }
    BackHandler(onBack = onBack)

    Column(
        Modifier
            .fillMaxSize()
            .background(NumioBackground)
            .statusBarsPadding()
    ) {
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = NumioText)
            }
            Text("Trash", color = NumioText, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
            if (trash.isNotEmpty()) {
                TextButton(onClick = { confirmEmpty = true }) {
                    Text("Empty", color = Danger, fontWeight = FontWeight.Bold)
                }
            }
        }

        if (trash.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp).padding(bottom = 120.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("Trash is empty", color = NumioText, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Text("Deleted notes stay here for 30 days", color = NumioTextFaint, fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.navigationBarsPadding(),
            ) {
                item {
                    Text(
                        "Notes here are deleted for good after 30 days.",
                        color = NumioTextFaint,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                    )
                }
                items(trash, key = { it.id }) { note ->
                    TrashCard(
                        note = note,
                        onRestore = { viewModel.restoreFromTrash(listOf(note)) },
                        onDelete = { confirmOne = note },
                    )
                }
            }
        }
    }

    if (confirmEmpty) {
        Confirm(
            title = "Empty Trash?",
            text = if (trash.size == 1) "1 note will be deleted for good." else "${trash.size} notes will be deleted for good.",
            action = "Empty",
            onConfirm = { viewModel.emptyTrash(); confirmEmpty = false },
            onDismiss = { confirmEmpty = false },
        )
    }
    confirmOne?.let { n ->
        Confirm(
            title = "Delete for good?",
            text = "\"" + n.title.ifBlank { "This note" } + "\" can't be brought back.",
            action = "Delete",
            onConfirm = { viewModel.deleteForever(listOf(n)); confirmOne = null },
            onDismiss = { confirmOne = null },
        )
    }
}

@Composable
private fun TrashCard(note: Note, onRestore: () -> Unit, onDelete: () -> Unit) {
    val daysLeft = max(0L, 30 - (System.currentTimeMillis() - note.deletedAt) / DAY)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(NumioCard)
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
    ) {
        Text(
            note.title.ifBlank { if (note.isDrawing) "Drawing" else if (note.isTodo) "To-do list" else note.preview.ifBlank { "Note" } },
            color = NumioText,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(end = 8.dp),
        )
        if (note.preview.isNotBlank() && note.title.isNotBlank()) {
            Text(note.preview, color = NumioTextSoft, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp, end = 8.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "${note.notebook} · " + if (daysLeft <= 1) "deleted tomorrow" else "$daysLeft days left",
                color = NumioTextFaint,
                fontSize = 12.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRestore) {
                Icon(painterResource(R.drawable.ic_undo), null, tint = NumioAccentInk, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore", color = NumioAccentInk, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete) {
                Icon(painterResource(R.drawable.ic_delete), "Delete for good", tint = Danger, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun Confirm(title: String, text: String, action: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NumioCard,
        titleContentColor = NumioText,
        textContentColor = NumioTextSoft,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(action, color = Danger, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = NumioTextSoft) } },
    )
}
