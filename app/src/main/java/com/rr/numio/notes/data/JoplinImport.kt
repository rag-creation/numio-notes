package com.rr.numio.notes.data

import java.io.InputStream
import java.time.Instant

// A note read from Joplin, still in Markdown
data class ImportedNote(
    val title: String,
    val markdown: String,
    val notebook: String,
    val updatedAt: Long,
)

data class ImportBatch(
    val notes: List<ImportedNote>,
    val skippedEncrypted: Int,
)

/**
 * Reads Joplin exports.
 *
 * JEX ("Joplin Export File") is a plain tar archive. Every item is a text file:
 *   title
 *   (blank line)
 *   body in Markdown
 *   (blank line)
 *   id: …
 *   parent_id: …
 *   updated_time: 2024-05-01T10:00:00.000Z
 *   type_: 1          (1 = note, 2 = notebook)
 * Images and attachments (resources/) are skipped for now.
 */
object JoplinImport {

    private val propLine = Regex("^([a-z_]+): ?(.*)$")

    fun readJex(input: InputStream): ImportBatch {
        val items = mutableListOf<Map<String, String>>()
        readTar(input) { name, bytes ->
            if (name.endsWith(".md") && !name.contains("resources/")) {
                parseItem(String(bytes, Charsets.UTF_8))?.let { items += it }
            }
        }

        val folders = items.filter { it["type_"] == "2" }.associate { it["id"].orEmpty() to it["_title"].orEmpty() }
        var encrypted = 0
        val notes = items.filter { it["type_"] == "1" }.mapNotNull { item ->
            if (item["encryption_applied"] == "1") {
                encrypted++
                return@mapNotNull null
            }
            ImportedNote(
                title = item["_title"].orEmpty().trim(),
                markdown = cleanMarkdown(item["_body"].orEmpty()),
                notebook = folders[item["parent_id"]]?.trim()?.ifBlank { null } ?: "Joplin",
                updatedAt = parseTime(item["updated_time"]) ?: System.currentTimeMillis(),
            )
        }
        return ImportBatch(notes, encrypted)
    }

    // Plain .md files (Joplin "Markdown" export, or any Markdown notes)
    fun readMarkdownFile(fileName: String, text: String): ImportedNote {
        var body = text.replace("\r\n", "\n")
        var title = fileName.substringBeforeLast('.').trim()
        // Front matter (Joplin "Markdown + Front Matter"): take the title from it
        if (body.startsWith("---\n")) {
            val end = body.indexOf("\n---", 4)
            if (end > 0) {
                body.substring(4, end).lines().forEach { line ->
                    propLine.find(line.trim())?.let { m ->
                        if (m.groupValues[1] == "title") title = m.groupValues[2].trim().trim('"')
                    }
                }
                body = body.substring(end + 4).trimStart('\n')
            }
        }
        // A leading "# Title" line is the title, not part of the text
        val first = body.lineSequence().firstOrNull().orEmpty()
        if (first.startsWith("# ")) {
            title = first.removePrefix("# ").trim()
            body = body.substringAfter('\n', "")
        }
        return ImportedNote(title, cleanMarkdown(body), "Imported", System.currentTimeMillis())
    }

    // title / body / properties, as described above
    private fun parseItem(raw: String): Map<String, String>? {
        val lines = raw.replace("\r\n", "\n").split('\n').toMutableList()
        while (lines.isNotEmpty() && lines.last().isBlank()) lines.removeAt(lines.lastIndex)
        val props = mutableMapOf<String, String>()
        while (lines.isNotEmpty()) {
            val m = propLine.find(lines.last()) ?: break
            props[m.groupValues[1]] = m.groupValues[2]
            lines.removeAt(lines.lastIndex)
        }
        if ("type_" !in props) return null
        val title = lines.firstOrNull().orEmpty()
        val body = if (lines.size > 2) lines.subList(2, lines.size).joinToString("\n").trimEnd() else ""
        props["_title"] = title
        props["_body"] = body
        return props
    }

    // Joplin links images and files as ":/<id>". Those files aren't imported yet,
    // so images become a small marker and file links keep only their text.
    private fun cleanMarkdown(md: String): String = md
        .replace(Regex("!\\[([^\\]]*)]\\(:/[0-9a-f]{32}[^)]*\\)")) { "[image" + (if (it.groupValues[1].isNotBlank()) ": " + it.groupValues[1] else "") + "]" }
        .replace(Regex("(?<!!)\\[([^\\]]*)]\\(:/[0-9a-f]{32}[^)]*\\)")) { it.groupValues[1] }
        .replace(Regex("<img[^>]*>"), "[image]")

    private fun parseTime(s: String?): Long? =
        s?.takeIf { it.isNotBlank() }?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }

    // Minimal tar reader: 512-byte header blocks, file data padded to 512
    private fun readTar(input: InputStream, onFile: (String, ByteArray) -> Unit) {
        val header = ByteArray(512)
        var longName: String? = null
        while (true) {
            if (!readFully(input, header)) return
            if (header.all { it.toInt() == 0 }) return // end of archive
            val name = cString(header, 0, 100)
            val prefix = cString(header, 345, 155)
            val size = cString(header, 124, 12).trim().ifEmpty { "0" }.toLong(8)
            val type = header[156].toInt().toChar()
            val data = ByteArray(size.toInt())
            if (!readFully(input, data)) return
            val pad = ((512 - size % 512) % 512).toInt()
            if (pad > 0) skipFully(input, pad)
            val fullName = longName ?: if (prefix.isNotEmpty()) "$prefix/$name" else name
            when (type) {
                'L' -> { longName = String(data, Charsets.UTF_8).trimEnd('\u0000'); continue } // GNU long name
                'x', 'g' -> continue // pax headers
                '0', '\u0000' -> onFile(fullName, data)
            }
            longName = null
        }
    }

    private fun cString(b: ByteArray, off: Int, len: Int): String {
        var end = off
        while (end < off + len && b[end].toInt() != 0) end++
        return String(b, off, end - off, Charsets.UTF_8)
    }

    private fun readFully(input: InputStream, buf: ByteArray): Boolean {
        var read = 0
        while (read < buf.size) {
            val n = input.read(buf, read, buf.size - read)
            if (n < 0) return read == 0 && buf.isEmpty()
            read += n
        }
        return true
    }

    private fun skipFully(input: InputStream, count: Int) {
        var left = count.toLong()
        while (left > 0) {
            val n = input.skip(left)
            if (n <= 0) { if (input.read() < 0) return; left-- } else left -= n
        }
    }
}
