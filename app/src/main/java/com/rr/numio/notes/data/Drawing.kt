package com.rr.numio.notes.data

// One pen line: colour (ARGB), width and points. Points are stored as fractions of the
// canvas width (x and y both), so the drawing scales to any screen and to the small card.
data class Stroke(val color: Long, val width: Float, val points: List<Pair<Float, Float>>)

// paper = light page (good for signatures), otherwise the dark Numio page
data class Drawing(val aspect: Float, val strokes: List<Stroke>, val paper: Boolean = false)

// Text format, one stroke per line:
//   D1 <aspect> <paper 0/1>
//   <color hex> <width> x,y x,y x,y …
object DrawingCodec {
    fun encode(d: Drawing): String = buildString {
        append("D1 ").append(fmt(d.aspect)).append(' ').append(if (d.paper) "1" else "0").append('\n')
        d.strokes.filter { it.points.isNotEmpty() }.forEach { s ->
            append(java.lang.Long.toHexString(s.color)).append(' ').append(fmt(s.width))
            s.points.forEach { (x, y) -> append(' ').append(fmt(x)).append(',').append(fmt(y)) }
            append('\n')
        }
    }

    fun decode(raw: String): Drawing {
        val lines = raw.lines().filter { it.isNotBlank() }
        val head = lines.firstOrNull()?.takeIf { it.startsWith("D1 ") }?.removePrefix("D1 ")?.split(' ').orEmpty()
        val aspect = head.getOrNull(0)?.toFloatOrNull() ?: 1.3f
        val paper = head.getOrNull(1) == "1"
        val strokes = lines.drop(1).mapNotNull { line ->
            val parts = line.trim().split(' ')
            if (parts.size < 3) return@mapNotNull null
            val color = parts[0].toLongOrNull(16) ?: return@mapNotNull null
            val width = parts[1].toFloatOrNull() ?: return@mapNotNull null
            val pts = parts.drop(2).mapNotNull { p ->
                val xy = p.split(',')
                val x = xy.getOrNull(0)?.toFloatOrNull()
                val y = xy.getOrNull(1)?.toFloatOrNull()
                if (x != null && y != null) x to y else null
            }
            if (pts.isEmpty()) null else Stroke(color, width, pts)
        }
        return Drawing(aspect, strokes, paper)
    }

    private fun fmt(f: Float): String = "%.4f".format(java.util.Locale.ROOT, f).trimEnd('0').trimEnd('.')
}
