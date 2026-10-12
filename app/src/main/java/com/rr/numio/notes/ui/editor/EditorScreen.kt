package com.rr.numio.notes.ui.editor

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.style.TextAlign
import com.mohamedrejeb.richeditor.annotation.ExperimentalRichTextApi
import com.mohamedrejeb.richeditor.paragraph.type.ListMarkerStyleBehavior
import com.mohamedrejeb.richeditor.paragraph.type.ListPrefixAlignment
import com.mohamedrejeb.richeditor.paragraph.type.OrderedListStyleType
import com.mohamedrejeb.richeditor.paragraph.type.UnorderedListStyleType
import com.rr.numio.notes.ui.theme.NotesFont
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
import com.mohamedrejeb.richeditor.model.RichTextState
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.text.TextRange
import com.mohamedrejeb.richeditor.ui.BasicRichTextEditor
import com.rr.numio.notes.R
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.theme.NumioAccentInk
import com.rr.numio.notes.ui.theme.NumioAccentSoft
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioButton
import com.rr.numio.notes.ui.theme.NumioDivider
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioYellow

@OptIn(ExperimentalRichTextApi::class)
@Composable
fun EditorScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onDone: () -> Unit,
) {
    val richState = rememberRichTextState()

    // Lists: same indent for numbers and bullets so the text lines up,
    // and a little more room after "1." and "•"
    LaunchedEffect(Unit) {
        richState.config.listIndent = 34
        richState.config.orderedListStyleType = NotesNumbering
        // Bullet + en space + hair space + the editor's own space = exactly the width of "1." + en space
        // (Inter, equal-width digits), so bullet text starts where numbered text starts
        richState.config.unorderedListStyleType = UnorderedListStyleType.from("•\u2002\u200A", "◦\u2002\u200A", "▪\u2002\u200A")
        // Markers keep the plain style: no bold, italic or underlined numbers
        richState.config.listMarkerStyleBehavior = ListMarkerStyleBehavior.AlwaysDefault
        // The next line keeps the style you were typing with
        richState.config.preserveStyleOnEmptyLine = true
        // Every marker starts at the same left edge (1. and 10. line up on the left)
        richState.config.listPrefixAlignment = ListPrefixAlignment.Start
    }
    var title by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var notebook by remember { mutableStateOf("Personal") }
    var loaded by remember { mutableStateOf(noteId == 0L) }

    // What B / I / U were set to, and from which text position. Keyboards send a word that is
    // still being typed as a "replace the whole word" on every key, and the editor library then
    // restyles the word like the text before it, losing the change. So we re-apply it.
    var styleFrom by remember { mutableStateOf<Int?>(null) }
    var wanted by remember { mutableStateOf(Marks()) }

    LaunchedEffect(richState) {
        var lastText = richState.annotatedString.text
        snapshotFlow { richState.annotatedString.text to richState.selection }.collect { (text, sel) ->
            val from = styleFrom
            if (from != null) {
                when {
                    // Cursor moved without typing: the pick only applies where it was made
                    text == lastText -> styleFrom = null
                    !sel.collapsed || sel.min < from -> styleFrom = null
                    text.substring(from, sel.min.coerceAtMost(text.length)).contains('\n') ->
                        // New line: the editor carries the style over by itself
                        styleFrom = null
                    sel.min > from -> applyMarks(richState, TextRange(from, sel.min), wanted)
                }
            }
            lastText = text
        }
    }

    fun toggle(kind: Mark) {
        val sel = richState.selection
        val current = styleFrom?.takeIf { sel.collapsed && sel.min >= it }?.let { wanted }
            ?: richState.currentSpanStyle.toMarks()
        richState.toggleSpanStyle(kind.spanStyle)
        if (sel.collapsed) {
            wanted = current.toggled(kind)
            styleFrom = sel.min
        } else {
            styleFrom = null
        }
    }

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
                    tint = if (pinned) NumioAccentInk else NumioText,
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
                    Text("Title", color = NumioTextFaint, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = NotesFont)
                }
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    textStyle = TextStyle(color = NumioText, fontSize = 28.sp, fontWeight = FontWeight.Bold, fontFamily = NotesFont),
                    cursorBrush = SolidColor(NumioYellow),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(NumioDivider))
            Box {
                if (richState.annotatedString.text.isEmpty()) {
                    Text("Start writing…", color = NumioTextFaint, fontSize = 16.sp, fontFamily = NotesFont)
                }
                BasicRichTextEditor(
                    state = richState,
                    textStyle = TextStyle(color = NumioText, fontSize = 16.sp, lineHeight = 25.sp, fontFamily = NotesFont, fontFeatureSettings = "tnum"),
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
            val shown = styleFrom?.takeIf { richState.selection.collapsed }?.let { wanted }
                ?: richState.currentSpanStyle.toMarks()
            FormatButton("B", shown.bold, FontWeight.ExtraBold) { toggle(Mark.Bold) }
            FormatButton("I", shown.italic, FontWeight.SemiBold, italic = true) { toggle(Mark.Italic) }
            FormatButton("U", shown.underline, FontWeight.SemiBold, underline = true) { toggle(Mark.Underline) }
            FormatButton("•", richState.isUnorderedList, FontWeight.ExtraBold) {
                richState.toggleUnorderedList()
            }
            FormatButton("1.", richState.isOrderedList, FontWeight.Bold) {
                richState.toggleOrderedList()
            }
            // Alignment: tap to go left → centre → right
            val align = richState.currentParagraphStyle.textAlign
            val next = when (align) {
                TextAlign.Center -> TextAlign.End
                TextAlign.End, TextAlign.Right -> TextAlign.Start
                else -> TextAlign.Center
            }
            AlignButton(align = align) {
                richState.addParagraphStyle(ParagraphStyle(textAlign = next))
            }
        }
    }
}

private enum class Mark(val spanStyle: SpanStyle) {
    Bold(SpanStyle(fontWeight = FontWeight.Bold)),
    Italic(SpanStyle(fontStyle = FontStyle.Italic)),
    Underline(SpanStyle(textDecoration = TextDecoration.Underline)),
}

private data class Marks(val bold: Boolean = false, val italic: Boolean = false, val underline: Boolean = false) {
    fun has(m: Mark) = when (m) { Mark.Bold -> bold; Mark.Italic -> italic; Mark.Underline -> underline }
    fun toggled(m: Mark) = when (m) {
        Mark.Bold -> copy(bold = !bold)
        Mark.Italic -> copy(italic = !italic)
        Mark.Underline -> copy(underline = !underline)
    }
}

private fun SpanStyle.toMarks() = Marks(
    bold = fontWeight == FontWeight.Bold,
    italic = fontStyle == FontStyle.Italic,
    underline = textDecoration?.contains(TextDecoration.Underline) == true,
)

// Make the text in `range` match the wanted B / I / U, touching only what differs
private fun applyMarks(state: RichTextState, range: TextRange, wanted: Marks) {
    val chars = (range.min until range.max).map { state.getSpanStyle(TextRange(it, it + 1)).toMarks() }
    Mark.entries.forEach { m ->
        if (wanted.has(m) && !chars.all { it.has(m) }) state.addSpanStyle(m.spanStyle, range)
        if (!wanted.has(m) && chars.any { it.has(m) }) state.removeSpanStyle(m.spanStyle, range)
    }
}

// 1. 2. 3. at the first level, then a. b. c., then i. ii. iii.
private object NotesNumbering : OrderedListStyleType {
    override fun format(number: Int, listLevel: Int): String = when ((listLevel - 1) % 3) {
        0 -> number.toString()
        1 -> OrderedListStyleType.LowerAlpha.format(number, listLevel)
        else -> OrderedListStyleType.LowerRoman.format(number, listLevel)
    }

    override fun getSuffix(listLevel: Int): String = ".\u2002"
}

@Composable
private fun AlignButton(align: TextAlign, onClick: () -> Unit) {
    val active = align == TextAlign.Center || align == TextAlign.End || align == TextAlign.Right
    val ink = if (active) NumioAccentInk else NumioText
    val label = when (align) {
        TextAlign.Center -> "Aligned centre"
        TextAlign.End, TextAlign.Right -> "Aligned right"
        else -> "Aligned left"
    }
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(if (active) NumioAccentSoft else Color.Transparent)
            .semantics { contentDescription = label },
    ) {
        // Four lines; the short ones move with the alignment
        Canvas(Modifier.size(20.dp)) {
            val u = size.width / 24f
            val stroke = 2.2f * u
            val rows = listOf(5f to 18f, 10f to 12f, 15f to 18f, 20f to 12f)
            rows.forEach { (y, len) ->
                val x0 = when (align) {
                    TextAlign.Center -> (24f - len) / 2f
                    TextAlign.End, TextAlign.Right -> 21f - len
                    else -> 3f
                }
                drawLine(ink, Offset(x0 * u, y * u), Offset((x0 + len) * u, y * u), stroke, cap = StrokeCap.Round)
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
            .background(if (active) NumioAccentSoft else Color.Transparent),
    ) {
        Text(
            label,
            color = if (active) NumioAccentInk else NumioText,
            fontSize = 17.sp,
            fontWeight = weight,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = if (underline) TextDecoration.Underline else null,
        )
    }
}
