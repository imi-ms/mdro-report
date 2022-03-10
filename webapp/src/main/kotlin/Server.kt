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


//TODO: Test dynamic architecture
class LayoutTemplate(private val url: String) : Template<HTML> {
    val header = Placeholder<FlowContent>()
    val content = TemplatePlaceholder<OverviewTemplate>()
    override fun HTML.apply() {
        head {
            meta(charset = "UTF-8")
            meta(name = "viewport", content = "width=device-width, initial-scale=1, shrink-to-fit=no")
            title { url }
            link(rel = "stylesheet", href = "/webjars/bootstrap/dist/css/bootstrap.min.css")
            link(rel = "stylesheet", href = "/webjars/bootstrap-icons/font/bootstrap-icons.css")
            link(rel = "stylesheet", href = "/static/custom-styles.css")

            script(src = "/webjars/jquery/dist/jquery.min.js") {}
            //script(src = "/webjars/popper.js/dist/popper.min.js") {} //TODO: Popper needed?
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
                        insert(OverviewTemplate(), content)
                    }
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
            }
        }
    }
}

fun FlowContent.drawTable(data: List<Map<String, String>>) {
    val keys = data.first().keys
    table(classes = "table") {
        thead {
            tr(classes = "sticky-tr"){
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


fun FlowContent.drawTable2(data: List<OverviewEntry>) {
    table(classes = "table") {
        data.forEachIndexed { index, entry ->
            tr {
                th { +entry.title }
                td {
                    span{+entry.data}
                    button(classes = "btn btn-link") {
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
                                    button(classes = "close") {
                                        attributes["type"] = "button"
                                        attributes["data-dismiss"] = "modal"
                                        attributes["aria-label"] = "close"
                                        span {
                                            attributes["aria-hidden"] = "true"
                                            +"×"
                                        }
                                    }
                                }
                                div(classes = "modal-body") {
                                    +entry.query
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

abstract class OverviewEntry(val title: String, val query: String, val data: String) {}


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
            val url: String
            val username: String
            val password: String
            val database: String
            if(args.isNotEmpty()) {
                url = args[0]
                username = args[1]
                password = args[2]
                database = args[3]
            } else {
                println("Bitte gib eine BaseX URL an: ")
                url = readLine()!!
                println("Bitte gib deinen Usernamen an: ")
                username = readLine()!!
                password = System.console()?.readPassword("Bitte gib das Passwort ein: \n")!!.concatToString()
                println("Bitte gib die Datenbank an: ")
                database = readLine()!!
            }

            val baseXClient = RestClient(
                baseURL = url,
                username = username,
                password = password,
                database = database
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
                val overviewContent = WebappComponents.getMRSAOverview(baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRSA: Übersicht" }
                    content {
                        data {
                            drawTable2(overviewContent)
                        }
                    }
                }
            }
            get("MRSA/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getMRSA())
                val tableData = WebappComponents.getMRSACSV(text)
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
                val overviewContent = WebappComponents.getMRGNOverview(baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN Übersicht" }
                    content {
                        data {
                            drawTable2(overviewContent)
                        }
                    }
                }
            }
            get("MRGN/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getMRGN())
                val tableData = WebappComponents.getMRGACSV(text)
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
                val overviewContent = WebappComponents.getVREOverview(baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"VRE Übersicht" }
                    content {
                        data {
                            drawTable2(overviewContent)
                        }
                    }
                }
            }
            get("VRE/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getVRE())
                val tableData = WebappComponents.getVRECSV(text)
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

