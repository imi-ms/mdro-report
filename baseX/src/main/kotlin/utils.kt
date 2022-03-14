package de.uni_muenster.imi.oegd.baseX

import java.net.ServerSocket

fun parseCsv(text: String, headers: List<String>): List<Map<String, String>> {
    return buildList {
        for (line in text.lineSequence()) {
            add(
                buildMap {
                    for ((content, header) in line.split("||").zip(headers)) {
                        put(header, content.trim())
                    }
                }
            )
        }
    }
}


fun findOpenPortInRange(portRange: ClosedRange<Int>): Int? =
    (portRange.start..portRange.endInclusive).find { isLocalPortFree(it) }

private fun isLocalPortFree(port: Int): Boolean = runCatching { ServerSocket(port).close() }.isSuccess