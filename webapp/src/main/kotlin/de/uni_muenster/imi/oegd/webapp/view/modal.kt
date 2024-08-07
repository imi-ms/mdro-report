package de.uni_muenster.imi.oegd.webapp.view

import de.uni_muenster.imi.oegd.webapp.get
import de.uni_muenster.imi.oegd.webapp.i18n
import de.uni_muenster.imi.oegd.webapp.model.CaseType
import de.uni_muenster.imi.oegd.webapp.model.OverviewEntry
import de.uni_muenster.imi.oegd.webapp.model.Params
import kotlinx.html.*
import kotlinx.html.ButtonType.button
import kotlinx.html.ButtonType.submit
import kotlinx.html.FormMethod.post
import java.time.LocalDate
import java.util.*
import kotlin.collections.set

fun FlowContent.drawInfoModal(index: Int, entry: OverviewEntry) {
    button(classes = "btn btn-link text-muted") {
        attributes["data-bs-toggle"] = "modal"
        attributes["data-bs-target"] = "#query-modal-$index"
        i(classes = "bi bi-info-circle") {}
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
                        +"XQuery"
                    }
                    button(classes = "btn-close", type = button) {
                        attributes["data-bs-dismiss"] = "modal"
                        attributes["aria-label"] = "close"
                    }
                }
                div(classes = "modal-body modal-query") {
                    pre { +(entry.query + "\n\n") }
                }
            }
        }
    }
}

fun FlowContent.drawSettingsModal(q: String?) {
    val q_ = Params.fromJson(q)
    form(method = post, action = "/changeLanguage") {
        select(classes = "form-select languageSelect navbar-text text-muted") {
            id = "languageSelect"
            onChange = "this.form.submit();"
            name = "language"

            option {
                selected = i18n.locale == Locale.GERMAN
                value = "de"
                +i18n.getString("settingspanel.language.german")
            }
            option {
                selected = i18n.locale == Locale.ENGLISH
                value = "en"
                +i18n.getString("settingspanel.language.english")
            }
        }
        hiddenInput(name = "q") { value = q ?: "null" }
    }

    a(classes = "navbar-text") {
        style = "text-decoration: none"
        attributes["data-bs-toggle"] = "modal"
        attributes["data-bs-target"] = "#settings-modal"
        if (q_ != null) {
            span(classes = "") { +"${i18n["settingspanel.year"]}: " }
            span {
                style = "color:black;font-weight:bold;"
                +q_.xquery.year.toString()
            }
            span(classes = "") { +" Falltyp: " }
            span {
                style = "color:black;font-weight:bold;"
                +q_.filter.caseTypes.joinToString()
            }
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
        div(classes = "modal-dialog modal-dialog-centered modal-md") {
            attributes["role"] = "document"
            div(classes = "modal-content") {
                div(classes = "modal-header d-flex") {
                    h5(classes = "modal-title") {
                        id = "settings-title"
                        +i18n["settingspanel.heading"]
                    }
                    button(classes = "ms-auto btn-close") {
                        attributes["data-bs-dismiss"] = "modal"
                        attributes["aria-label"] = "close"
                    }
                }
                div(classes = "modal-body") {
                    form(action = "/settings/save", method = post) {
                        div(classes = "form-group mb-3") {
                            label {
                                htmlFor = "inputYear"
                                +i18n["settingspanel.year"]
                            }
                            numberInput(name = "year", classes = "form-control") {
                                id = "inputYear"
                                min = "2000"
                                max = LocalDate.now().year.toString()
                                value = q_?.xquery?.year?.toString() ?: ""
                            }
                        }
                        div(classes = "form-group mb-3") {
                            for (caseType in CaseType.entries) {
                                div(classes = "form-check form-check-inline") {
                                    checkBoxInput(classes = "form-check-input") {
                                        id = "chk$caseType"
                                        value = "$caseType"
                                        name = "caseTypes"
                                        checked = caseType in (q_?.filter?.caseTypes ?: emptyList())
                                    }
                                    label(classes = "form-check-label") {
                                        htmlFor = "chk$caseType"
                                        +i18n["settingspanel.casetype." + caseType.toString().lowercase()]
                                    }
                                }
                            }
                        }
                        div(classes = "form-group mb-3") {
                            button(classes = "btn btn-light", type = button) {
                                attributes["data-bs-dismiss"] = "modal"
                                +i18n["settingspanel.buttons.abort"]
                            }
                            button(type = submit, classes = "btn btn-secondary ms-2") {
                                +i18n["settingspanel.buttons.saveChanges"]
                            }
                        }
                    }
                    /*hr {}
                    form(action = "/settings/uploadCache", method = post, encType = multipartFormData) {
                        div(classes = "form-group mb-3") {
                            label {
                                htmlFor = "inputCache"
                                +i18n["settingspanel.uploadReport"]
                            }
                            fileInput(classes = "form-control mt-2", name = "uploadedCache") {
                                id = "inputCache"
                            }
                        }
                        div(classes = "form-group mb-3") {
                            button(type = submit, classes = "btn btn-secondary mt-2") {
                                +i18n["settingspanel.buttons.uploadReport"]
                            }
                            a(href = "/settings/downloadCache?q=$q", classes = "btn btn-secondary mt-2 ms-2") {
                                +i18n["settingspanel.buttons.downloadReport"]
                            }
                        }
                    }*/
                }
            }
        }
    }
}
