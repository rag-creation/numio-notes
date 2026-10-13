package com.rr.numio.notes.ui

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.text.Html
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.FileProvider
import com.rr.numio.notes.data.Drawing
import com.rr.numio.notes.data.TodoItem
import com.rr.numio.notes.ui.drawing.PaperColor
import com.rr.numio.notes.ui.drawing.drawPenStroke
import java.io.File

object Share {

    // A text note: title, then the text
    fun text(context: Context, title: String, html: String) {
        val body = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
        send(context, listOf(title.trim(), body).filter { it.isNotEmpty() }.joinToString("\n\n"), title)
    }

    // A to-do list as ☐ / ☑ lines
    fun todo(context: Context, title: String, items: List<TodoItem>) {
        val lines = items.filter { it.text.isNotBlank() }.joinToString("\n") { (if (it.done) "☑ " else "☐ ") + it.text }
        send(context, listOf(title.trim(), lines).filter { it.isNotEmpty() }.joinToString("\n\n"), title)
    }

    fun send(context: Context, text: String, title: String) {
        if (text.isBlank()) return
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            if (title.isNotBlank()) putExtra(Intent.EXTRA_SUBJECT, title.trim())
        }
        context.startActivity(Intent.createChooser(intent, null))
    }

    // A drawing as a picture (also used by Markdown export)
    fun render(drawing: Drawing, darkPage: Color = Color(0xFF1A1A1A)): Bitmap {
        val w = 1440
        val h = (w * drawing.aspect).toInt().coerceIn(400, 4000)
        val image = ImageBitmap(w, h)
        CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
            drawRect(if (drawing.paper) PaperColor else darkPage)
            drawing.strokes.forEach { s ->
                val c = when {
                    drawing.paper && s.color == 0xFFF3EEE3 -> 0xFF141414
                    !drawing.paper && s.color == 0xFF141414 -> 0xFFF3EEE3
                    else -> s.color
                }
                drawPenStroke(s.points, Color(c), s.width)
            }
        }
        return image.asAndroidBitmap()
    }

    // A drawing as a PNG picture. Light page → transparent-free cream; dark page → dark.
    fun drawing(context: Context, title: String, drawing: Drawing, darkPage: Color) {
        if (drawing.strokes.isEmpty()) return
        val image = render(drawing, darkPage)
        val dir = File(context.cacheDir, "shared").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }
        val safe = title.trim().ifEmpty { "Drawing" }.replace(Regex("[^\\p{L}\\p{N} _-]"), "").take(40).ifEmpty { "Drawing" }
        val file = File(dir, "$safe.png")
        file.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, context.packageName + ".files", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(safe, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, null))
    }
}
