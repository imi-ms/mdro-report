package de.uni_muenster.imi.oegd.common

import java.net.ServerSocket

fun parseCsv(text: String, headers: List<String>, separator: String = "||"): List<Map<String, String>> {
    return text.lines().map { line ->
        headers.zip(line.split(separator).map { it.trim() }).toMap()
    }
}


fun findOpenPortInRange(portRange: IntRange): Int? = portRange.find { isLocalPortFree(it) }

private fun isLocalPortFree(port: Int): Boolean = runCatching { ServerSocket(port).close() }.isSuccess