package de.uni_muenster.imi.oegd.webapp

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
                        +"MD-Report"
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

                            li(classes = "nav-item") {
                                if (url.startsWith("about")) {
                                    classes += "active"
                                }
                                a(classes = "nav-link", href = "/about") {
                                    +"Über"
                                }
                            }
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
}


fun FlowContent.drawCaseList(data: List<Map<String, String>>) {
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


fun FlowContent.drawOverviewTable(data: List<OverviewEntry>) {
    table(classes = "table") {
        data.forEachIndexed { index, entry ->
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

private fun FlowContent.drawInfoModal(
    index: Int,
    entry: OverviewEntry
) {
    button(classes = "btn btn-link text-muted") {
        attributes["data-toggle"] = "modal"
        attributes["data-target"] = "#query-modal-$index"
        i {
            attributes["class"] = "bi bi-info-circle"
        }
    }
    div(classes = "modal fade") {
        attributes["id"] = "query-modal-$index"
        attributes["tabindex"] = "-1"
        attributes["role"] = "dialog"
        attributes["aria-labelledby"] = "#query-modal-$index-title"
        attributes["aria-hidden"] = "true"
        div(classes = "modal-dialog modal-lg") {
            attributes["role"] = "document"
            div(classes = "modal-content") {
                div(classes = "modal-header") {
                    h5(classes = "modal-title") {
                        attributes["id"] = "query-modal-$index-title"
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
                div(classes = "modal-body") {
                    pre {
                        +entry.query
                    }
                }
            }
        }
    }
}
