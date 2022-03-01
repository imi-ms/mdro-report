package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.baseX.*
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.webjars.*
import kotlinx.html.*
import java.net.InetAddress


//TODO: Add remaining queries
//TODO: Test dynamic architecture
//TODO: Make header and footer "sticky"
class LayoutTemplate(private val url: String) : Template<HTML> {
    val header = Placeholder<FlowContent>()
    val content = TemplatePlaceholder<OverviewTemplate>()
    override fun HTML.apply() {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
            title { url }
            link(rel = "stylesheet", href = "/webjars/bootstrap/dist/css/bootstrap.min.css")
            link(rel = "stylesheet", href = "/static/custom-styles.css")
        }
        body {
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

            main(classes = "container") {
                role = "main"
                h1 {
                    insert(header)
                }
                insert(OverviewTemplate(), content)
            }


            footer(classes = "footer") {
                div(classes = "container") {
                    span(classes = "text-muted") {
                        +"© 2022 Copyright "
                    }
                    a(href = "https://imi.uni-muenster.de") {
                        +"Institut für Medizinische Informatik Münster"
                    }
                }
            }
            script(src = "/webjars/jquery/dist/jquery.min.js") {}
            //TODO: Import popper from webjars
            script(src = "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js") {}
            script(src = "/webjars/bootstrap/dist/js/bootstrap.min.js") {}
        }
    }
}

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


fun FlowContent.drawTable2(data: List<Pair<String, String>>) {
    table(classes = "table") {
        for (datum in data) {
            tr {
                th { +datum.first }
                td {
                    title = datum.first
                    +datum.second
                    unsafe { +"<i>ich bin kursiv</i>" }
                }
            }
        }
    }
}

abstract class OverviewEntry(val title: String, val tooltip: String, val data: String) {
}


class OverviewTemplate : Template<FlowContent> {
    val data = Placeholder<FlowContent>()

    override fun FlowContent.apply() {
        insert(data)
    }
}

class TableTemplate : Template<FlowContent> {
    val articleText = Placeholder<FlowContent>()
    override fun FlowContent.apply() {
        article {
            p {
                insert(articleText)
            }
        }
    }
}

class Server {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val webappPort = findOpenPortInRange(8080..8888)
            //TODO: Config via command line parameters
            val baseXClient = RestClient(
                baseURL = "https://basex.ukmuenster.de/rest",
                username = "oehm",
                password = "M2QWcX7tJsLBPic",
                database = "2021-copy3"
            )
            createServer(baseXClient, webappPort!!).start(wait = true)
        }
    }
}

fun Application.warEntrypoint() {
    val baseXClient = with(environment.config.config("BaseX")) {
        RestClient(
            baseURL = property("baseUrl").getString(),
            username = property("username").getString(),
            password = property("password").getString(),
            database = property("database").getString()
        )
    }
    application(baseXClient, serverMode = true)()
}

fun createServer(baseXClient: IBaseXClient, port: Int = 8080) =
    embeddedServer(Netty, host = "127.0.0.1", port = port, module = application(baseXClient))

private fun application(baseXClient: IBaseXClient, serverMode: Boolean = false): Application.() -> Unit =
    {
        install(Webjars)
        install(StatusPages) {
            status(HttpStatusCode.NotFound) {
                call.respondHtmlTemplate(
                    status = HttpStatusCode.NotFound,
                    template = LayoutTemplate(call.request.uri.removePrefix("/"))
                ) {
                    header { +"404 Not Found" }
                    content { data { +"No route defined for URL! Pleaes switch URL on top" } }
                }
            }
            exception<Throwable> { cause ->
                call.respondHtmlTemplate(
                    status = HttpStatusCode.InternalServerError,
                    template = LayoutTemplate(call.request.uri.removePrefix("/"))
                ) {
                    header { +"500 Internal Server Error" }
                    content { data { +"${cause.message}" } }
                }
            }
        }
        routing {
            //Protect against non-localhost calls
            if (!serverMode) {
                intercept(ApplicationCallPipeline.Features) {
                    val ip = InetAddress.getByName(call.request.local.remoteHost)
                    if (!(ip.isAnyLocalAddress || ip.isLoopbackAddress)) {
                        call.respondText(
                            "The request origin '$ip' is not a localhost address.",
                            status = HttpStatusCode.Unauthorized
                        )
                        this.finish()
                    }
                }
            }
            get("/") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"Willkommen" }
                    content {
                        data { +"Bitte nutzen Sie die Navigationsleiste oben, um zwischen den verschiedenen Funktionen zu navigieren!" }
                    }
                }
            }
            get("MRSA/overview") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRSA: Übersicht" }
                    content {
                        data {
                            drawTable2(
                                listOf(
                                    "stationäre Fälle gesamt pro Erfassungszeitraum" to "fallzahlen.xq",
                                    "stationäre Falltage gesamt pro Erfassungszeitraum" to "Falltage.xq",
                                    "Anzahl der Nasenabstriche bzw. kombinierte Nasen/Rachenabstiche pro Erfassungszeitraum " to "nasenabstriche.xq",
                                    "Anzahl aller S. aureus aus Blutkulturen (MSSA und MRSA)" to "mssa_bk.xq",
                                    "Anzahl MRSA aus Blutkulturen" to "mrsa_bk.xq",
                                    "Gesamtanzahl aller Fälle mit Methicillin Resistenten S. aureus (MRSA)" to "count(falliste)",
                                    "Anzahl der importierten MRSA Fälle" to "count(falliste)",
                                    "Anzahl nosokomialer MRSA Fälle" to "count(falliste)",
                                    "stationäre Falltage von MRSA-Fällen" to "fallzahlen.xq",
                                )
                            )
                        }
                    }
                }
            }

            get("MRSA/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getMRSA())
                val tableData = parseCsv(
                    text,
                    listOf(
                        "PID",
                        "Abnahmezeitpunkt",
                        "Probeart",
                        "Infektion",
                        "nosokomial?",
                        "Einsender",
                        "Einsender2",
                        "Spa",
                        "ClusterType"
                    )
                )
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRSA Fallliste" }
                    content {
                        data {
                            drawTable(tableData)
                        }
                    }
                }
            }
            get("MRGN/overview") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN Übersicht" }
                    content {
                        data {
                            drawTable2(
                                listOf(
                                    "stationäre Fälle gesamt pro Erfassungszeitraum" to "fallzahlen.xq",
                                    "stationäre Falltage gesamt pro Erfassungszeitraum" to "Falltage.xq",
                                    "Anzahl der 3MRGN Fälle" to "count(falliste)",
                                    "Anzahl der 4MRGN Fälle" to "count(falliste)",
                                )
                            )
                        }
                    }
                }
            }
            get("MRGN/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getMRGN())
                val tableData = parseCsv(
                    text,
                    listOf(
                        "PID",
                        "Abnahmezeitpunkt",
                        "Probenart",
                        "Einsender",
                        "?",
                        "Klasse",
                        "Piperacillin und Tazobactam Ergebnis",
                        "Cefotaxime Ergebnis",
                        "cefTAZidime Ergebnis",
                        "Cefepime Ergebnis",
                        "Meropenem Ergebnis",
                        "Imipenem Ergebnis",
                        "Ciprofloxacin Ergebnis"
                    )
                )
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN Fallliste" }
                    content {
                        data {
                            drawTable(tableData)
                        }
                    }
                }
            }
            get("VRE/overview") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"VRE Übersicht" }
                    content {
                        data {
                            drawTable2(
                                listOf(
                                    "stationäre Fälle gesamt pro Erfassungszeitraum" to "fallzahlen.xq",
                                    "stationäre Falltage gesamt pro Erfassungszeitraum" to "Falltage.xq",
                                    "Anzahl der gesamten E.faecalis Fälle (resistente und sensible)" to "anzahlEfaecalis.xq",
                                    "Anzahl der VRE E.faecalis Fälle" to "count(falliste) wo erreger = e.faecalis",
                                    "Anzahl der gesamten E.faecium Fälle (resistente und sensible)" to "count(falliste) wo erreger = e.faecalis",
                                    "Anzahl der VRE E.faecium Fälle" to "count(falliste) wo erreger = e.faecalis",
                                    "Anzahl sonstiger VRE Fälle" to "count(falliste) wo erreger != e.faecalis oder e.faecium",
                                    "Anzahl E.faecium Fälle (inkl. Vancomycin empfindliche und resistente Isolate) in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)" to "efaecium_bk.xq",
                                    "Anzahl der VRE-E.faecium Fälle in Blutkulturen (Angabe nur einer 1 Kultur pro Patient)" to "vre_bk.xq",
                                )
                            )
                        }
                    }
                }
            }
            get("VRE/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getVRE())
                val tableData = parseCsv(
                    text,
                    listOf(
                        "PID",
                        "Abnahmezeitpunkt",
                        "Probenart",
                        "Einsender",
                        "?",
                        "Linezolid Ergebnis",
                        "Tigecylin Ergebnis",
                        "Vancomycin Ergebnis",
                        "Teicoplanin Ergebnis",
                        "Quinupristin und Dalfopristin Ergebnis"
                    )
                )
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"VRE-ÖGD-Report" }
                    content {
                        data {
                            drawTable(tableData)
                        }
                    }
                }
            }
            get("/about") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"Über" }
                    content {
                        data {
                            +"Dies ist ein Proof-of-Concept zur automatischen Erstellung des ÖGD-Reports anhand der Integration von ORBIS, OPUS-L und SeqSphere in der internen BaseX-Zwischenschicht des Medics."
                        }
                    }
                }
            }

            static("/static") {
                resources()
            }
        }
    }

