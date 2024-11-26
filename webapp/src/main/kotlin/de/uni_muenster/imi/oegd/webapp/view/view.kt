package de.uni_muenster.imi.oegd.webapp.view

import de.uni_muenster.imi.oegd.webapp.get
import de.uni_muenster.imi.oegd.webapp.i18n
import de.uni_muenster.imi.oegd.webapp.model.*
import io.ktor.server.html.*
import kotlinx.html.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.text.MessageFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit


class LayoutTemplate(_url: String, private val q: String? = null) : Template<HTML> {
    val header = Placeholder<FlowContent>()
    val content = Placeholder<FlowContent>()
    private val url = _url.removePrefix("/")
    override fun HTML.apply() {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
            title { +url }
            link(rel = "stylesheet", href = "/static/bootstrap/dist/css/bootstrap.min.css")
            link(rel = "stylesheet", href = "/static/bootstrap-icons/font/bootstrap-icons.css")
            link(rel = "stylesheet", href = "/static/custom-styles.css")
            script(src = "/static/jquery/dist/jquery.min.js") {}
            script(src = "/static/bootstrap/dist/js/bootstrap.bundle.min.js") {}
        }
        body {
            div(classes = "wrapper") {
                nav(classes = "navbar navbar-expand-md navbar-light bg-light px-2") {
                    a(classes = "navbar-brand", href = "/?q=$q") {
                        +"MRE-Report"
                    }
                    button(classes = "navbar-toggler") {
                        attributes["data-bs-toggle"] = "collapse"
                        attributes["data-bs-target"] = "#navbarNav"
                        attributes["aria-controls"] = "navbarNav"
                        attributes["aria-expanded"] = "false"
                        attributes["aria-label"] = "Toggle navigation"
                        span(classes = "navbar-toggler-icon")
                    }
                    div(classes = "collapse navbar-collapse") {
                        id = "navbarNav"
                        ul(classes = "navbar-nav") {
                            navItem("global/overview?q=$q", i18n.getString("navigation.hospitalMetrics"))
                            for (germ in GermType.entries.map { it.germtype }) {
                                li(classes = "nav-item dropdown") {
                                    if (url.startsWith(germ)) {
                                        classes = classes + "active"
                                    }
//                                attributes["aria-haspopup"] = "true"
                                    a(classes = "nav-link dropdown-toggle", href = "#") {
                                        id = "navbar$germ"
                                        role = "button"
                                        attributes["data-bs-toggle"] = "dropdown"
                                        attributes["aria-expanded"] = "false"
                                        +germ
                                    }
                                    div(classes = "dropdown-menu") {
                                        attributes["aria-labelledby"] = "navbar$germ"
                                        a(classes = "dropdown-item", href = "/$germ/overview?q=$q") {
                                            +"${i18n.getString("navigation.overview")} $germ"
                                        }
                                        a(
                                            classes = "dropdown-item",
                                            href = "/$germ/list?q=$q"
                                        ) { +i18n.getString("navigation.list") }
                                        a(
                                            classes = "dropdown-item",
                                            href = "/$germ/statistic?q=$q"
                                        ) { +i18n.getString("navigation.diagrams") }
                                    }
                                }
                            }
                            navItem("statistic?q=$q", i18n.getString("navigation.diagrams"))
                            navItem("about?q=$q", i18n.getString("navigation.about"))
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
                            +"© 2024 Copyright "
                        }
                        a(
                            classes = "text-muted link-underline link-underline-opacity-50 link-underline-opacity-100-hover",
                            href = "https://imi.uni-muenster.de",
                            target = "_blank"
                        ) {
                            +i18n["footer.imi"]
                        }
                        +" & "
                        a(
                            classes = "text-muted link-underline link-underline-opacity-50 link-underline-opacity-100-hover",
                            href = "https://www.ukm.de/institute/hygiene",
                            target = "_blank"
                        ) {
                            +i18n["footer.ukmHygiene"]
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
        +i18n.getString("page.welcome.description")
    }

    div {
        table(classes = "table") {
            tr {
                td {
                    attributes["colspan"] = "2"
                    +i18n.getString("page.welcome.currentSettings")
                }
            }
            if (basexInfo is RestConnectionInfo) {
                tr {
                    td { +"${i18n.getString("page.welcome.URL")}: " }
                    td { +basexInfo.serverUrl }
                }
                tr {
                    td { +"${i18n.getString("page.welcome.database")}: " }
                    td { +basexInfo.databaseId }
                }
            } else if (basexInfo is LocalBasexInfo) {
                tr {
                    td { +"${i18n.getString("page.welcome.directory")}: " }
                    td { +basexInfo.directory }
                }
            }

        }
    }
}


fun FlowContent.drawCaseList(germ: GermType, data: List<Map<String, String>>, lastUpdate: String, q: String) {
    drawInvalidateButton(lastUpdate, q)

    if (data.isEmpty()) {
        +i18n["page.caselist.isEmpty"]
        return
    }

    a(href = "list/csv?q=${q}", classes = "btn btn-secondary btn-sm mt-2 ms-2") {
        attributes["download"] = "mrereport-${germ.germtype}-export.csv"
        +"Download CSV"
    }

    val columnNames = data.first().keys
    table(classes = "table") {
        thead {
            tr(classes = "sticky-tr") {
                for (columnName in columnNames) {
                    th(scope = ThScope.col) {
                        +try {
                            i18n["page.${germ.germtype}.caselist.$columnName"]
                        } catch (_: Exception) {
                            columnName
                        }
                    }
                }
            }
        }
        for (datum in data) {
            tr {
                for (key in columnNames) {
                    td { +translate(germ, key, datum[key]) }
                }
            }
        }
    }
}

private fun translate(germ: GermType, columnName: String, value: String?): String {
    if (columnName in setOf("caseType", "infection", "nosocomial")) {
        val i18nSlug = "page.caselist.$columnName.$value"
        return if (i18n.containsKey(i18nSlug)) i18n[i18nSlug] else value ?: "null"
    }


    if (columnName == "sampleType" && i18n.locale.language != "de") {
        return sampleTranslations[value?.trim()] ?: run {
            println("Translation missing '$value'")
            value
        }
        ?: "null"
    }


    return value ?: "null"
}

val sampleTranslations: Map<String, String> by lazy {
    val `is` = object {}::class.java.classLoader.getResourceAsStream("view/translation.json")
    Json.decodeFromStream(`is`)
}

private fun FlowContent.drawInvalidateButton(lastUpdate: String, q: String) {
    div(classes = "btn-toolbar") {
        form(classes = "form-inline", action = "invalidate-cache", method = FormMethod.post) {
            label {
                title = lastUpdate
                +"${i18n["page.other.reportAge"]}: ${LocalDateTime.parse(lastUpdate).toDifferenceFromNow()}"
            }
            hiddenInput(name = "q") { value = q }
            button(type = ButtonType.submit, classes = "btn btn-light btn-sm") {
                +i18n["page.other.createNewReport"]
            }
        }
    }
}


fun FlowContent.drawOverviewTable(germ: GermType?, data: List<OverviewEntry>, lastUpdate: String, q: String) {
    drawInvalidateButton(lastUpdate, q)

    table(classes = "table") {
        for ((index, entry) in data.withIndex()) {
            tr {
                th {
                    span {
                        unsafe { +i18n[if (germ != null) "page.${germ.name}.overview.${entry.title}" else entry.title] }
                    }
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

    script("text/javascript", "/static/github-com-chartjs-Chart-js/Chart.min.js") {}
    div(classes = "container") {
        div(classes = "row") {
            for ((germ, data) in data) {
                div(classes = "col-3") {
                    style = "height: 400px;"
                    drawBarChart("${i18n["page.diagrams.numberOf"]} $germ", data.mapKeys { it.key.toString() })
                }
            }

        }
    }
}

fun FlowContent.drawYearSelector(cacheData: List<CacheData>, q: String?) {
    form(method = FormMethod.post, action = "/statistic/create") {
        id = "statistics-create"
        if (q != null) {
            hiddenInput(name = "q") { value = q }
        }
    }
    form(action = "/statistic") {
        if (q != null) {
            hiddenInput(name = "q") { value = q }
        }
        for (cache in cacheData) {
            val xQueryParams = cache.metadata.xQueryParams
            div(classes = "form-check mb-2") {
                checkBoxInput(classes = "form-check-input", name = "year[]") {
                    id = "q${xQueryParams.year}"
                    value = "${xQueryParams.year}"
//                    checked = xQueryParams.year in yearsEnabled
                }
                label(classes = "form-check-label") {
                    htmlFor = "q${xQueryParams.year}"
                    +xQueryParams.year.toString()
                    val missingGerms = GermType.entries.map { it.germtype } -
                            cache.germCache.filter { it.created != null }.map { it.type }
                    span(classes = "text-muted ms-1") {
                        +"${i18n["page.diagrams.reportsCreated"]}: "
                        +LocalDateTime.parse(cache.metadata.timeUpdated).toDifferenceFromNow()
                        if (missingGerms.isNotEmpty()) {
                            +", ${MessageFormat.format(i18n["page.diagrams.openReports"], missingGerms.joinToString())}"
                        }

                    }
                }
                button(classes = "btn btn-outline-danger btn-sm ms-2", type = ButtonType.submit) {
                    form = "deleteReportForm_${xQueryParams.year}"
                    +i18n["page.diagrams.delete"]
                }
            }
        }
        div(classes = "row pl-5") {
            div(classes = "col-3") {
                numberInput(classes = "form-control form-control-sm mb-2 mr-sm-2", name = "year") {
//                style = "max-width: 200px;"
                    min = "2000"
                    max = LocalDate.now().year.toString()
                    placeholder = i18n["settingspanel.year"]
                    required = true
                    form = "statistics-create"
                }
            }
            div(classes = "col-2") {
                button(type = ButtonType.submit, classes = "btn btn-light btn-sm mb-2") {
                    form = "statistics-create"
                    +i18n["page.diagrams.createReport"]
                }
            }
        }
        button(type = ButtonType.submit, classes = "btn btn-secondary mb-2") {
            +i18n["page.diagrams.createDiagrams"]
        }
    }

    for (cache in cacheData) {
        form(action = "/statistic/deleteReport", method = FormMethod.post) {
            id = "deleteReportForm_${cache.metadata.xQueryParams.year}"
            hiddenInput(name = "year") {
                value = cache.metadata.xQueryParams.year.toString()
            }
        }
    }


}


fun LocalDateTime.toDifferenceFromNow(): String {
    val now = LocalDateTime.now()
    val years = ChronoUnit.YEARS.between(this, now)
    if (years == 1L) {
        MessageFormat.format(i18n.getString("timeDifference.yearAgo"), years)
        return MessageFormat.format(i18n.getString("timeDifference.yearAgo"), years)
    } else if (years > 1) {
        return MessageFormat.format(i18n.getString("timeDifference.yearsAgo"), years)
    }
    val month = ChronoUnit.MONTHS.between(this, now)
    if (month == 1L) {
        return MessageFormat.format(i18n.getString("timeDifference.monthAgo"), month)
    } else if (month > 1) {
        return MessageFormat.format(i18n.getString("timeDifference.monthsAgo"), month)
    }
    val days = ChronoUnit.DAYS.between(this, now)
    if (days == 1L) {
        return MessageFormat.format(i18n.getString("timeDifference.dayAgo"), days)
    } else if (days > 1) {
        return MessageFormat.format(i18n.getString("timeDifference.daysAgo"), days)
    }
    val hours = ChronoUnit.HOURS.between(this, now)
    if (hours == 1L) {
        return MessageFormat.format(i18n.getString("timeDifference.hourAgo"), hours)
    } else if (hours > 1) {
        return MessageFormat.format(i18n.getString("timeDifference.hoursAgo"), hours)
    }
    val minutes = ChronoUnit.MINUTES.between(this, now)
    if (minutes == 1L) {
        return MessageFormat.format(i18n.getString("timeDifference.minuteAgo"), minutes)
    } else if (minutes > 1) {
        return MessageFormat.format(i18n.getString("timeDifference.minutesAgo"), minutes)
    }
    val seconds = ChronoUnit.SECONDS.between(this, now)
    if (seconds == 1L) {
        return MessageFormat.format(i18n.getString("timeDifference.secondAgo"), seconds)
    }

    return MessageFormat.format(i18n.getString("timeDifference.secondsAgo"), seconds)
}