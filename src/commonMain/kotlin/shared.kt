import kotlinx.html.*

fun FlowContent.drawTable(data: List<Map<String, String>>) {
    val keys = data.first().keys
    table(classes = "table") {
        thead {
            tr {
                for (columnName in keys) {
                    th(scope = ThScope.col) { +columnName }
                }
            }
        }
        for (datum in data) {
            tr {
                for (key in keys) {
                    td {
                        +(datum[key] ?: "null")
                    }
                }

            }
        }
    }
}

fun parseCsv(text: String, headers: List<String>): List<Map<String, String>> {
    return buildList {
        for (line in text.split("\n")) {
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