package dev.tagnotes.app

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo as Node
import java.io.File

/** End-to-end demonstration. Run on a dedicated emulator with an empty notebook. */
class NotebookDemo : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }

    private fun nodes(root: Node): Sequence<Node> = sequence {
        yield(root)
        for (i in 0 until root.childCount) root.getChild(i)?.let { yieldAll(nodes(it)) }
    }
    private fun find(label: String, predicate: (Node) -> Boolean): Node {
        val end = SystemClock.uptimeMillis() + 26000
        do {
            uiAutomation.rootInActiveWindow?.let { nodes(it).firstOrNull(predicate) }?.let { return it }
            SystemClock.sleep(180)
        } while (SystemClock.uptimeMillis() < end)
        val labels = uiAutomation.rootInActiveWindow?.let { nodes(it).mapNotNull { n -> n.text?.toString() }.toList() }
        error("Missing "+label+"; visible="+labels)
    }
    private fun tap(vararg names: String) {
        var n: Node? = find(names.joinToString()) { it.text?.toString() in names || it.contentDescription?.toString() in names }
        while (n != null && !n.isClickable) n = n.parent
        check(n?.performAction(Node.ACTION_CLICK) == true)
        SystemClock.sleep(450)
    }
    private fun field(index: Int, value: String) {
        var found: Node? = null
        val deadline = SystemClock.uptimeMillis()+10000
        do {
            found = uiAutomation.rootInActiveWindow?.let { nodes(it).filter { n -> n.isEditable }.elementAtOrNull(index) }
            if (found == null) SystemClock.sleep(180)
        } while (found == null && SystemClock.uptimeMillis()<deadline)
        check(found?.performAction(Node.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(Node.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }) == true) { "Missing input "+index }
        SystemClock.sleep(450)
    }
    private fun shot(name: String) {
        SystemClock.sleep(2400)
        val folder = File(targetContext.getExternalFilesDir(null), "demo").apply { mkdirs() }
        val bmp = checkNotNull(uiAutomation.takeScreenshot())
        File(folder, name+".png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bmp.recycle()
    }
    override fun onStart() {
        val result = Bundle()
        try {
            targetContext.startActivity(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            find("empty notebook") { it.text?.toString() == "Пока нет заметок" }
            shot("00-empty")
            tap("Новая заметка")
            field(0, "Книжная полка"); field(1, "Взять в библиотеке сборник рассказов."); field(2, "книги, планы")
            tap("Сохранить")
            tap("Новая заметка")
            field(0, "После лекции"); field(1, "Разобрать пример с Room и повторить запросы к базе."); field(2, "учёба, планы")
            shot("01-editor")
            tap("Сохранить")
            find("list") { it.text?.toString() == "После лекции" }
            shot("02-notebook")
            tap("#учёба")
            shot("03-filter")
            field(0, "Room")
            shot("04-search")
            field(0, "погода")
            find("empty search") { it.text?.toString() == "Совпадений нет" }
            shot("05-empty-search")
            tap("Сбросить поиск и фильтр")
            tap("После лекции")
            field(0, "После семинара")
            tap("Сохранить")
            find("updated title") { it.text?.toString() == "После семинара" }
            tap("Настройки"); tap("Тёмная")
            shot("06-theme")
            tap("Заметки")
            shot("07-dark-list")
            tap("Настройки")
            tap("Сохранить в файл")
            find("file picker") { it.packageName?.toString()?.contains("documentsui") == true }
            shot("08-export")
            tap("SAVE","Save","Сохранить","СОХРАНИТЬ")
            find("export success") { it.text?.toString() == "Файл сохранён" }
            shot("09-exported")
            tap("Заметки"); tap("Книжная полка"); tap("Удалить"); tap("Отмена")
            tap("Удалить")
            val dialog = find("delete confirmation") { it.text?.toString() == "Удалить заметку?" }
            check(dialog.isVisibleToUser); tap("Удалить")
            find("remaining note") { it.text?.toString() == "После семинара" }
            shot("10-deleted")
            result.putString("stream", "PASS CRUD, cancel, tag, search, dark theme, document export")
            finish(Activity.RESULT_OK,result)
        } catch (e: Throwable) {
            result.putString("stream", e.stackTraceToString())
            finish(Activity.RESULT_CANCELED,result)
        }
    }
}
