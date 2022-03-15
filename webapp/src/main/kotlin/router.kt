package de.uni_muenster.imi.oegd.webapp


import de.uni_muenster.imi.oegd.common.BaseXQueries
import de.uni_muenster.imi.oegd.common.IBaseXClient
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.webjars.*
import java.net.InetAddress

fun application(baseXClient: IBaseXClient, serverMode: Boolean = false): Application.() -> Unit =
    {
        install(Webjars)
        install(StatusPages) {
            status(HttpStatusCode.NotFound) {
                call.respondHtmlTemplate(
                    status = HttpStatusCode.NotFound,
                    template = LayoutTemplate(call.request.uri.removePrefix("/"))
                ) {
                    header { +"404 Not Found" }
                    content { +"No route defined for URL!" }
                }
            }
            exception<Throwable> { cause ->
                call.respondHtmlTemplate(
                    status = HttpStatusCode.InternalServerError,
                    template = LayoutTemplate(call.request.uri.removePrefix("/"))
                ) {
                    header { +"500 Internal Server Error" }
                    content { +"${cause.message}" }
                }
            }
        }
        routing {
            //Protect against non-localhost calls, avoid leaking data to unauthorized persons
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
                        +"Bitte nutzen Sie die Navigationsleiste oben, um zwischen den verschiedenen Funktionen zu navigieren!"
                    }
                }
            }
            get("MRSA/overview") {
                val overviewContent = WebappComponents.getMRSAOverview(baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRSA: Übersicht" }
                    content { drawOverviewTable(overviewContent) }
                }
            }
            get("MRSA/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getMRSA())
                val tableData = WebappComponents.getMRSACSV(text)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRSA: Fallliste" }
                    content { drawCaseList(tableData) }
                }
            }
            get("MRGN/overview") {
                val overviewContent = WebappComponents.getMRGNOverview(baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN: Übersicht" }
                    content { drawOverviewTable(overviewContent) }
                }
            }
            get("MRGN/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getMRGN())
                val tableData = WebappComponents.getMRGACSV(text)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN: Fallliste" }
                    content { drawCaseList(tableData) }
                }
            }
            get("VRE/overview") {
                val overviewContent = WebappComponents.getVREOverview(baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"VRE: Übersicht" }
                    content { drawOverviewTable(overviewContent) }
                }
            }
            get("VRE/list") {
                val text = baseXClient.executeXQuery(BaseXQueries.getVRE())
                val tableData = WebappComponents.getVRECSV(text)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"VRE: Fallliste" }
                    content { drawCaseList(tableData) }
                }
            }
            get("/about") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"Über" }
                    content {
                        +"Dies ist ein Proof-of-Concept zur automatischen Erstellung des ÖGD-Reports anhand der Integration von ORBIS, OPUS-L und SeqSphere in der internen BaseX-Zwischenschicht des Medics."
                    }
                }
            }

            static("/static") {
                resources()
            }
        }
        environment.monitor.subscribe(ApplicationStopping) {
            baseXClient.close()
        }
    }