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
                            drawIndex(baseXClient.getInfo())
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
                        drawIndex(baseXClient.getInfo())
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
                    cachingUtility.clearGermInfo(xQueryParams, germ)
                }
                call.respondRedirect(call.request.headers["Referer"] ?: ("/$value/overview?q=" + call.parameters["q"]))
            }
            post("{germ}/list/invalidate-cache") {
                val germ = GermType.valueOf(call.parameters["germ"]!!)
                val xQueryParams = call.attributes[xqueryparams]
                cachingUtility.clearGermInfo(xQueryParams, germ)
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
                    cachingUtility.getOrLoadGermInfo(xQueryParams, germType, baseXClient)
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
                val (overviewContent, lastUpdate) = cachingUtility.getOrLoadGlobalInfo(xQueryParams, baseXClient)
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Globale Statistiken" }
                    content { drawOverviewTable(overviewContent!!, lastUpdate!!) }
                }
            }
            for (germ in GermType.values()) {
                get("$germ/overview") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, germ, baseXClient)
                    println(germInfo)
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"$germ: Übersicht" }
                        content { drawOverviewTable(germInfo.overviewEntries!!, germInfo.created!!) }
                    }
                }
                get("$germ/list") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, germ, baseXClient)
                    println(germInfo)
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"$germ: Fallliste" }
                        content { drawCaseList(germInfo.caseList!!, germInfo.created!!) }
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

private suspend fun CachingUtility.getOrLoadGermInfo(
    xQueryParams: XQueryParams,
    germ: GermType,
    baseXClient: IBaseXClient
): GermInfo {
    if (this.getGermForGermtype(xQueryParams, germ)?.created == null) {
        log.info { "Loading $germ-GermInfo from server for $xQueryParams" }
        val germInfo = WebappComponents.getGermInfo(baseXClient, germ, xQueryParams)
        this.cache(xQueryParams, germInfo)
    }
    return this.getGermForGermtype(xQueryParams, germ)!!
}


private suspend fun CachingUtility.getOrLoadGlobalInfo(
    xQueryParams: XQueryParams,
    baseXClient: IBaseXClient,
): GlobalInfo {
    if (this.getGlobalInfo(xQueryParams)?.created == null) {
        log.info { "Loading GlobalInfo from server $xQueryParams" }
        val overviewContent = WebappComponents.getGlobalStatistics(baseXClient, xQueryParams)
        this.cache(xQueryParams, overviewContent)
    }
    return this.getGlobalInfo(xQueryParams)!!

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

