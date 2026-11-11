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

    private fun freshRoot(): Node? {
        if (android.os.Build.VERSION.SDK_INT >= 33) uiAutomation.clearCache()
        return uiAutomation.rootInActiveWindow
    }
    private fun nodes(root: Node): Sequence<Node> = sequence {
        yield(root)
        for (i in 0 until root.childCount) root.getChild(i)?.let { yieldAll(nodes(it)) }
    }
    private fun find(label: String, predicate: (Node) -> Boolean): Node {
        val end = SystemClock.uptimeMillis() + 26000
        do {
            freshRoot()?.let { nodes(it).firstOrNull(predicate) }?.let { return it }
            SystemClock.sleep(180)
        } while (SystemClock.uptimeMillis() < end)
        val labels = freshRoot()?.let { nodes(it).mapNotNull { n -> n.text?.toString() }.toList() }
        error("Missing "+label+"; visible="+labels)
    }
    private fun tap(vararg names: String) {
        uiAutomation.waitForIdle(350, 6000)
        var n: Node? = find(names.joinToString()) { node ->
            val matches = node.contentDescription?.toString() in names || (node.text?.toString() in names && "Новая заметка" !in names)
            matches && node.isVisibleToUser && generateSequence(node) { it.parent }.any { it.isClickable && it.isEnabled }
        }
        while (n != null && !n.isClickable) n = n.parent
        val bounds = android.graphics.Rect()
        checkNotNull(n).getBoundsInScreen(bounds)
        val now = SystemClock.uptimeMillis()
        for (action in listOf(android.view.MotionEvent.ACTION_DOWN, android.view.MotionEvent.ACTION_UP)) {
            val event = android.view.MotionEvent.obtain(now, SystemClock.uptimeMillis(), action, bounds.exactCenterX(), bounds.exactCenterY(), 0)
            event.source = android.view.InputDevice.SOURCE_TOUCHSCREEN
            uiAutomation.injectInputEvent(event, true)
            event.recycle()
        }
        SystemClock.sleep(800)
    }
    private fun openEditor() {
        for (attempt in 1..4) {
            tap("Новая заметка")
            SystemClock.sleep(700)
            val editor = freshRoot()?.let { nodes(it).any { n -> n.text?.toString() == "Текст заметки" } }
            if (editor == true) return
        }
        error("Editor did not open")
    }
    private fun field(index: Int, value: String) {
        var found: Node? = null
        val deadline = SystemClock.uptimeMillis()+10000
        do {
            found = freshRoot()?.let { nodes(it).filter { n -> n.className?.toString() == "android.widget.EditText" }.elementAtOrNull(index) }
            if (found == null) SystemClock.sleep(180)
        } while (found == null && SystemClock.uptimeMillis()<deadline)
        check(found?.performAction(Node.ACTION_SET_TEXT, Bundle().apply {
            putCharSequence(Node.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, value)
        }) == true) { "Missing input "+index }
        SystemClock.sleep(800)
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
            val service = uiAutomation.serviceInfo
            service.eventTypes = android.view.accessibility.AccessibilityEvent.TYPES_ALL_MASK
            uiAutomation.serviceInfo = service
            targetContext.startActivity(Intent(targetContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            find("empty notebook") { it.text?.toString() == "Пока нет заметок" }
            shot("00-empty")
            openEditor()
            field(0, "Книжная полка"); field(1, "Взять в библиотеке сборник рассказов."); field(2, "книги, планы")
            tap("Сохранить")
            find("first note saved") { it.text?.toString() == "Книжная полка" && it.className?.toString() == "android.widget.TextView" }
            openEditor()
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
            runCatching { shot("failure") }
            val tree = freshRoot()?.let { nodes(it).map { n -> n.className.toString()+"/"+n.text+"/"+n.contentDescription }.toList() }
            result.putString("stream", e.stackTraceToString()+" Visible="+tree)
            finish(Activity.RESULT_CANCELED,result)
        }
    }
}
