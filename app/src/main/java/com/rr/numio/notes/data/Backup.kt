package com.rr.numio.notes.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Numio Notes backup (.numio): one file with every note (not Trash) and every notebook.
 * Inside it's plain JSON, so it stays readable and open.
 * {
 *   "app": "numio-notes", "version": 1, "exportedAt": 1760000000000,
 *   "notebooks": ["Personal", "Numio"],
 *   "notes": [{ "title", "content", "notebook", "pinned", "createdAt", "updatedAt", "type" }]
 * }
 * type is "note" (content = HTML), "todo" (content = to-do lines) or "drawing" (content = strokes).
 */
object Backup {

    private const val APP = "numio-notes"

    fun write(notes: List<Note>, notebooks: List<String>): String {
        val root = JSONObject()
        root.put("app", APP)
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("notebooks", JSONArray(notebooks))
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(
                JSONObject()
                    .put("title", n.title)
                    .put("content", n.contentHtml)
                    .put("notebook", n.notebook)
                    .put("pinned", n.pinned)
                    .put("createdAt", n.createdAt)
                    .put("updatedAt", n.updatedAt)
                    .put("type", if (n.isDrawing) "drawing" else if (n.isTodo) "todo" else "note")
            )
        }
        root.put("notes", arr)
        return root.toString(1)
    }

    fun isBackup(text: String): Boolean =
        runCatching { JSONObject(text).optString("app") == APP }.getOrDefault(false)

    data class Restored(val notes: List<Note>, val notebooks: List<String>)

    // Notes come back without ids, so they're added as new rows
    fun read(text: String): Restored {
        val root = JSONObject(text)
        val books = root.optJSONArray("notebooks")?.let { a -> (0 until a.length()).map { a.optString(it) } }.orEmpty()
        val arr = root.optJSONArray("notes") ?: JSONArray()
        val notes = (0 until arr.length()).mapNotNull { i ->
            val o = arr.optJSONObject(i) ?: return@mapNotNull null
            val type = o.optString("type", "note")
            val updated = o.optLong("updatedAt", System.currentTimeMillis())
            Note(
                title = o.optString("title"),
                contentHtml = o.optString("content"),
                notebook = o.optString("notebook").ifBlank { "Personal" },
                pinned = o.optBoolean("pinned"),
                updatedAt = updated,
                createdAt = o.optLong("createdAt", updated),
                isTodo = type == "todo",
                isDrawing = type == "drawing",
            )
        }
        return Restored(notes, books.filter { it.isNotBlank() })
    }
}
