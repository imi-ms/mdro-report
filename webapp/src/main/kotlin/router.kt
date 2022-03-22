package de.uni_muenster.imi.oegd.webapp


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


fun application(baseXClient: IBaseXClient, serverMode: Boolean = false): Application.() -> Unit =
    {
        val cachingUtility = CachingUtility(baseXClient.getInfo())
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
                val (overviewContent, lastUpdate) = cachingUtility.getOverviewEntries(GermType.MRSA, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRSA: Übersicht" }
                    content { drawOverviewTable(overviewContent, lastUpdate) }
                }
            }
            get("MRSA/list") {
                val (tableData, lastUpdate) = cachingUtility.getCaseList(GermType.MRSA, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRSA: Fallliste" }
                    content { drawCaseList(tableData, lastUpdate) }
                }
            }
            get("MRGN/overview") {
                val (overviewEntries, lastUpdate) = cachingUtility.getOverviewEntries(GermType.MRGN, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN: Übersicht" }
                    content { drawOverviewTable(overviewEntries, lastUpdate) }
                }
            }
            get("MRGN/list") {
                val (caseList, lastUpdate) = cachingUtility.getCaseList(GermType.MRGN, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"MRGN: Fallliste" }
                    content { drawCaseList(caseList, lastUpdate) }
                }
            }
            get("VRE/overview") {
                val (overviewContent, lastUpdate) = cachingUtility.getOverviewEntries(GermType.VRE, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"VRE: Übersicht" }
                    content { drawOverviewTable(overviewContent, lastUpdate) }
                }
            }
            get("VRE/list") {
                val (tableData, lastUpdate) = cachingUtility.getCaseList(GermType.VRE, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri.removePrefix("/"))) {
                    header { +"VRE: Fallliste" }
                    content { drawCaseList(tableData, lastUpdate) }
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

private suspend fun CachingUtility.getCaseList(
    germ: GermType,
    baseXClient: IBaseXClient
): Pair<List<Map<String, String>>, String> {
    if (this.getGermForGermtype(germ)?.caseListTimeCreated == null) {
        val tableData = WebappComponents.getCaseList(baseXClient, germ)
        this.cache(germ, tableData)
    }
    val (_, _, _, caseList, lastUpdate) = this.getGermForGermtype(germ)!!

    return caseList!! to lastUpdate!!
}

private suspend fun CachingUtility.getOverviewEntries(
    germ: GermType,
    baseXClient: IBaseXClient
): Pair<List<OverviewEntry>, String> {
    if (this.getGermForGermtype(germ)?.overviewTimeCreated == null) {
        val overviewContent = WebappComponents.getOverview(baseXClient, germ)
        this.cache(germ, overviewContent)
    }
    val (_, overviewEntries, lastUpdate, _, _) = this.getGermForGermtype(germ)!!

    return overviewEntries!! to lastUpdate!!
}