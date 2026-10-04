package app.wata

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Every language must define the same strings as English, with the same placeholders. */
class TranslationsTest {

    @Test fun composeStringsAreComplete() = checkTranslations(File("src/commonMain/composeResources"))

    @Test fun androidStringsAreComplete() = checkTranslations(File("src/androidMain/res"))

    private fun checkTranslations(resDir: File) {
        val default = read(File(resDir, "values/strings.xml"))
        val languages = resDir.listFiles { f -> f.isDirectory && f.name.matches(Regex("values-[a-z]{2}")) }.orEmpty()
        assertTrue(languages.size >= 4, "expected translations in $resDir")
        for (dir in languages) {
            val translated = read(File(dir, "strings.xml"))
            assertEquals(default.keys, translated.keys, "${dir.name}: missing or extra strings")
            for ((name, placeholders) in default) {
                assertEquals(placeholders, translated.getValue(name), "${dir.name}/$name: placeholders differ")
            }
        }
    }

    /** Resource name -> its placeholders (e.g. "%1\$d"), or the item count for arrays. */
    private fun read(file: File): Map<String, List<String>> {
        val root = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).documentElement
        val result = mutableMapOf<String, List<String>>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val e = children.item(i) as? Element ?: continue
            if (e.getAttribute("translatable") == "false") continue
            val name = "${e.tagName}:${e.getAttribute("name")}"
            result[name] = when (e.tagName) {
                "string-array" -> listOf("items=${e.getElementsByTagName("item").length}")
                else -> Regex("""%\d+\$[ds]""").findAll(e.textContent).map { it.value }.toSortedSet().toList()
            }
        }
        return result
    }
}
