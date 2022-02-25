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
            title {
                url
            }
            link(rel = "stylesheet", href = "/webjars/bootstrap/dist/css/bootstrap.min.css")
            link(rel = "stylesheet", href = "/static/custom-styles.css")
        }
        body {
            nav(classes = "navbar navbar-expand-md navbar-light bg-light") {
                a(classes = "navbar-brand", href = "/") {
                    +"ÖGD-Tool"
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
            script(src = "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.7/umd/popper.min.js") {}
            script(src = "/webjars/bootstrap/dist/js/bootstrap.min.js") {}
        }
    }
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
            val server = createServer(baseXClient, webappPort!!).start(wait = true)
        }
    }
}

val baseXClient = RestClient(
    baseURL = "https://basex.ukmuenster.de/rest",
    username = "oehm",
    password = "M2QWcX7tJsLBPic",
    database = "2021-copy3"
)

fun createServer(baseXClient: IBaseXClient, port: Int = 8080) = embeddedServer(Netty, host = "127.0.0.1", port = port) {
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
        exception<Throwable>() { cause ->
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
        intercept(ApplicationCallPipeline.Features) {
            val ip = InetAddress.getByName(call.request.local.remoteHost)
            if (!(ip.isAnyLocalAddress || ip.isLoopbackAddress)) {
                call.respondText("The request origin '$ip' is not a localhost address.")
                this.finish()
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
                header { +"MRSA-ÖGD-Report" }
                content {
                    data {
                        drawTable(tableData) //TODO: Fix classpath error for drawTable
                    }
                }
            }
        }
        get("MRGN/list") {
            val text = baseXClient.executeXQuery(BaseXQueries.getVRE())
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
                header { +"MRGN-ÖGD-Report" }
                content {
                    data {
                        drawTable(tableData)
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

