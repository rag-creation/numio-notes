package com.rr.numio.notes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.notes.R
import com.rr.numio.notes.data.TodoCodec
import com.rr.numio.notes.data.TodoItem
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.notebooks.NotebookPickerSheet
import com.rr.numio.notes.ui.notebooks.NotebookPill
import androidx.compose.runtime.collectAsState
import com.rr.numio.notes.ui.theme.NumioAccentInk
import com.rr.numio.notes.ui.theme.NumioCheckBorder
import com.rr.numio.notes.ui.theme.NumioTrack
import com.rr.numio.notes.ui.theme.NumioOnAccent
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioDivider
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioYellow

@Composable
fun TodoScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onDone: () -> Unit,
) {
    val items = remember { mutableStateListOf<TodoItem>() }
    var title by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var notebook by remember { mutableStateOf(viewModel.newNoteNotebook) }
    var pickingNotebook by remember { mutableStateOf(false) }
    val notebooks by viewModel.notebooks.collectAsState()
    var loaded by remember { mutableStateOf(false) }
    var nextId by remember { mutableLongStateOf(1L) }
    var focusId by remember { mutableLongStateOf(-1L) }

    fun addItemAfter(index: Int) {
        val item = TodoItem(id = nextId++, text = "", done = false)
        items.add((index + 1).coerceIn(0, items.size), item)
        focusId = item.id
    }

    LaunchedEffect(noteId) {
        viewModel.load(noteId)?.let { n ->
            title = n.title
            pinned = n.pinned
            notebook = n.notebook
            items.addAll(TodoCodec.decode(n.contentHtml))
            nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1
        }
        // A new list starts with one empty item, ready to type
        if (items.isEmpty()) addItemAfter(-1)
        loaded = true
    }

    val saveAndExit = {
        if (loaded) viewModel.saveTodo(noteId, title, items.toList(), pinned, notebook)
        onDone()
    }
    BackHandler(onBack = saveAndExit)

    val doneCount = items.count { it.done && it.text.isNotBlank() }
    val total = items.count { it.text.isNotBlank() }

    Column(
        Modifier
            .fillMaxSize()
            .background(NumioBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = saveAndExit) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = NumioText)
            }
            NotebookPill(notebook) { pickingNotebook = true }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { pinned = !pinned }) {
                Icon(
                    painterResource(R.drawable.ic_pin),
                    if (pinned) "Unpin" else "Pin",
                    tint = if (pinned) NumioAccentInk else NumioText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box {
                if (title.isEmpty()) {
                    Text("To-do list", color = NumioTextFaint, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    singleLine = true,
                    textStyle = TextStyle(color = NumioText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                    cursorBrush = SolidColor(NumioYellow),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            if (total > 0) {
                ProgressBar(doneCount, total)
            } else {
                Box(Modifier.fillMaxWidth().height(1.dp).background(NumioDivider))
            }

            items.forEachIndexed { index, item ->
                key(item.id) {
                    TodoRow(
                        item = item,
                        requestFocus = item.id == focusId,
                        onToggle = { items[index] = item.copy(done = !item.done) },
                        onTextChange = { items[index] = item.copy(text = it) },
                        onNext = { addItemAfter(index) },
                        onRemove = {
                            items.removeAt(index)
                            if (items.isEmpty()) addItemAfter(-1)
                        },
                    )
                }
            }

            // Add item
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { addItemAfter(items.lastIndex) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_add), null, tint = NumioAccentInk, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Text("Add item", color = NumioAccentInk, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingNotebook) {
        NotebookPickerSheet(
            notebooks = notebooks,
            current = notebook,
            onPick = { notebook = it; pickingNotebook = false },
            onCreate = { viewModel.addNotebook(it); notebook = it; pickingNotebook = false },
            onDismiss = { pickingNotebook = false },
        )
    }
}

@Composable
private fun TodoRow(
    item: TodoItem,
    requestFocus: Boolean,
    onToggle: () -> Unit,
    onTextChange: (String) -> Unit,
    onNext: () -> Unit,
    onRemove: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(requestFocus) {
        if (requestFocus) runCatching { focus.requestFocus() }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        // Checkbox: yellow and ticked when done
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (item.done) {
                Box(
                    Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(NumioYellow),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_check), "Done", tint = NumioOnAccent, modifier = Modifier.size(15.dp))
                }
            } else {
                Box(
                    Modifier
                        .size(20.dp)
                        .border(1.5.dp, NumioCheckBorder, RoundedCornerShape(7.dp))
                )
            }
        }
        BasicTextField(
            value = item.text,
            onValueChange = { onTextChange(it.replace("\n", "")) },
            singleLine = true,
            textStyle = TextStyle(
                color = if (item.done) NumioTextFaint else NumioText,
                fontSize = 16.sp,
                textDecoration = if (item.done) TextDecoration.LineThrough else null,
            ),
            cursorBrush = SolidColor(NumioYellow),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { onNext() }),
            modifier = Modifier.weight(1f).focusRequester(focus),
        )
        IconButton(onClick = onRemove, modifier = Modifier.size(36.dp)) {
            Icon(painterResource(R.drawable.ic_close), "Remove", tint = NumioTextFaint, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun ProgressBar(done: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(NumioTrack)) {
            Box(
                Modifier
                    .fillMaxWidth(done.toFloat() / total)
                    .height(4.dp)
                    .background(NumioYellow)
            )
        }
        Text("$done of $total done", color = NumioTextFaint, fontSize = 12.sp)
    }
}
