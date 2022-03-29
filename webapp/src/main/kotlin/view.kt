package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.GermType
import io.ktor.html.*
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.LocalDate
import kotlin.random.Random


class LayoutTemplate(url2: String, val q: String? = null) : Template<HTML> {
    val header = Placeholder<FlowContent>()
    val content = Placeholder<FlowContent>()
    val url = url2.removePrefix("/")
    override fun HTML.apply() {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
            title { +url }
            link(rel = "stylesheet", href = "/webjars/bootstrap/dist/css/bootstrap.min.css")
            link(rel = "stylesheet", href = "/webjars/bootstrap-icons/font/bootstrap-icons.css")
            link(rel = "stylesheet", href = "/static/custom-styles.css")

            script(src = "/webjars/jquery/dist/jquery.min.js") {}
            script(src = "/webjars/bootstrap/dist/js/bootstrap.min.js") {}

        }
        body {
            div(classes = "wrapper") {
                nav(classes = "navbar navbar-expand-md navbar-light bg-light") {
                    a(classes = "navbar-brand", href = "/?q=$q") {
                        +"MDReport"
                    }
                    button(classes = "navbar-toggler") {
                        attributes["data-toggle"] = "collapse"
                        attributes["data-target"] = "#navbarNav"
                        attributes["aria-controls"] = "navbarNav"
                        attributes["aria-expanded"] = "false"
                        attributes["aria-label"] = "Toggle navigation"
                        span(classes = "navbar-toggler-icon")
                    }
                    div(classes = "collapse navbar-collapse") {
                        id = "navbarNav"
                        ul(classes = "navbar-nav") {
                            navItem("global/overview?q=$q", "Krankenhauskennzahlen")
                            for (germ in GermType.values().map { it.germtype }) {
                                li(classes = "nav-item dropdown") {
                                    if (url.startsWith(germ)) {
                                        classes += "active"
                                    }
//                                attributes["aria-haspopup"] = "true"
                                    a(classes = "nav-link dropdown-toggle", href = "#") {
                                        id = "navbar$germ"
                                        role = "button"
                                        attributes["data-toggle"] = "dropdown"
                                        attributes["aria-expanded"] = "false"
                                        +germ
                                    }
                                    div(classes = "dropdown-menu") {
                                        attributes["aria-labelledby"] = "navbar$germ"
                                        a(
                                            classes = "dropdown-item",
                                            href = "/$germ/overview?q=$q"
                                        ) { +"Übersicht $germ" }
                                        a(classes = "dropdown-item", href = "/$germ/list?q=$q") { +"Fallliste" }
                                        a(classes = "dropdown-item", href = "/$germ/statistic?q=$q") { +"Diagramme" }
                                    }
                                }
                            }
                            navItem("statistic?q=$q", "Diagramme")
                            navItem("about?q=$q", "Über")
                        }
                    }
                    div(classes = "navbar float-left") {

                        ul(classes = "navbar-nav") {
                            drawSettingsModal(q)
                        }
                    }
                }

                main(classes = "content") {
                    role = "main"
                    div(classes = "container") {
                        h1 {
                            insert(header)
                        }
                        insert(content)
                    }
                }


                footer(classes = "footer") {
                    div(classes = "container") {
                        span(classes = "text-muted") {
                            +"© 2022 Copyright "
                        }
                        a(href = "https://imi.uni-muenster.de", target = "_blank") {
                            +"Institut für Medizinische Informatik Münster"
                        }
                    }
                }
            }
        }
    }

    private fun UL.navItem(href: String, label: String) {
        li(classes = "nav-item") {
            if (url.startsWith(href.substringBefore("?"))) {
                classes += "active"
            }
            a(classes = "nav-link", href = "/$href") {
                +label
            }
        }
    }
}

fun FlowContent.drawIndex(basexInfo: BasexInfo) {
    div(classes = "mb-5") {
        +"Bitte nutzen Sie die Navigationsleiste oben, um zwischen den verschiedenen Funktionen zu navigieren!"
    }

    div {
        table(classes = "table") {
            tr {
                td {
                    attributes["colspan"] = "2"
                    +"Aktuelle Einstellungen"
                }
            }
            if (basexInfo is RestConnectionInfo) {
                tr {
                    td { +"URL: " }
                    td { +basexInfo.serverUrl }
                }
                tr {
                    td { +"Datenbank: " }
                    td { +basexInfo.databaseId }
                }
            } else if (basexInfo is LocalBasexInfo) {
                tr {
                    td { +"Verzeichnis: " }
                    td { +basexInfo.directory }
                }
            }

        }
    }
}


fun FlowContent.drawCaseList(data: List<Map<String, String>>, lastUpdate: String) {
    drawInvalidateButton(lastUpdate)


    if (data.isEmpty()) {
        +"Fallliste ist leer"
        return
    }

    val keys = data.first().keys
    table(classes = "table") {
        thead {
            tr(classes = "sticky-tr") {
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

private fun FlowContent.drawInvalidateButton(lastUpdate: String) {
    div(classes = "btn-toolbar") {
        form(classes = "form-inline", action = "invalidate-cache", method = FormMethod.post) {
            label {
                +"Bericht erstellt: $lastUpdate"
            }
            button(type = ButtonType.submit, classes = "btn btn-light btn-sm") {
                +"Neu erstellen"
            }
        }
    }
}


fun FlowContent.drawOverviewTable(data: List<OverviewEntry>, lastUpdate: String) {
    drawInvalidateButton(lastUpdate)

    table(classes = "table") {
        for ((index, entry) in data.withIndex()) {
            tr {
                th {
                    span { +entry.title }
                }
                td {
                    span { +entry.data }
                    drawInfoModal(index, entry)
                }
            }
        }
    }
}

private fun FlowContent.drawInfoModal(index: Int, entry: OverviewEntry) {
    button(classes = "btn btn-link text-muted") {
        attributes["data-toggle"] = "modal"
        attributes["data-target"] = "#query-modal-$index"
        i {
            attributes["class"] = "bi bi-info-circle"
        }
    }
    div(classes = "modal fade") {
        id = "query-modal-$index"
        attributes["tabindex"] = "-1"
        attributes["role"] = "dialog"
        attributes["aria-labelledby"] = "#query-modal-$index-title"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-lg") {
            attributes["role"] = "document"
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "query-modal-$index-title"
                        +"${entry.title} - Query"
                    }
                    button(classes = "close", type = ButtonType.button) {
                        attributes["data-dismiss"] = "modal"
                        attributes["aria-label"] = "close"
                        span {
                            attributes["aria-hidden"] = "true"
                            +"×"
                        }
                    }
                }
                div(classes = "modal-body modal-query") {
                    pre {
                        +(entry.query + "\n")
                    }
                }
            }
        }
    }
}

private fun FlowContent.drawSettingsModal(q: String?) {
    val q2 = q?.let { Json.decodeFromString<XQueryParams>(it.replace("%22", "\"")) }
    a(classes = "navbar-text") {
        attributes["data-toggle"] = "modal"
        attributes["data-target"] = "#settings-modal"
        q2?.let {
            span(classes = "text-muted") { +"Jahr: " }
            span(classes = "font-weight-bold") { +("" + q2.year) }
        }
    }

    button(classes = "btn") {
        attributes["data-toggle"] = "modal"
        attributes["data-target"] = "#settings-modal"
        i(classes = "bi bi-gear-fill") { }
    }

    div(classes = "modal fade") {
        id = "settings-modal"
        attributes["tabindex"] = "-1"
        attributes["role"] = "dialog"
        attributes["aria-labelledby"] = "#settings-title"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-dialog-slideout modal-md") {
            attributes["role"] = "document"
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        id = "settings-title"
                        +"Einstellungen"
                    }
                    button(classes = "close", type = ButtonType.button) {
                        attributes["data-dismiss"] = "modal"
                        attributes["aria-label"] = "close"
                        span {
                            attributes["aria-hidden"] = "true"
                            +"×"
                        }
                    }
                }
                div(classes = "modal-body") {
                    form(action = "/settings/save", method = FormMethod.post) {
                        div(classes = "form-group") {
                            label {
                                attributes["for"] = "inputYear"
                                +"Jahr"
                            }
                            input(type = InputType.number) {
                                id = "inputYear"
                                min = "2000"
                                max = LocalDate.now().year.toString()
                                classes = setOf("form-control")
                                name = "year"
                                value = q2?.year?.toString() ?: ""
                            }
                        }
                        div(classes = "form-group") {
                            button(classes = "btn btn-light") {
                                attributes["data-dismiss"] = "modal"
                                +"Abbrechen"
                            }
                            button(type = ButtonType.submit, classes = "btn btn-secondary ml-2") {
                                +"Änderungen übernehmen"
                            }
                        }
                    }
                    hr {}
                    form(action = "/settings/uploadCache", method = FormMethod.post, encType = FormEncType.multipartFormData) {
                        div(classes = "form-group") {
                            label {
                                attributes["for"] = "inputCache"
                                +"Cache Datei hochladen"
                            }
                            input(type = InputType.file) {
                                classes = setOf("form-control-file")
                                id = "inputCache"
                                name = "uploadedCache"
                            }
                        }
                        div(classes = "form-group") {
                            button(type = ButtonType.submit, classes = "btn btn-secondary mt-2") {
                                +"Cache hochladen"
                            }
                            a(href = "/settings/downloadCache?q=$q", classes = "btn btn-secondary mt-2 ml-2") {
                                +"Aktuellen Cache herunterladen"
                            }
                        }
                    }
                }
            }
        }
    }
}

fun FlowContent.drawBarChart(label: String, data: Map<String, String>) = drawChart("bar", label, data)
fun FlowContent.drawPieChart(label: String, data: Map<String, String>) = drawChart("pie", label, data)

fun FlowContent.drawChart(type: String, label: String, data: Map<String, String>) {
    val backgroundColors = listOf(
        "rgba(255, 99, 132, 0.2)",
        "rgba(54, 162, 235, 0.2)",
        "rgba(255, 206, 86, 0.2)",
        "rgba(75, 192, 192, 0.2)",
        "rgba(153, 102, 255, 0.2)",
        "rgba(255, 159, 64, 0.2)"
    )
    val borderColors = listOf(
        "rgba(255, 99, 132, 1)",
        "rgba(54, 162, 235, 1)",
        "rgba(255, 206, 86, 1)",
        "rgba(75, 192, 192, 1)",
        "rgba(153, 102, 255, 1)",
        "rgba(255, 159, 64, 1)"
    )
    val randomId = "myChart" + Random.nextInt(100000)
    canvas {
        id = randomId
        width = "100%"
        height = "100%"
    }
    val labels = Json.encodeToString(data.keys)
    val dataValues = Json.encodeToString(data.values.toList())
    script(type = "text/javascript") {
        unsafe {
            +"""new Chart(document.getElementById('$randomId').getContext('2d'), {
    type: '$type',
    data: {
        labels: $labels,
        datasets: [{
            label: "$label",
            data: $dataValues,
            backgroundColor: ${
                if (type == "pie") Json.encodeToString(backgroundColors) else Json.encodeToString(
                    backgroundColors[0]
                )
            },
            borderColor: ${
                if (type == "pie") Json.encodeToString(borderColors) else Json.encodeToString(
                    backgroundColors[0]
                )
            },
            borderWidth: 1
        }]
    },
    options: {
        responsive: true,
        maintainAspectRatio: false,
        scales: {
            y: {
                beginAtZero: true,
            }
        }
    }
});"""
        }
    }


}
