package de.uni_muenster.imi.oegd.webapp


import de.uni_muenster.imi.oegd.common.BaseXQueries
import de.uni_muenster.imi.oegd.common.GermType
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.InetAddress

val cachingUtility = CachingUtility()

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
                    content { +"No route defined for URL: ${call.request.uri}" }
                }
            }
            exception<Throwable> { cause ->
                cause.printStackTrace()
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
            post("{germ}/overview/invalidate-cache") {
                val germ = GermType.valueOf(call.parameters["germ"]!!)
                cachingUtility.clearOverviewCache(germ)
                call.respondRedirect("/$germ/overview")
            }
            post("{germ}/list/invalidate-cache") {
                val germ = GermType.valueOf(call.parameters["germ"]!!)
                cachingUtility.clearCaseListCache(germ)
                call.respondRedirect("/$germ/list")
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
                    content { drawCaseList(tableData, "Please Fix!") }
                }
            }
            get("MRGN/overview") {
                val (overviewEntries, lastUpdate) = getOverviewEntries(GermType.MRGN, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN: Übersicht" }
                    content { drawOverviewTable(overviewEntries, lastUpdate) }
                }
            }
            get("MRGN/list") {
                val (caseList, lastUpdate) = getCaseList(GermType.MRGN, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN: Fallliste" }
                    content { drawCaseList(caseList, lastUpdate) }
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
                    content { drawCaseList(tableData, "foo") }
                }
            }
            get("/statistic") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"Statistik" }
                    content { +"Upload files or select stored cache files:" }
                }
            }
            get("/download") {
                //TODO: Cache everything
                call.response.header(
                    HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName, "foo.mdreport" //TODO: Use database / year name
                    ).toString()
                )
                call.respondText(
                    Json.encodeToString(cachingUtility.getCache()),
                    contentType = ContentType.Application.Json
                )
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

private suspend fun getCaseList(
    germ: GermType,
    baseXClient: IBaseXClient
): Pair<List<Map<String, String>>, String> {
    if (cachingUtility.getGermForGermtype(germ)?.caseListTimeCreated == null) {
        val text = baseXClient.executeXQuery(BaseXQueries.getMRGN()) //TODO: Edit here
        val tableData = WebappComponents.getMRGACSV(text) //TODO: Edit here
        cachingUtility.cache(germ, tableData)
    }
    val (_, _, _, caseList, lastUpdate) = cachingUtility.getGermForGermtype(germ)!!

    return caseList!! to lastUpdate!!
}

private suspend fun getOverviewEntries(
    germ: GermType,
    baseXClient: IBaseXClient
): Pair<List<OverviewEntry>, String> {
    if (cachingUtility.getGermForGermtype(germ)?.overviewTimeCreated == null) {
        val overviewContent = WebappComponents.getMRGNOverview(baseXClient) //TODO: Edit here
        cachingUtility.cache(germ, overviewContent)
    }
    val (_, overviewEntries, lastUpdate, _, _) = cachingUtility.getGermForGermtype(germ)!!

    return overviewEntries!! to lastUpdate!!
}