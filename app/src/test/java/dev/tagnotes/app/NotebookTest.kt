package dev.tagnotes.app

import org.junit.Assert.*
import org.junit.Test

class NotebookTest {
    private val metro = Note(1, "План поездки", "Музей и метро", listOf("лето", "маршрут"), 0)
    private val exam = Note(2, "План зачёта", "Повторить Room", listOf("учёба"), 1)
    private val all = listOf(exam, metro)
    @Test fun tagsIgnoreCaseHashAndDuplicates() = assertEquals(listOf("учёба","мой проект"),parseTags(" #Учёба,учёба, , МОЙ ПРОЕКТ"))
    @Test fun searchAcrossContent() = assertEquals(listOf(metro),selectNotes(all,"  МЕТРО ",null))
    @Test fun searchAcrossTags() = assertEquals(listOf(exam),selectNotes(all,"#Учёба",null))
    @Test fun filterAndSearchIntersect() = assertEquals(listOf(metro),selectNotes(all,"план","лето"))
    @Test fun filterIsExact() = assertTrue(selectNotes(all,"","мар").isEmpty())
    @Test fun missingQueryHasEmptyResult() = assertTrue(selectNotes(all,"самолёт",null).isEmpty())
    @Test fun blankSearchPreservesOrder() = assertEquals(all,selectNotes(all," ",null))
    @Test fun bodyCanStandAlone() {
        assertFalse(validNote(" ","\n")); assertTrue(validNote("","Текст"))
        assertEquals("Первая строка",metro.copy(title="",body="\nПервая строка\nещё").caption())
    }
    @Test fun markdownPreservesUnicodeTagsAndBody() {
        assertEquals("# Tagnotes\n\n## План поездки\n\n#лето · #маршрут\n\nМузей и метро\n", markdown(listOf(metro)))
    }
}
