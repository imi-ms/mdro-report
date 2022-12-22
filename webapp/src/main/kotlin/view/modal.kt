package view

import kotlinx.html.*
import model.OverviewEntry
import model.XQueryParams
import java.time.LocalDate

fun FlowContent.drawInfoModal(index: Int, entry: OverviewEntry) {
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
                        +(entry.query + "\n\n")
                    }
                }
            }
        }
    }
}

fun FlowContent.drawSettingsModal(q: String?) {
    val q2 = XQueryParams.fromJson(q)
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
                    form(
                        action = "/settings/uploadCache",
                        method = FormMethod.post,
                        encType = FormEncType.multipartFormData
                    ) {
                        div(classes = "form-group") {
                            label {
                                attributes["for"] = "inputCache"
                                +"Report-Datei hochladen"
                            }
                            input(type = InputType.file, classes = "form-control-file") {
                                id = "inputCache"
                                name = "uploadedCache"
                            }
                        }
                        div(classes = "form-group") {
                            button(type = ButtonType.submit, classes = "btn btn-secondary mt-2") {
                                +"Report hochladen"
                            }
                            a(href = "/settings/downloadCache?q=$q", classes = "btn btn-secondary mt-2 ml-2") {
                                +"Report herunterladen"
                            }
                        }
                    }
                }
            }
        }
    }
}
