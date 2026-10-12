package com.rr.numio.notes.ui.notebooks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.notes.R
import com.rr.numio.notes.ui.theme.NumioAccentInk
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioCard
import com.rr.numio.notes.ui.theme.NumioDivider
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioTextSoft
import com.rr.numio.notes.ui.theme.NumioYellow

// Pick a notebook for a note, or make a new one
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookPickerSheet(
    notebooks: List<String>,
    current: String,
    onPick: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var naming by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NumioCard,
        contentColor = NumioText,
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
            Text(
                "Move to notebook",
                color = NumioTextSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                notebooks.forEach { name ->
                    val selected = name == current
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(name) }
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (selected) NumioYellow else NumioDivider)
                        )
                        Spacer(Modifier.width(16.dp))
                        Text(
                            name,
                            color = NumioText,
                            fontSize = 16.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        if (selected) {
                            Icon(painterResource(R.drawable.ic_check), "Current", tint = NumioAccentInk, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { naming = true }
                    .padding(horizontal = 16.dp, vertical = 15.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(R.drawable.ic_add), null, tint = NumioAccentInk, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(14.dp))
                Text("New notebook", color = NumioAccentInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (naming) {
        NameDialog(
            title = "New notebook",
            initial = "",
            confirm = "Create",
            taken = notebooks,
            onConfirm = { name -> naming = false; onCreate(name) },
            onDismiss = { naming = false },
        )
    }
}

// Long-press a notebook chip: rename or delete it
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookActionsSheet(
    name: String,
    noteCount: Int,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NumioCard,
        contentColor = NumioText,
        dragHandle = { SheetHandle() },
    ) {
        Column(Modifier.navigationBarsPadding().padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
            Text(
                "$name · " + if (noteCount == 1) "1 note" else "$noteCount notes",
                color = NumioTextSoft,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            )
            SheetAction("Rename", NumioText, onRename)
            SheetAction("Delete notebook", androidx.compose.ui.graphics.Color(0xFFE5534B), onDelete)
            Text(
                "Deleting keeps the notes. They move to Personal.",
                color = NumioTextFaint,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun SheetAction(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        label,
        color = color,
        fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
    )
}

@Composable
private fun SheetHandle() {
    Box(
        Modifier
            .padding(top = 10.dp, bottom = 6.dp)
            .size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(NumioDivider)
    )
}

// Small dialog to type a notebook name
@Composable
fun NameDialog(
    title: String,
    initial: String,
    confirm: String,
    taken: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var text by remember { mutableStateOf(initial) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    val clean = text.trim()
    val clash = clean != initial.trim() && taken.any { it.equals(clean, ignoreCase = true) }
    val ok = clean.isNotEmpty() && clean.length <= 30 && !clash

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = NumioCard,
        titleContentColor = NumioText,
        textContentColor = NumioText,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(NumioBackground)
                        .padding(horizontal = 14.dp, vertical = 14.dp)
                ) {
                    if (text.isEmpty()) Text("Notebook name", color = NumioTextFaint, fontSize = 16.sp)
                    BasicTextField(
                        value = text,
                        onValueChange = { text = it.take(30) },
                        singleLine = true,
                        textStyle = TextStyle(color = NumioText, fontSize = 16.sp),
                        cursorBrush = SolidColor(NumioYellow),
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { if (ok) onConfirm(clean) }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    )
                }
                if (clash) {
                    Text("You already have that notebook", color = NumioTextFaint, fontSize = 12.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(clean) }, enabled = ok) {
                Text(confirm, color = if (ok) NumioAccentInk else NumioTextFaint, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = NumioTextSoft) }
        },
    )
}

// Notebook name at the top of a note; tap to move the note
@Composable
fun NotebookPill(name: String, onClick: () -> Unit) {
    Row(
        Modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, NumioDivider, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(NumioYellow))
        Spacer(Modifier.width(8.dp))
        Text(name, color = NumioTextSoft, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(4.dp))
        Icon(painterResource(R.drawable.ic_chevron_down), null, tint = NumioTextFaint, modifier = Modifier.size(14.dp))
    }
}
