package de.uni_muenster.imi.oegd.webapp

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.StringReader
import java.net.ServerSocket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import javax.xml.parsers.SAXParserFactory

fun parseCsv(text: String, headers: List<String>, separator: String = "||"): List<Map<String, String>> {
    if (text.isBlank()) return emptyList()

    return text.lines().map { line ->
        (headers zip line.split(separator).map { it.trim() }).toMap()
    }
}

/**
 * parse
 *     &lt;data key1="foo" key2="bar" />
 *     &lt;data key1="foo1" key2="bar1" />
 * to
 *     [{key1: "foo", key2: "bar"}, {key1: "foo1", key2: "bar1"}]
 */
fun parseXmlAttributeOrderPreserving(xml: String, tagName: String = "data"): List<LinkedHashMap<String, String>> {
    val result = mutableListOf<LinkedHashMap<String, String>>()

    val handler = object : DefaultHandler() {
        var attributesMap = LinkedHashMap<String, String>()

        override fun startElement(uri: String?, localName: String?, qName: String?, attributes: Attributes?) {
            if (qName == tagName && attributes != null) {
                for (i in 0 until attributes.length) {
                    attributesMap[attributes.getQName(i)] = attributes.getValue(i)
                }
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            if (qName == tagName) {
                result += attributesMap
                attributesMap = LinkedHashMap<String, String>()
            }
        }
    }

    SAXParserFactory.newInstance().newSAXParser().parse(
        InputSource(StringReader("<foo>$xml</foo>")), handler
    )
    return result
}

fun List<Map<String, String>>.toCsv(separator: String = "\t"): String {
    return this.joinToString("\n") { it.values.joinToString(separator) }
}


fun findOpenPortInRange(portRange: IntRange): Int? = portRange.find { isLocalPortFree(it) }

private fun isLocalPortFree(port: Int): Boolean = runCatching { ServerSocket(port).close() }.isSuccess

operator fun ResourceBundle.get(k: String): String = this.getString(k)


fun <K, V> Map<K, V>.transformEntry(key: K, transformer: (V) -> V): Map<K, V> {
    return this.mapValues { (k, v) -> if (k == key) transformer(v) else v }
}

private val pattern = DateTimeFormatter.ofPattern("dd.MM.yyyy")
fun toGermanDate(v: String): String = LocalDateTime.parse(v).toLocalDate().format(pattern)
