package view

import de.uni_muenster.imi.oegd.common.GermType
import io.ktor.server.html.*
import kotlinx.html.*
import model.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


class LayoutTemplate(url2: String, val q: String? = null) : Template<HTML> {
    val header = Placeholder<FlowContent>()
    val content = Placeholder<FlowContent>()
    private val url = url2.removePrefix("/")
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
                        +"MRE-Report"
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
                                        classes = classes + "active"
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
                            +"Institut für Medizinische Informatik"
                        }
                        +" & "
                        a(href = "https://www.ukm.de/institute/hygiene", target = "_blank") {
                            +"Institut für Hygiene"
                        }
                        +" Münster"
                    }
                }
            }
        }
    }

    private fun UL.navItem(href: String, label: String) {
        li(classes = "nav-item") {
            if (url.startsWith(href.substringBefore("?"))) {
                classes = classes + "active"
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


fun FlowContent.drawCaseList(data: List<Map<String, String>>, lastUpdate: String, q: String) {
    drawInvalidateButton(lastUpdate, q)


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

private fun FlowContent.drawInvalidateButton(lastUpdate: String, q: String) {
    div(classes = "btn-toolbar") {
        form(classes = "form-inline", action = "invalidate-cache", method = FormMethod.post) {
            label {
                title = lastUpdate
                +"Teilbericht erstellt: ${LocalDateTime.parse(lastUpdate).toDifferenceFromNow()}"
            }
            hiddenInput {
                name = "q"
                value = q
            }
            button(type = ButtonType.submit, classes = "btn btn-light btn-sm") {
                +"Neu erstellen"
            }
        }
    }
}


fun FlowContent.drawOverviewTable(data: List<OverviewEntry>, lastUpdate: String, q: String) {
    drawInvalidateButton(lastUpdate, q)

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

fun FlowContent.drawDiagrams(
    data: Map<String, Map<Int?, String>>,
    years: List<Int>,
    yearEnabled: List<Int>,
    q: String?
) {
    form(action = "/statistic") {
        if (q != null) {
            hiddenInput(name = "q") { value = q }
        }
        for (year in years) {
            div(classes = "form-check form-check-inline") {
                checkBoxInput(classes = "form-check-input", name = "year[]") {
                    id = "p${year}"
                    value = "$year"
                    checked = year in yearEnabled
                }
                label(classes = "form-check-label") {
                    htmlFor = "p${year}"
                    +year.toString()
                }
            }
        }
        button(type = ButtonType.submit, classes = "btn btn-primary mb-2") { +"OK" }
    }

    script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
    div(classes = "container") {
        div(classes = "row") {
            for ((germ, data) in data) {
                div(classes = "col-3") {
                    style = "height: 400px;"
                    drawBarChart("Anzahl $germ", data.mapKeys { it.key.toString() })
                }
            }

        }
    }
}

fun FlowContent.drawYearSelector(cacheData: List<CacheData>, q: String?) {
    script(type = "text/javascript") {
        unsafe {
            +"window.deleteReport = function(button,xQueryParams) {"
            +"  button.disabled=true;"
            +"  var formData=new FormData();"
            +"  formData.append('toDelete', xQueryParams);"
            +"  fetch('statistic/deleteReport', {method: 'POST', body: formData})"
            +"      .then(res => window.location.reload());"
            +"  return false;"
            +"}"
        }
    }
    form(classes = "form-inline", method = FormMethod.post, action = "/statistic/create") {
        input(classes = "form-control b-2 mr-sm-2", name = "year", type = InputType.number) {
            min = "2000"
            max = LocalDate.now().year.toString()
            placeholder = "Jahr"
            required = true
        }
        if (q != null) {
            hiddenInput(name = "q") { value = q }
        }

        button(type = ButtonType.submit, classes = "btn btn-light btn-mb-2") {
            +"Bericht erstellen"
        }
    }
    form(action = "/statistic") {
        if (q != null) {
            hiddenInput(name = "q") { value = q }
        }
        for (cache in cacheData) {
            val xQueryParams = cache.metadata.xQueryParams
            div(classes = "form-check") {
                checkBoxInput(classes = "form-check-input", name = "year[]") {
                    id = "q${xQueryParams.year}"
                    value = "${xQueryParams.year}"
//                    checked = xQueryParams.year in yearsEnabled
                }
                label(classes = "form-check-label") {
                    htmlFor = "q${xQueryParams.year}"
                    +xQueryParams.year.toString()
                    val teilberichteZuErstellen = GermType.values().map { it.germtype } -
                            cache.germCache.filter { it.created != null }.map { it.type }
                    span(classes = "text-muted") {
                        +"Bericht erstellt: "
                        +LocalDateTime.parse(cache.metadata.timeUpdated).toDifferenceFromNow()
                        if (teilberichteZuErstellen.isNotEmpty()) {
                            +", Teilbericht(e) für ${teilberichteZuErstellen.joinToString()} müssen noch erzeugt werden."
                        }

                    }
                }
                button(type = ButtonType.submit, classes = "btn btn-outline-danger btn-small") {
                    onClick = "window.deleteReport(this,'${xQueryParams.toJson()}')"
                    +"delete"
                }
            }
        }

        button(type = ButtonType.submit, classes = "btn btn-secondary mb-2") {
            +"Diagramme erstellen"
        }
    }
}


fun LocalDateTime.toDifferenceFromNow(): String {
    val now = LocalDateTime.now()
    val years = ChronoUnit.YEARS.between(this, now)
    if (years == 1L) {
        return "$years year ago"
    } else if (years > 1) {
        return "$years years ago"
    }
    val month = ChronoUnit.MONTHS.between(this, now)
    if (month == 1L) {
        return "$month month ago"
    } else if (month > 1) {
        return "$month months ago"
    }
    val days = ChronoUnit.DAYS.between(this, now)
    if (days == 1L) {
        return "$days day ago"
    } else if (days > 1) {
        return "$days days ago"
    }
    val hours = ChronoUnit.HOURS.between(this, now)
    if (hours == 1L) {
        return "$hours hour ago"
    } else if (hours > 1) {
        return "$hours hours ago"
    }
    val minutes = ChronoUnit.MINUTES.between(this, now)
    if (minutes == 1L) {
        return "$minutes minute ago"
    } else if (minutes > 1) {
        return "$minutes minutes ago"
    }
    val seconds = ChronoUnit.SECONDS.between(this, now)
    return "$seconds second(s) ago"
}