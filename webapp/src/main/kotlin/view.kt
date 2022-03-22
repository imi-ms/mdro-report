package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.common.GlobalData
import io.ktor.client.request.forms.*
import io.ktor.html.*
import kotlinx.html.*


class LayoutTemplate(private val url: String) : Template<HTML> {
    val header = Placeholder<FlowContent>()
    val content = Placeholder<FlowContent>()
    override fun HTML.apply() {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
            title { url }
            link(rel = "stylesheet", href = "/webjars/bootstrap/dist/css/bootstrap.min.css")
            link(rel = "stylesheet", href = "/webjars/bootstrap-icons/font/bootstrap-icons.css")
            link(rel = "stylesheet", href = "/static/custom-styles.css")

            script(src = "/webjars/jquery/dist/jquery.min.js") {}
            script(src = "/webjars/bootstrap/dist/js/bootstrap.min.js") {}

        }
        body {
            div(classes = "wrapper") {
                nav(classes = "navbar navbar-expand-md navbar-light bg-light") {
                    a(classes = "navbar-brand", href = "/") {
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
                            for (germ in sequenceOf("MRSA", "MRGN", "VRE")) {
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
                                        a(classes = "dropdown-item", href = "/$germ/overview") { +"Übersicht $germ" }
                                        a(classes = "dropdown-item", href = "/$germ/list") { +"Fallliste" }
                                    }
                                }
                            }
                            navItem("statistic", "Statistik")
                            navItem("about", "Über")
                        }
                    }
                    div(classes = "navbar float-left"){
                        ul(classes = "navbar-nav"){
                            drawSettingsModal()
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
            if (url.startsWith("$href")) {
                classes += "active"
            }
            a(classes = "nav-link", href = "/$href") {
                +label
            }
        }
    }
}

fun FlowContent.drawIndex() {
    div(classes = "mb-5") {
        +"Bitte nutzen Sie die Navigationsleiste oben, um zwischen den verschiedenen Funktionen zu navigieren!"
    }

    div {
        table(classes = "table") {
            tr{
                td {
                    attributes["colspan"] = "2"
                    +"Aktuelle Einstellungen"
                }
            }
            tr {
                td { +"Nutzer: " }
                td { +GlobalData.user }
            }
            tr {
                td { +"URL: " }
                td { +GlobalData.url }
            }
            tr {
                td { +"Datenbank: " }
                td { +GlobalData.database }
            }
            tr {
                td { +"Jahr: " }
                td { +GlobalData.year }
            }
        }
    }
}


fun FlowContent.drawCaseList(data: List<Map<String, String>>, lastUpdate: String) {
    div(classes = "btn-toolbar") {
        span {
            +"Bericht erstellt: $lastUpdate"
        }
        form(action = "list/invalidate-cache", method = FormMethod.post) {
            button(type = ButtonType.submit, classes = "btn btn-light btn-sm") {
                +"Neu erstellen"
            }
        }
    }


    if (data.isEmpty()) {
        +"Falliste ist leer"
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


fun FlowContent.drawOverviewTable(data: List<OverviewEntry>, lastUpdate: String = "foo") {
    span {
        +"Bericht erstellt: $lastUpdate"
    }
    form(action = "overview/invalidate-cache", method = FormMethod.post) {
        button(type = ButtonType.submit, classes = "btn btn-light btn-sm") {
            +"Neu erstellen"
        }
    }

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

private fun FlowContent.drawSettingsModal() {
    button(classes="btn") {
        attributes["data-toggle"] = "modal"
        attributes["data-target"] = "#settings-modal"
        i(classes="bi bi-gear-fill") {  }
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
                            input(type = InputType.text) {
                                id = "inputYear"
                                classes = setOf("form-control")
                                name = "year"
                                value = GlobalData.year
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
                            a(href = "/settings/downloadCache", classes = "btn btn-secondary mt-2 ml-2") {
                                +"Aktuellen Cache herunterladen"
                            }
                        }
                    }
                }
            }
        }
    }
}
