package view

import de.uni_muenster.imi.oegd.webapp.i18n
import kotlinx.html.*
import model.OverviewEntry
import model.XQueryParams
import java.time.LocalDate

fun FlowContent.drawInfoModal(index: Int, entry: OverviewEntry) {
    button(classes = "btn btn-link text-muted") {
        attributes["data-bs-toggle"] = "modal"
        attributes["data-bs-target"] = "#query-modal-$index"
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
                        +"${i18n.getString(entry.title)} - Query"
                    }
                    button(classes = "btn-close", type = ButtonType.button) {
                        attributes["data-bs-dismiss"] = "modal"
                        attributes["aria-label"] = "close"
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
    val q_ = XQueryParams.fromJson(q)
    a(classes = "navbar-text") {
        attributes["data-bs-toggle"] = "modal"
        attributes["data-bs-target"] = "#settings-modal"
        if (q_ != null) {
            span(classes = "text-muted") { +"${i18n.getString("settingspanel.year")}: " }
            span(classes = "font-weight-bold") { +q_.year.toString() }
        }
    }

    button(classes = "btn") {
        attributes["data-bs-toggle"] = "modal"
        attributes["data-bs-target"] = "#settings-modal"
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
                div(classes = "modal-header d-flex") {
                    h5(classes = "modal-title") {
                        id = "settings-title"
                        +i18n.getString("settingspanel.heading")
                    }
                    button(classes = "ms-auto btn-close") {
                        attributes["data-bs-dismiss"] = "modal"
                        attributes["aria-label"] = "close"
                    }
                }
                div(classes = "modal-body") {
                    form(action = "/settings/save", method = FormMethod.post) {
                        div(classes = "form-group mb-3") {
                            label {
                                attributes["for"] = "inputYear"
                                +i18n.getString("settingspanel.year")
                            }
                            numberInput(name = "year", classes = "form-control") {
                                id = "inputYear"
                                min = "2000"
                                max = LocalDate.now().year.toString()
                                value = q_?.year?.toString() ?: ""
                            }
                        }
                        div(classes = "form-group mb-3") {
                            button(classes = "btn btn-light", type = ButtonType.button) {
                                attributes["data-bs-dismiss"] = "modal"
                                +i18n.getString("settingspanel.buttons.abort")
                            }
                            button(type = ButtonType.submit, classes = "btn btn-secondary ms-2") {
                                +i18n.getString("settingspanel.buttons.saveChanges")
                            }
                        }
                    }
                    hr {}
                    form(
                        action = "/settings/uploadCache",
                        method = FormMethod.post,
                        encType = FormEncType.multipartFormData
                    ) {
                        div(classes = "form-group mb-3") {
                            label {
                                htmlFor = "inputCache"
                                +i18n.getString("settingspanel.uploadReport")
                            }
                            fileInput(classes = "form-control mt-2", name = "uploadedCache") {
                                id = "inputCache"
                            }
                        }
                        div(classes = "form-group mb-3") {
                            button(type = ButtonType.submit, classes = "btn btn-secondary mt-2") {
                                +i18n.getString("settingspanel.buttons.uploadReport")
                            }
                            a(href = "/settings/downloadCache?q=$q", classes = "btn btn-secondary mt-2 ms-2") {
                                +i18n.getString("settingspanel.buttons.downloadReport")
                            }
                        }
                    }
                }
            }
        }
    }
}
