package com.rr.numio.notes.ui.drawing

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke as DrawStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rr.numio.notes.R
import com.rr.numio.notes.data.Drawing
import com.rr.numio.notes.data.DrawingCodec
import com.rr.numio.notes.data.Stroke
import com.rr.numio.notes.ui.NotesViewModel
import com.rr.numio.notes.ui.Share
import androidx.compose.ui.platform.LocalContext
import com.rr.numio.notes.ui.notebooks.NotebookPickerSheet
import com.rr.numio.notes.ui.notebooks.NotebookPill
import com.rr.numio.notes.ui.theme.NotesFont
import com.rr.numio.notes.ui.theme.NumioAccentInk
import com.rr.numio.notes.ui.theme.NumioAccentSoft
import com.rr.numio.notes.ui.theme.NumioBackground
import com.rr.numio.notes.ui.theme.NumioButton
import com.rr.numio.notes.ui.theme.NumioCard
import com.rr.numio.notes.ui.theme.NumioDivider
import com.rr.numio.notes.ui.theme.NumioText
import com.rr.numio.notes.ui.theme.NumioTextFaint
import com.rr.numio.notes.ui.theme.NumioYellow
import kotlin.math.hypot

// Pen colours: cream and ink work on both pages, the rest are Numio accents
private val PenColors = listOf(
    0xFFF3EEE3, // cream
    0xFF141414, // ink
    0xFFFFC107, // Numio yellow
    0xFFE5534B, // red
    0xFF4FA8F7, // blue
    0xFF5FB86A, // green
)

// Pen sizes, as a fraction of the page width
private val PenSizes = listOf(0.006f, 0.012f, 0.024f)

val PaperColor = Color(0xFFF6F1E6)

@Composable
fun DrawingScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    val darkPage = NumioCard
    var title by remember { mutableStateOf("") }
    var pinned by remember { mutableStateOf(false) }
    var notebook by remember { mutableStateOf(viewModel.newNoteNotebook) }
    var pickingNotebook by remember { mutableStateOf(false) }
    val notebooks by viewModel.notebooks.collectAsState()
    var loaded by remember { mutableStateOf(false) }

    val strokes = remember { mutableStateListOf<Stroke>() }
    val undo = remember { mutableStateListOf<List<Stroke>>() }
    val redo = remember { mutableStateListOf<List<Stroke>>() }
    var paper by remember { mutableStateOf(false) }
    var color by remember { mutableLongStateOf(PenColors[0]) }
    var size by remember { mutableFloatStateOf(PenSizes[1]) }
    var erasing by remember { mutableStateOf(false) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    // Points of the line being drawn right now
    val live = remember { mutableStateListOf<Pair<Float, Float>>() }

    LaunchedEffect(noteId) {
        viewModel.load(noteId)?.let { n ->
            title = n.title
            pinned = n.pinned
            notebook = n.notebook
            val d = DrawingCodec.decode(n.contentHtml)
            strokes.addAll(d.strokes)
            paper = d.paper
        }
        loaded = true
    }

    fun pushUndo(before: List<Stroke>) {
        undo.add(before)
        if (undo.size > 60) undo.removeAt(0)
        redo.clear()
    }

    val saveAndExit = {
        if (loaded) {
            val aspect = if (canvasSize.width > 0) canvasSize.height.toFloat() / canvasSize.width else 1.3f
            viewModel.saveDrawing(noteId, title, Drawing(aspect, strokes.toList(), paper), pinned, notebook)
        }
        onDone()
    }
    BackHandler(onBack = saveAndExit)

    // On the light page, cream would vanish, so it draws as ink (and the other way round)
    fun shown(c: Long): Color {
        val v = when {
            paper && c == PenColors[0] -> PenColors[1]
            !paper && c == PenColors[1] -> PenColors[0]
            else -> c
        }
        return Color(v)
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(NumioBackground)
            .statusBarsPadding()
    ) {
        // Top bar: back, notebook, undo, redo, pin
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = saveAndExit) {
                Icon(painterResource(R.drawable.ic_back), "Back", tint = NumioText)
            }
            NotebookPill(notebook) { pickingNotebook = true }
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = {
                    undo.removeLastOrNull()?.let { prev ->
                        redo.add(strokes.toList()); strokes.clear(); strokes.addAll(prev)
                    }
                },
                enabled = undo.isNotEmpty(),
            ) {
                Icon(painterResource(R.drawable.ic_undo), "Undo", tint = NumioText, modifier = Modifier.size(20.dp).alpha(if (undo.isNotEmpty()) 1f else 0.3f))
            }
            IconButton(
                onClick = {
                    redo.removeLastOrNull()?.let { next ->
                        undo.add(strokes.toList()); strokes.clear(); strokes.addAll(next)
                    }
                },
                enabled = redo.isNotEmpty(),
            ) {
                Icon(painterResource(R.drawable.ic_redo), "Redo", tint = NumioText, modifier = Modifier.size(20.dp).alpha(if (redo.isNotEmpty()) 1f else 0.3f))
            }
            IconButton(
                onClick = {
                    val aspect = if (canvasSize.width > 0) canvasSize.height.toFloat() / canvasSize.width else 1.3f
                    Share.drawing(context, title, Drawing(aspect, strokes.toList(), paper), darkPage)
                },
                enabled = strokes.isNotEmpty(),
            ) {
                Icon(painterResource(R.drawable.ic_share), "Share as picture", tint = NumioText, modifier = Modifier.size(20.dp).alpha(if (strokes.isNotEmpty()) 1f else 0.3f))
            }
            IconButton(onClick = { pinned = !pinned }) {
                Icon(
                    painterResource(R.drawable.ic_pin),
                    if (pinned) "Unpin" else "Pin",
                    tint = if (pinned) NumioAccentInk else NumioText,
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        // Title
        Box(Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
            if (title.isEmpty()) Text("Drawing", color = NumioTextFaint, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = NotesFont)
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                singleLine = true,
                textStyle = TextStyle(color = NumioText, fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = NotesFont),
                cursorBrush = SolidColor(NumioYellow),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // The page
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(if (paper) PaperColor else NumioCard)
                .onSizeChanged { canvasSize = it }
                .pointerInput(erasing, color, size, paper) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        val w = this.size.width.toFloat().coerceAtLeast(1f)
                        fun norm(o: Offset) = (o.x / w) to (o.y / w)
                        if (erasing) {
                            val before = strokes.toList()
                            fun eraseAt(o: Offset) {
                                val (x, y) = norm(o)
                                strokes.removeAll { s -> s.points.any { (px, py) -> hypot(px - x, py - y) < 0.035f + s.width / 2 } }
                            }
                            eraseAt(down.position)
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                eraseAt(change.position)
                                change.consume()
                            }
                            if (strokes.size != before.size) pushUndo(before)
                        } else {
                            live.clear()
                            live.add(norm(down.position))
                            down.consume()
                            while (true) {
                                val change = awaitPointerEvent().changes.firstOrNull() ?: break
                                if (!change.pressed) break
                                live.add(norm(change.position))
                                change.consume()
                            }
                            pushUndo(strokes.toList())
                            strokes.add(Stroke(color, size, live.toList()))
                            live.clear()
                        }
                    }
                }
                .semantics { contentDescription = "Drawing page" },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                strokes.forEach { s -> drawPenStroke(s.points, shown(s.color), s.width) }
                if (live.isNotEmpty()) drawPenStroke(live, shown(color), size)
            }
            if (strokes.isEmpty() && live.isEmpty()) {
                Text(
                    "Draw or sign here",
                    color = if (paper) Color(0xFF8A8478) else NumioTextFaint,
                    fontSize = 15.sp,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
        }

        // Tools
        Column(
            Modifier
                .navigationBarsPadding()
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(NumioButton)
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Colours
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PenColors.forEach { c ->
                    val on = !erasing && color == c
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(2.dp, if (on) NumioText else Color.Transparent, CircleShape)
                            .clickable { color = c; erasing = false }
                            .semantics { contentDescription = "Pen colour" },
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(Modifier.size(28.dp).clip(CircleShape).background(shown(c)).border(1.dp, NumioDivider, CircleShape))
                    }
                }
            }
            // Size · eraser · page · clear
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PenSizes.forEachIndexed { i, w ->
                        val on = !erasing && size == w
                        Box(
                            Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (on) NumioAccentSoft else Color.Transparent)
                                .clickable { size = w; erasing = false }
                                .semantics { contentDescription = listOf("Thin pen", "Medium pen", "Thick pen")[i] },
                            contentAlignment = Alignment.Center,
                        ) {
                            Box(Modifier.size((6 + i * 5).dp).clip(CircleShape).background(if (on) NumioAccentInk else NumioText))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ToolButton(R.drawable.ic_eraser, "Eraser", active = erasing) { erasing = !erasing }
                    ToolButton(R.drawable.ic_page, if (paper) "Dark page" else "Light page", active = paper) { paper = !paper }
                    ToolButton(R.drawable.ic_delete, "Clear all", active = false) {
                        if (strokes.isNotEmpty()) { pushUndo(strokes.toList()); strokes.clear() }
                    }
                }
            }
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
private fun ToolButton(icon: Int, label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(if (active) NumioAccentSoft else Color.Transparent)
            .clickable(onClick = onClick)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Icon(painterResource(icon), null, tint = if (active) NumioAccentInk else NumioText, modifier = Modifier.size(20.dp))
    }
}

// Smooth line through the points (curves through the midpoints), scaled to this canvas
fun DrawScope.drawPenStroke(points: List<Pair<Float, Float>>, color: Color, width: Float) {
    if (points.isEmpty()) return
    val w = size.width
    val px = points.map { Offset(it.first * w, it.second * w) }
    val strokeW = (width * w).coerceAtLeast(1f)
    if (px.size == 1) {
        drawCircle(color, radius = strokeW / 2, center = px[0])
        return
    }
    val path = Path().apply {
        moveTo(px[0].x, px[0].y)
        for (i in 1 until px.size) {
            val prev = px[i - 1]
            val cur = px[i]
            val mid = Offset((prev.x + cur.x) / 2, (prev.y + cur.y) / 2)
            quadraticTo(prev.x, prev.y, mid.x, mid.y)
        }
        lineTo(px.last().x, px.last().y)
    }
    drawPath(path, color, style = DrawStroke(width = strokeW, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// Small picture of a drawing for the note card (very tall drawings are cropped)
@Composable
fun DrawingThumbnail(raw: String, modifier: Modifier = Modifier) {
    val d = remember(raw) { DrawingCodec.decode(raw) }
    val ratio = (1f / d.aspect).coerceIn(0.75f, 2f) // width / height
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(ratio)
            .clip(RoundedCornerShape(12.dp))
            .background(if (d.paper) PaperColor else NumioBackground)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            d.strokes.forEach { s ->
                val c = when {
                    d.paper && s.color == 0xFFF3EEE3 -> 0xFF141414
                    !d.paper && s.color == 0xFF141414 -> 0xFFF3EEE3
                    else -> s.color
                }
                drawPenStroke(s.points, Color(c), s.width)
            }
        }
    }
}
