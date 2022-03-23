package de.uni_muenster.imi.oegd.webapp


import de.uni_muenster.imi.oegd.common.GermType
import de.uni_muenster.imi.oegd.common.GlobalData
import de.uni_muenster.imi.oegd.common.IBaseXClient
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.webjars.*
import kotlinx.html.script
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.net.InetAddress
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger {  }

fun application(baseXClient: IBaseXClient, serverMode: Boolean = false): Application.() -> Unit {
    val xqueryparams = AttributeKey<XQueryParams>("XQueryParams")
    return {
        val cachingUtility = CachingUtility(baseXClient.getInfo())
        install(Webjars)
        install(StatusPages) {
            status(HttpStatusCode.NotFound) {
                call.respondHtmlTemplate(
                    status = HttpStatusCode.NotFound,
                    template = LayoutTemplate(call.request.uri, call.parameters["q"])
                ) {
                    header { +"404 Not Found" }
                    content { +"No route defined for URL: ${call.request.uri}" }
                }
            }
            exception<Throwable> { cause ->
                cause.printStackTrace()
                call.respondHtmlTemplate(
                    status = HttpStatusCode.InternalServerError,
                    template = LayoutTemplate(call.request.uri, call.parameters["q"])
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
            intercept(ApplicationCallPipeline.Features) {
                val params: XQueryParams? = call.parameters["q"]?.let { Json.decodeFromString(it) }
                if (params != null) {
                    call.attributes.put(xqueryparams, params)
                }
            }
            post("/settings/save") {
                val parameters = call.receiveParameters()
                val x = XQueryParams(parameters["year"]?.toInt())
                val q = Json.encodeToString(x)
                val referer = call.request.headers["Referer"]?.substringBefore("?")
                call.respondRedirect("$referer?q=$q")
            }
            intercept(ApplicationCallPipeline.Call) {
                if (call.request.uri.contains("settings/save")) return@intercept
                val s = call.parameters["q"]
                if (s.isNullOrBlank() || s == "null") {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, s)) {
                        header { +"Willkommen" }
                        content {
                            drawIndex()
                            script(type = "application/javascript") {
                                +"$(function() { $('#settings-modal').modal({focus:true}) });"
                            }
                        }
                    }
                    this.finish()
                }
            }

            get("/") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Willkommen" }
                    content {
                        drawIndex()
                    }
                }
            }
            post("{germ}/overview/invalidate-cache") {
                val value = call.parameters["germ"]!!
                val xQueryParams = call.attributes[xqueryparams]
                if (value == "global") {
                    cachingUtility.clearGlobalInfoCache(xQueryParams)
                } else {
                    val germ = GermType.valueOf(value)
                    cachingUtility.clearOverviewCache(xQueryParams, germ)
                }
                call.respondRedirect(call.request.headers["Referer"] ?: ("/$value/overview?q=" + call.parameters["q"]))
            }
            post("{germ}/list/invalidate-cache") {
                val germ = GermType.valueOf(call.parameters["germ"]!!)
                val xQueryParams = call.attributes[xqueryparams]
                cachingUtility.clearCaseListCache(xQueryParams, germ)
                call.respondRedirect(call.request.headers["Referer"] ?: ("/$germ/overview?q=" + call.parameters["q"]))
            }

            post("/settings/uploadCache") {
                uploadCache(call.receiveMultipart(), cachingUtility)
                call.respondRedirect("/")
            }
            get("/settings/downloadCache") {
                val xQueryParams = call.attributes[xqueryparams]

                cachingUtility.getGlobalInfo(xQueryParams)
                for (germType in GermType.values()) {
                    cachingUtility.getCaseList(xQueryParams, germType, baseXClient)
                    cachingUtility.getOverviewEntries(xQueryParams, germType, baseXClient)
                }
                call.response.header(
                    HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName, cachingUtility.getCacheFileName(xQueryParams)
                    ).toString()
                )
                call.respondText(
                    Json.encodeToString(cachingUtility.getCache(xQueryParams)),
                    contentType = ContentType.Application.Json
                )
            }
            get("global/overview") {
                val xQueryParams = call.attributes[xqueryparams]
                val (overviewContent, lastUpdate) = cachingUtility.getGlobalInfo(xQueryParams, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Globale Statistiken" }
                    content { drawOverviewTable(overviewContent, lastUpdate) }
                }
            }
            for (germ in GermType.values()) {
                get("$germ/overview") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val (overviewContent, lastUpdate) = cachingUtility.getOverviewEntries(
                        xQueryParams,
                        germ,
                        baseXClient
                    )
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"MRSA: Übersicht" }
                        content { drawOverviewTable(overviewContent, lastUpdate) }
                    }
                }
                get("$germ/list") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val (tableData, lastUpdate) = cachingUtility.getCaseList(xQueryParams, germ, baseXClient)
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"MRSA: Fallliste" }
                        content { drawCaseList(tableData, lastUpdate) }
                    }
                }
            }

            get("/statistic") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Statistik" }
                    content { +"Upload files or select stored cache files:" }
                }
            }
            get("/about") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
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
}

private suspend fun CachingUtility.getCaseList(
    xQueryParams: XQueryParams,
    germ: GermType,
    baseXClient: IBaseXClient
): Pair<List<Map<String, String>>, String> {
    if (this.getGermForGermtype(xQueryParams, germ)?.caseListTimeCreated == null) {
        val tableData = WebappComponents.getCaseList(baseXClient, germ)
        this.cache(xQueryParams, germ, tableData)
    }
    val (_, _, _, caseList, lastUpdate) = this.getGermForGermtype(xQueryParams, germ)!!

    return caseList!! to lastUpdate!!
}

private suspend fun CachingUtility.getOverviewEntries(
    xQueryParams: XQueryParams,
    germ: GermType,
    baseXClient: IBaseXClient
): Pair<List<OverviewEntry>, String> {
    if (this.getGermForGermtype(xQueryParams, germ)?.overviewTimeCreated == null) {
        val overviewContent = WebappComponents.getOverview(baseXClient, germ)
        this.cache(xQueryParams, germ, overviewContent)
    }
    val (_, overviewEntries, lastUpdate, _, _) = this.getGermForGermtype(xQueryParams, germ)!!

    return overviewEntries!! to lastUpdate!!
}

private suspend fun CachingUtility.getGlobalInfo(
    xQueryParams: XQueryParams,
    baseXClient: IBaseXClient,
): Pair<List<OverviewEntry>, String> {
    if (this.getGlobalInfo(xQueryParams)?.overviewTimeCreated == null) {
        val overviewContent = WebappComponents.getGlobalStatistics(baseXClient)
        this.cache(xQueryParams, overviewContent)
    }
    val (overviewEntries, lastUpdate) = this.getGlobalInfo(xQueryParams)!!

    return overviewEntries!! to lastUpdate!!
}

private fun updateGlobalData(parameters: Parameters) {
    GlobalData.year = parameters["year"].toString()
    log.info("User updated settings. New parameters: database - ${GlobalData.database} | year - ${GlobalData.year}")
}

private suspend fun uploadCache(multipartdata: MultiPartData, cachingUtility: CachingUtility) {
    var fileBytes: ByteArray? = null
    multipartdata.forEachPart { part ->
        if (part is PartData.FileItem) {
            fileBytes = part.streamProvider().readBytes()
        }
    }
    if (fileBytes != null) {
        val newCache = String(fileBytes!!, StandardCharsets.UTF_8)
        cachingUtility.uploadExistingCache(newCache)
    }
}

