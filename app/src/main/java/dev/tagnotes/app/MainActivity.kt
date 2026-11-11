package dev.tagnotes.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    private val model by viewModels<NotebookModel>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { Tagnotes(model) }
    }
}

private val light = lightColorScheme(primary = Color(0xFF245444), onPrimary = Color.White,
    secondaryContainer = Color(0xFFDBE9C8), onSecondaryContainer = Color(0xFF20392C),
    background = Color(0xFFF8F6EF), surface = Color(0xFFF8F6EF), surfaceContainer = Color(0xFFF0EEE4),
    surfaceContainerLow = Color(0xFFFFFDF7), primaryContainer = Color(0xFFDBE9C8),
    onPrimaryContainer = Color(0xFF20392C), onSurface = Color(0xFF202C25),
    onBackground = Color(0xFF202C25), outlineVariant = Color(0xFFD4D6C9))
private val dark = darkColorScheme(primary = Color(0xFFB6DAB3), onPrimary = Color(0xFF173929),
    secondaryContainer = Color(0xFF344B39), onSecondaryContainer = Color(0xFFE3EED7),
    background = Color(0xFF17221E), surface = Color(0xFF17221E), surfaceContainer = Color(0xFF24332B),
    surfaceContainerLow = Color(0xFF202D26), primaryContainer = Color(0xFF344B39),
    onPrimaryContainer = Color(0xFFE3EED7), onSurface = Color(0xFFE5E9DE),
    onBackground = Color(0xFFE5E9DE), outlineVariant = Color(0xFF455249))
private val radius = RoundedCornerShape(18.dp)
private val dateFormat = DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("ru"))
private fun stamp(time: Long) = dateFormat.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()))

@Composable
private fun Brand() {
    Text("TAGNOTES", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp,
        letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Heading(value: String) {
    Text(value, style = MaterialTheme.typography.headlineLarge, fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium)
}

@Composable
private fun Tagnotes(vm: NotebookModel) {
    val night = vm.theme == "Тёмная" || (vm.theme == "Системная" && isSystemInDarkTheme())
    var screen by rememberSaveable { mutableStateOf("notes") }
    var editId by rememberSaveable { mutableLongStateOf(0L) }
    var title by rememberSaveable { mutableStateOf("") }
    var body by rememberSaveable { mutableStateOf("") }
    var tags by rememberSaveable { mutableStateOf("") }
    var original by rememberSaveable { mutableStateOf(listOf("", "", "")) }
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var confirm by rememberSaveable { mutableStateOf("") }
    val host = remember { SnackbarHostState() }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) {
        it?.let(vm::export)
    }
    val focus = LocalFocusManager.current
    fun edit(note: Note?) {
        focus.clearFocus()
        editId = note?.id ?: 0L
        title = note?.title.orEmpty()
        body = note?.body.orEmpty()
        tags = note?.tags?.joinToString(", ") ?: filter.orEmpty()
        original = listOf(title, body, tags)
        screen = "edit"
    }
    fun back() {
        if (screen == "edit" && original != listOf(title, body, tags)) confirm = "discard"
        else screen = "notes"
    }
    BackHandler(screen != "notes") { if (!vm.busy) back() }
    LaunchedEffect(vm.message) {
        vm.message?.let { host.showSnackbar(it); vm.message = null }
    }
    LaunchedEffect(vm.notes, vm.busy) {
        if (!vm.busy && !vm.loadError && vm.notes.none { filter in it.tags }) filter = null
    }
    MaterialTheme(colorScheme = if (night) dark else light) {
        val view = androidx.compose.ui.platform.LocalView.current
        SideEffect {
            val activity = view.context as? ComponentActivity
            activity?.let {
                val bars = androidx.core.view.WindowCompat.getInsetsController(it.window, view)
                bars.isAppearanceLightStatusBars = !night
                bars.isAppearanceLightNavigationBars = !night
            }
        }
        Scaffold(modifier = Modifier.fillMaxSize().imePadding(), snackbarHost = { SnackbarHost(host) },
            bottomBar = {
                if (screen != "edit") NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    listOf("notes" to "Заметки", "settings" to "Настройки").forEach { (page, label) ->
                        NavigationBarItem(selected = screen == page, onClick = { focus.clearFocus(); screen = page },
                            icon = { Glyph(if (page == "notes") "book" else "settings") }, label = { Text(label) })
                    }
                }
            },
            floatingActionButton = {
                if (screen == "notes" && !vm.loadError) ExtendedFloatingActionButton(onClick = { edit(null) },
                    modifier = Modifier.semantics { contentDescription = "Новая заметка" },
                    containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Glyph("plus") }, text = { Text("Новая заметка") })
            }) { padding ->
            BoxWithConstraints(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                val width = Modifier.widthIn(max = 720.dp).fillMaxSize()
                when (screen) {
                    "notes" -> NotebookList(vm, query, { query = it }, filter, { filter = it }, { edit(it) }, width)
                    "edit" -> Column(width.verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { back() }, enabled = !vm.busy) { Glyph("back", "Назад") }
                            Spacer(Modifier.weight(1f))
                            if (editId != 0L) TextButton(onClick = { confirm = "delete" }, enabled = !vm.busy) {
                                Text("Удалить", color = MaterialTheme.colorScheme.error)
                            }
                            Button(onClick = { vm.save(editId, title, body, tags) { focus.clearFocus(); screen = "notes" } },
                                enabled = !vm.busy && validNote(title, body)) { Text("Сохранить") }
                        }
                        Brand()
                        Heading(if (editId == 0L) "Новая заметка" else "Редактор")
                        OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("Заголовок") }, singleLine = true, shape = radius)
                        OutlinedTextField(body, { body = it }, Modifier.fillMaxWidth(), label = { Text("Текст заметки") }, minLines = 5, shape = radius)
                        OutlinedTextField(tags, { tags = it }, Modifier.fillMaxWidth(), label = { Text("Теги") },
                            supportingText = { Text("Разделяйте теги запятыми") }, singleLine = true, shape = radius)
                    }
                    else -> Column(width.verticalScroll(rememberScrollState()).padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Brand(); Heading("Настройки")
                        Text("Оформление", style = MaterialTheme.typography.titleMedium)
                        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow, radius)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, radius)) {
                            for (choice in listOf("Системная", "Светлая", "Тёмная")) {
                                Row(Modifier.fillMaxWidth().clickable { vm.changeTheme(choice) }.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically) {
                                    Text(choice, Modifier.weight(1f))
                                    RadioButton(selected = choice == vm.theme, onClick = null)
                                }
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text("Экспорт блокнота", style = MaterialTheme.typography.titleMedium)
                        Text("Все заметки и теги будут записаны в один файл Markdown. Выберите папку для сохранения.")
                        Button(onClick = { try { picker.launch("Tagnotes.md") } catch (_: android.content.ActivityNotFoundException) {
                            vm.message = "Системный выбор файла недоступен"
                        } }, enabled = vm.notes.isNotEmpty() && !vm.busy) { Text("Сохранить в файл") }
                        if (vm.notes.isEmpty()) Text("Для экспорта добавьте первую заметку.")
                    }
                }
            }
        }
        if (confirm.isNotEmpty()) {
            val deleting = confirm == "delete"
            AlertDialog(onDismissRequest = { confirm = "" },
                title = { Text(if (deleting) "Удалить заметку?" else "Выйти без сохранения?") },
                text = { Text(if (deleting) "Заметка будет удалена из блокнота." else "Изменения в черновике будут потеряны.") },
                dismissButton = { TextButton(onClick = { confirm = "" }) { Text("Отмена") } },
                confirmButton = { TextButton(onClick = {
                    confirm = ""
                    if (deleting) vm.delete(editId) { screen = "notes" } else screen = "notes"
                }) { Text(if (deleting) "Удалить" else "Выйти") } })
        }
    }
}

@Composable
private fun NotebookList(vm: NotebookModel, query: String, changeQuery: (String) -> Unit,
    filter: String?, changeFilter: (String?) -> Unit, open: (Note) -> Unit, modifier: Modifier) {
    val tags = remember(vm.notes) { vm.notes.flatMap { it.tags }.distinct().sorted() }
    val visible = remember(vm.notes, query, filter) { selectNotes(vm.notes, query, filter) }
    LazyColumn(modifier, contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Brand(); Spacer(Modifier.height(6.dp)); Heading("Мой блокнот") }
        item { OutlinedTextField(query, changeQuery, Modifier.fillMaxWidth(), placeholder = { Text("Поиск по заметкам") },
            singleLine = true, shape = radius, leadingIcon = { Glyph("search") },
            trailingIcon = if (query.isNotEmpty()) { { IconButton(onClick = { changeQuery("") }) { Glyph("close", "Очистить поиск") } } } else null) }
        if (tags.isNotEmpty()) item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(filter == null, { changeFilter(null) }, { Text("Все") })
                tags.forEach { tag -> FilterChip(filter == tag, { changeFilter(if (filter == tag) null else tag) }, { Text("#"+tag) }) }
            }
        }
        when {
            vm.busy && vm.notes.isEmpty() -> item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            vm.loadError -> item { Text("Не удалось загрузить заметки"); TextButton(onClick = vm::refresh) { Text("Повторить") } }
            visible.isEmpty() -> item {
                Spacer(Modifier.height(30.dp))
                Text(if (vm.notes.isEmpty()) "Пока нет заметок" else "Совпадений нет", style = MaterialTheme.typography.headlineSmall, fontFamily = FontFamily.Serif)
                Spacer(Modifier.height(8.dp))
                if (vm.notes.isEmpty()) Text("Нажмите «Новая заметка», чтобы записать первую мысль.")
                else TextButton(onClick = { changeQuery(""); changeFilter(null) }) { Text("Сбросить поиск и фильтр") }
            }
            else -> {
                item { Text("ЗАМЕТОК  /  "+visible.size, fontSize = 12.sp, letterSpacing = 1.4.sp, color = MaterialTheme.colorScheme.primary) }
                items(visible, key = { it.id }) { note ->
                    Surface(onClick = { open(note) }, shape = radius, color = MaterialTheme.colorScheme.surfaceContainerLow,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.fillMaxWidth().padding(17.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                            Text(stamp(note.modified), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                            Text(note.caption(), fontFamily = FontFamily.Serif, fontSize = 23.sp, lineHeight = 27.sp,
                                maxLines = 2, overflow = TextOverflow.Ellipsis)
                            if (note.body.isNotBlank() && note.title.isNotBlank()) Text(note.body, maxLines = 3, overflow = TextOverflow.Ellipsis)
                            if (note.tags.isNotEmpty()) Text(note.tags.joinToString("    ") { "#"+it }, maxLines = 2, overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}

