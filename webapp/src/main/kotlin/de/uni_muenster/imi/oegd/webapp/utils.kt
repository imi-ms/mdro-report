package de.uni_muenster.imi.oegd.webapp

import java.net.ServerSocket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

fun parseCsv(text: String, headers: List<String>, separator: String = "||"): List<Map<String, String>> {
    if (text.isBlank()) return emptyList()

    return text.lines().map { line ->
        (headers zip line.split(separator).map { it.trim() }).toMap()
    }
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
