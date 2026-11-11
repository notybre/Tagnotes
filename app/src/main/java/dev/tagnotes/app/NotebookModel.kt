package dev.tagnotes.app

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

class NotebookModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(app, NotebookDb::class.java, "tagnotes.db").build()
    private val prefs = app.getSharedPreferences("display", 0)
    var theme by mutableStateOf(prefs.getString("theme", "Системная") ?: "Системная")
        private set
    var notes by mutableStateOf(emptyList<Note>())
        private set
    var busy by mutableStateOf(false)
        private set
    var loadError by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)
    init { refresh() }

    fun changeTheme(value: String) {
        theme = value
        prefs.edit().putString("theme", value).apply()
    }

    private fun readAll() = db.notes().list().map { Note(it.id, it.title, it.body, parseTags(it.tags), it.modified) }

    private fun operation(error: String, onError: () -> Unit = {}, task: suspend () -> Unit) {
        if (busy) return
        busy = true
        viewModelScope.launch {
            try { task() } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                message = error
                onError()
            } finally { busy = false }
        }
    }

    fun refresh() = operation("Не удалось открыть блокнот.", { loadError = true }) {
        notes = withContext(Dispatchers.IO) { readAll() }
        loadError = false
    }

    fun save(id: Long, title: String, body: String, tags: String, done: () -> Unit) {
        if (!validNote(title, body)) return
        operation("Не удалось сохранить заметку. Черновик остался в редакторе.") {
            notes = withContext(Dispatchers.IO) {
                val row = NoteRow().apply {
                    this.id = id
                    this.title = title.trim()
                    this.body = body.trim()
                    this.tags = parseTags(tags).joinToString(", ")
                    modified = System.currentTimeMillis()
                }
                db.runInTransaction(java.util.concurrent.Callable {
                    if (id == 0L) db.notes().insert(row) else check(db.notes().update(row) == 1)
                    readAll()
                })
            }
            done()
        }
    }

    fun delete(id: Long, done: () -> Unit) = operation("Не удалось удалить заметку.") {
        notes = withContext(Dispatchers.IO) {
            db.runInTransaction(java.util.concurrent.Callable {
                check(db.notes().delete(id) == 1)
                readAll()
            })
        }
        done()
    }

    fun export(uri: Uri) = operation("Не удалось записать файл. Выберите другую папку и повторите экспорт.") {
        val snapshot = notes
        withContext(Dispatchers.IO) {
            val stream = getApplication<Application>().contentResolver.openOutputStream(uri, "wt")
                ?: throw IOException("Файл недоступен")
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(markdown(snapshot)) }
        }
        message = "Файл сохранён"
    }
}

