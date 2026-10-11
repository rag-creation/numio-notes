package com.rr.numio.notes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.rr.numio.notes.R
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioButton
import com.rr.numio.notes.ui.theme.NumioDivider
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioYellow

@Composable
fun EditorScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onDone: () -> Unit,
) {
    val richState = rememberRichTextState()
    var title by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var notebook by remember { mutableStateOf("Personal") }
    var loaded by remember { mutableStateOf(noteId == 0L) }

    LaunchedEffect(noteId) {
        viewModel.load(noteId)?.let { n ->
            title = n.title
            pinned = n.pinned
            notebook = n.notebook
            richState.setHtml(n.contentHtml)
        }
        loaded = true
    }

    // Save only when leaving with back (not on rotation), so a new note is never saved twice
    val saveAndExit = {
        if (loaded) viewModel.save(noteId, title, richState.toHtml(), pinned, notebook)
        onDone()
    }
    BackHandler(onBack = saveAndExit)

    Column(
        Modifier
            .fillMaxSize()
            .background(NumioBackground)
            .statusBarsPadding()
            .imePadding()
    ) {
        // Top bar: back, notebook, pin (yellow = on), more later
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = saveAndExit) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = NumioText)
            }
            Text(notebook, color = NumioTextFaint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { pinned = !pinned }) {
                Icon(
                    painterResource(R.drawable.ic_pin),
                    if (pinned) "Unpin" else "Pin",
                    tint = if (pinned) NumioYellow else NumioText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box {
                if (title.isEmpty()) {
                    Text("Title", color = NumioTextFaint, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(color = NumioText, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold),
                    cursorBrush = SolidColor(NumioYellow),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(NumioDivider))
            Box {
                if (richState.annotatedString.text.isEmpty()) {
                    Text("Start writing…", color = NumioTextFaint, fontSize = 16.sp)
                }
                BasicRichTextEditor(
                    state = richState,
                    textStyle = TextStyle(color = NumioText, fontSize = 16.sp, lineHeight = 25.sp),
                    cursorBrush = SolidColor(NumioYellow),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                )
            }
        }

        // Floating formatting pill
        Row(
            Modifier
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(NumioButton)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            val span = richState.currentSpanStyle
            FormatButton("B", span.fontWeight == FontWeight.Bold, FontWeight.ExtraBold) {
                richState.toggleSpanStyle(SpanStyle(fontWeight = FontWeight.Bold))
            }
            FormatButton("I", span.fontStyle == FontStyle.Italic, FontWeight.SemiBold, italic = true) {
                richState.toggleSpanStyle(SpanStyle(fontStyle = FontStyle.Italic))
            }
            FormatButton("U", span.textDecoration?.contains(TextDecoration.Underline) == true, FontWeight.SemiBold, underline = true) {
                richState.toggleSpanStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            }
            FormatButton("•", richState.isUnorderedList, FontWeight.ExtraBold) {
                richState.toggleUnorderedList()
            }
            FormatButton("1.", richState.isOrderedList, FontWeight.Bold) {
                richState.toggleOrderedList()
            }
        }
    }
}

@Composable
private fun FormatButton(
    label: String,
    active: Boolean,
    weight: FontWeight,
    italic: Boolean = false,
    underline: Boolean = false,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) Color(0xFF2A2410) else Color.Transparent),
    ) {
        Text(
            label,
            color = if (active) NumioYellow else NumioText,
            fontSize = 17.sp,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (underline) TextDecoration.Underline else null,
        )
    }
}
