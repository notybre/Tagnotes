package dev.tagnotes.app

import java.util.Locale

data class Note(val id: Long, val title: String, val body: String, val tags: List<String>, val modified: Long)

fun parseTags(raw: String): List<String> = raw.split(',', '\n')
    .map { it.trim().trimStart('#').trim().lowercase(Locale.ROOT) }
    .filter { it.isNotBlank() }.distinct()

fun validNote(title: String, body: String) = title.isNotBlank() || body.isNotBlank()

fun Note.caption() = title.ifBlank { body.lineSequence().firstOrNull { it.isNotBlank() }?.trim()?.take(70).orEmpty() }

fun selectNotes(notes: List<Note>, query: String, tag: String?): List<Note> {
    val needle = query.trim()
    return notes.filter { note ->
        (tag == null || tag in note.tags) && (needle.isEmpty() ||
            note.title.contains(needle, true) || note.body.contains(needle, true) ||
            note.tags.any { it.contains(needle.removePrefix("#"), true) })
    }
}

fun markdown(notes: List<Note>): String = buildString {
    appendLine("# Tagnotes")
    for (note in notes) {
        appendLine()
        appendLine("## " + note.caption().replace('\n', ' '))
        appendLine()
        if (note.tags.isNotEmpty()) {
            appendLine(note.tags.joinToString(" · ") { "#"+it })
            appendLine()
        }
        appendLine(note.body)
    }
}

