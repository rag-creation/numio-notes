package com.rr.numio.notes.data

import org.json.JSONObject
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Google Keep, from Google Takeout (takeout.google.com → Keep).
 * Takeout gives a .zip with one .json file per note:
 *   title, textContent, listContent [{text, isChecked}], isPinned, isTrashed,
 *   isArchived, labels [{name}], userEditedTimestampUsec
 * Images are not imported yet.
 */
object KeepImport {

    // A Takeout .zip: read every Keep .json inside
    fun readZip(input: InputStream): List<ImportedNote> {
        val notes = mutableListOf<ImportedNote>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (!entry.isDirectory && name.endsWith(".json", true) && "keep" in name.lowercase()) {
                    readJson(zip.readBytes().toString(Charsets.UTF_8))?.let { notes += it }
                }
            }
        }
        return notes
    }

    // One Keep .json note
    fun readJson(text: String): ImportedNote? {
        val o = runCatching { JSONObject(text) }.getOrNull() ?: return null
        if (!o.has("textContent") && !o.has("listContent") && !o.has("title")) return null
        if (o.optBoolean("isTrashed")) return null

        val label = o.optJSONArray("labels")?.let { arr ->
            (0 until arr.length()).mapNotNull { arr.optJSONObject(it)?.optString("name")?.takeIf { n -> n.isNotBlank() } }.firstOrNull()
        }
        val usec = o.optLong("userEditedTimestampUsec", 0L)
        val list = o.optJSONArray("listContent")?.let { arr ->
            (0 until arr.length()).mapNotNull { i ->
                val item = arr.optJSONObject(i) ?: return@mapNotNull null
                val t = item.optString("text").trim()
                if (t.isEmpty()) null else TodoItem(id = i.toLong() + 1, text = t, done = item.optBoolean("isChecked"))
            }
        }
        return ImportedNote(
            title = o.optString("title").trim(),
            notebook = label ?: "Keep",
            updatedAt = if (usec > 0) usec / 1000 else System.currentTimeMillis(),
            plainText = if (list == null) o.optString("textContent") else null,
            checklist = list,
            pinned = o.optBoolean("isPinned"),
            createdAt = o.optLong("createdTimestampUsec", 0L).takeIf { it > 0 }?.div(1000),
        )
    }
}
