package de.uni_muenster.imi.oegd.webapp


import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.application.ApplicationCallPipeline.ApplicationPhase.Plugins
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.webjars.*
import io.ktor.util.*
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.CachingUtility
import model.GermType
import model.IBaseXClient
import model.XQueryParams
import mu.KotlinLogging
import view.*
import java.net.InetAddress
import java.nio.charset.StandardCharsets

private val log = KotlinLogging.logger { }

//private val mutex = Mutex()
val xqueryparams = AttributeKey<XQueryParams>("XQueryParams")
val ApplicationCall.xQueryParams: XQueryParams
    get() = this.attributes[xqueryparams]

/**
 * @param serverMode do not block non-localhost connections
 */
fun application(baseXClient: IBaseXClient, serverMode: Boolean = false): Application.() -> Unit {
    return {
        val cachingUtility = CachingUtility(baseXClient)
        install(Webjars)
        install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, _ ->
                call.respondHtmlTemplate(
                    status = HttpStatusCode.NotFound,
                    template = LayoutTemplate(call.request.uri, call.parameters["q"])
                ) {
                    header { +"404 Not Found" }
                    content { +"No route defined for URL: ${call.request.uri}" }
                }
            }
            exception<Throwable> { call, cause ->
                cause.printStackTrace()
                call.respondHtmlTemplate(
                    status = HttpStatusCode.InternalServerError,
                    template = LayoutTemplate(call.request.uri, call.parameters["q"])
                ) {
                    header { +"500 Internal Server Error" }
                    content {
                        +"${cause.message}"
                        br
                        +"Please retry your query with different parameters!"
                        br
                        pre { +cause.stackTraceToString() }

                    }
                }
            }
        }
//        install(CallLogging) {
//            level = Level.INFO
//            filter { call -> call.request.path().startsWith("/") && !call.request.path().startsWith("/webjars/")}
//        }
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
            intercept(Plugins) {
                val params: XQueryParams? = XQueryParams.fromJson(call.parameters["q"])
                if (params != null) {
                    call.attributes.put(xqueryparams, params)
                }
            }
            post("/settings/save") {
                val parameters = call.receiveParameters()
                val x = XQueryParams(parameters["year"]?.toInt())
                val q = Json.encodeToString(x)
                val referrer = call.request.headers[HttpHeaders.Referrer]?.substringBefore("?")
                call.respondRedirect("$referrer?q=$q")
            }
            intercept(ApplicationCallPipeline.Call) {
                if (call.request.uri.startsWith("/settings/save")) return@intercept
                if (call.request.uri.startsWith("/about")) return@intercept
                if (call.request.uri == "/") return@intercept
                if (call.request.uri.startsWith("/static")) return@intercept
                if (call.request.uri.contains("invalidate-cache")) return@intercept
                if (call.request.uri.startsWith("/statistic")) return@intercept
                val q = call.parameters["q"]
                if (q.isNullOrBlank() || q == "null") {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"Anfragekonfiguration fehlt" }
                        content {
                            +"Bitte nutzen Sie die Einstellungsleiste, um die Konfiguration der Anfrage durchzuführen."
                            script(type = "text/javascript") {
                                +"$(function(){ $('#settings-modal').modal({focus:true}) });"
                            }
                        }
                    }
                    this.finish()
                }
            }

            get("/") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Willkommen" }
                    content { drawIndex(baseXClient.getInfo()) }
                }
            }
            post("{germ}/invalidate-cache") {
                val value = call.parameters["germ"]!!
                val parameters = call.receiveParameters()
                val xQueryParams = XQueryParams.fromJson(parameters["q"])!!
                if (value == "global") {
                    cachingUtility.clearGlobalInfoCache(xQueryParams)
                } else {
                    val germ = GermType.valueOf(value)
                    cachingUtility.clearGermInfo(xQueryParams, germ)
                }
                call.respondRedirect(
                    call.request.headers[HttpHeaders.Referrer] ?: ("/$value/overview?q=" + parameters["q"])
                )
            }
            post("/settings/uploadCache") {
                uploadCache(call.receiveMultipart(), cachingUtility)
                call.respondRedirect("/")
            }
            get("/settings/downloadCache") {
                val xQueryParams = call.xQueryParams

                cachingUtility.cacheAllData(xQueryParams)

                call.response.header(
                    HttpHeaders.ContentDisposition, ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        cachingUtility.cacheProvider.getCacheFileName(xQueryParams)
                    ).toString()
                )
                call.respondText(
                    Json.encodeToString(cachingUtility.cacheProvider.getCache(xQueryParams)),
                    contentType = ContentType.Application.Json
                )
            }
            get("global/overview") {
                val (overviewContent, lastUpdate) = cachingUtility.getOrLoadGlobalInfo(call.xQueryParams)
                val q = call.parameters["q"] ?: error("Query-Parameter 'q' is missing!")
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                    header { +"Krankenhauskennzahlen" }
                    content { drawOverviewTable(overviewContent!!, lastUpdate!!, q) }
                }
            }
            for (germ in GermType.values()) {
                get("$germ/overview") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, germ)
                    val q = call.parameters["q"] ?: error("Query-Parameter 'q' is missing!")
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: Übersicht" }
                        content { drawOverviewTable(germInfo.overviewEntries!!, germInfo.created!!, q) }
                    }
                }
                get("$germ/list") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, germ)
                    val q = call.parameters["q"] ?: error("Query-Parameter 'q' is missing!")
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: Fallliste" }
                        content { drawCaseList(germInfo.caseList!!, germInfo.created!!, q) }
                    }
                }
            }
            get("MRGN/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, GermType.MRGN)
                val departments = germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["Probenart"]!! }
                    .eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Diagramme" }
                    content {
                        script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart("MRGN Nachweis in den einzelnen Fachabteilungen", departments)
                        drawBarChart("Anzahl der Probenarten", probenart)
                    }
                }

            }
            get("VRE/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, GermType.VRE)
                val department = germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["Probenart"]!! }
                    .eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Diagramme" }
                    content {
                        script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart("VRE Nachweis in den einzelnen Fachabteilungen", department)
                        drawBarChart("Anzahl der Probenarten", probenart)
                    }
                }
            }
            get("MRSA/statistic") {
                val xQueryParams = call.xQueryParams
                val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.MRSA)
                    val department = germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }
                        .eachCount().mapValues { it.value.toString() }
                    val probenart = germInfo.caseList!!.groupingBy { it["Probeart"]!! }
                        .eachCount().mapValues { it.value.toString() }
                    val importedOrNosocomial = germInfo.caseList!!.groupingBy { it["nosokomial?"]!! }
                        .eachCount().mapValues { it.value.toString() }
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"Diagramme" }
                        content {
                            script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                            div(classes = "container") {
                                div(classes = "row") {
                                    style = "height: 400px;"
                                    div(classes = "col") {
                                        drawBarChart("MRSA Nachweis in den einzelnen Fachabteilungen", department)
                                    }
                                }
                                div(classes = "row") {
                                    style = "height: 400px;"
                                    div(classes = "col-6") {
                                        drawBarChart("Anzahl der Probenarten", probenart)
                                    }
                                    div(classes = "col-6") {
                                        drawPieChart("Anzahl Import/Nosokomial", importedOrNosocomial)
                                    }
                                }
                            }
                        }
                    }
            }
            get("/statistic") {
                val yearsEnabled = call.parameters.getAll("year[]")?.map { it.toInt() } ?: emptyList()
                val years = cachingUtility.cacheProvider.getCachedParameters().map { it.year!! }
                val xqueryParams = yearsEnabled.map { XQueryParams(it) }
                val mrgnData = xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.MRGN) }
                val mrgn3TotalNumberByYear =
                    mrgnData.map { (k, v) -> k.year to v.overviewEntries!!.find { "3MRGN" in it.title }!!.data }.toMap()
                val mrgn4TotalNumberByYear =
                    mrgnData.map { (k, v) -> k.year to v.overviewEntries!!.find { "4MRGN" in it.title }!!.data }.toMap()
                val mrsaTotalNumberByYear =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.MRSA) }
                        .map { (key, value) -> key.year to value.overviewEntries!!.find { "Gesamtanzahl aller" in it.title }!!.data }
                        .toMap()
                val vreTotalNumberByYear =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.VRE) }
                        .map { (key, value) -> key.year to value.overviewEntries!!.find { "Anzahl der gesamten E.faecalis Fälle (resistente und sensible)" in it.title }!!.data }
                        .toMap() //TODO
                val data = mapOf(
                    "3MRGN" to mrgn3TotalNumberByYear,
                    "4MRGN" to mrgn4TotalNumberByYear,
                    "MRSA" to mrsaTotalNumberByYear,
                    "VRE" to vreTotalNumberByYear
                )

                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Diagramme" }
                    content {
                        if (yearsEnabled.isNotEmpty()) {
                            drawDiagrams(data, years, yearsEnabled, call.parameters["q"])
                        } else {
                            val cacheData = cachingUtility.cacheProvider.getCachedParameters()
                                .map { cachingUtility.cacheProvider.getCache(it)!! }
                            drawYearSelector(cacheData, call.parameters["q"])
                        }
                    }
                }
            }
            post("/statistic/create") {
                val params = call.receiveParameters()
                val xQueryParams = XQueryParams(params["year"]?.toInt())
                cachingUtility.cacheAllData(xQueryParams)
                call.respondRedirect {
                    path("/statistic")
                    params["q"]?.let { parameters.append("q", it) }
                }
            }
            post("/statistic/deleteReport") {
                val params = call.receiveParameters()
                val xQueryParams = Json.decodeFromString<XQueryParams>(params["toDelete"]!!)
                cachingUtility.cacheProvider.clearCache(xQueryParams)
                call.respondRedirect {
                    path("/statistic")
                    params["q"]?.let { parameters.append("q", it) }
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
                resources("static")
            }
        }
        environment.monitor.subscribe(ApplicationStopping) {
            baseXClient.close()
        }
    }
}




private suspend fun uploadCache(multipartdata: MultiPartData, cachingUtility: CachingUtility) {
    multipartdata.forEachPart { part ->
        if (part is PartData.FileItem) {
            val fileBytes = part.streamProvider().readBytes()
            val newCache = String(fileBytes, StandardCharsets.UTF_8)
            cachingUtility.uploadExistingCache(newCache)
        }
        part.dispose()
    }

}

