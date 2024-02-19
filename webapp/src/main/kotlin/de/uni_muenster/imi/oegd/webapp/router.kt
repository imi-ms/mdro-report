package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.webapp.model.CachingUtility
import de.uni_muenster.imi.oegd.webapp.model.GermType
import de.uni_muenster.imi.oegd.webapp.model.IBaseXClient
import de.uni_muenster.imi.oegd.webapp.model.XQueryParams
import de.uni_muenster.imi.oegd.webapp.view.*
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
import io.ktor.util.*
import kotlinx.html.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.util.*

private val log = KotlinLogging.logger { }

//private val mutex = Mutex()
val xqueryparams = AttributeKey<XQueryParams>("XQueryParams")
lateinit var i18n: ResourceBundle
lateinit var currentLanguage: Locale
val ApplicationCall.xQueryParams: XQueryParams
    get() = this.attributes[xqueryparams]

/**
 * @param serverMode do not block non-localhost connections
 */
fun application(baseXClient: IBaseXClient, serverMode: Boolean = false, language: Locale): Application.() -> Unit {
    i18n = ResourceBundle.getBundle("webappMessages", language)
    return {
        val cachingUtility = CachingUtility(baseXClient)
        install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, _ ->
                call.respondHtmlTemplate(
                    status = HttpStatusCode.NotFound,
                    template = LayoutTemplate(call.request.uri, call.parameters["q"])
                ) {
                    header { +"404 Not Found" }
                    content { +"${i18n["page.error.notFound"]} ${call.request.uri}" }
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
                        +i18n["page.error.serverError"]
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
                intercept(Plugins) {
                    val ip = InetAddress.getByName(call.request.local.remoteHost)
                    if (!(ip.isAnyLocalAddress || ip.isLoopbackAddress)) {
                        call.respondText(
                            text = MessageFormat.format(i18n["page.error.noLocalhost"], ip),
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
            post("/changeLanguage") {
                val parameters = call.receiveParameters()
                val referrer = call.request.headers[HttpHeaders.Referrer]?.substringBefore("?")
                changeLanguage(parameters["language"] ?: "en")
                call.respondRedirect("$referrer?q=${parameters["q"]}")
            }
            intercept(ApplicationCallPipeline.Call) {
                if (call.request.uri.startsWith("/settings/save")) return@intercept
                if (call.request.uri.startsWith("/about")) return@intercept
                if (call.request.uri == "/") return@intercept
                if (call.request.uri.startsWith("/static")) return@intercept
                if (call.request.uri.contains("invalidate-cache")) return@intercept
                if (call.request.uri.startsWith("/statistic")) return@intercept
                if (call.request.uri.startsWith("/changeLanguage")) return@intercept
                val q = call.parameters["q"]
                if (q.isNullOrBlank() || q == "null") {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +i18n.getString("page.missingConfig.heading") }
                        content {
                            +i18n.getString("page.missingConfig.text")
                            script(type = "text/javascript") {
                                unsafe { +"new bootstrap.Modal($('#settings-modal'), { keyboard: false }).show();" }
                            }
                        }
                    }
                    this.finish()
                }
            }

            get("/") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n.getString("page.welcome.heading") }
                    content { drawIndex(baseXClient.getInfo()) }
                }
            }
            post("{germ}/invalidate-cache") {
                val germ = call.parameters["germ"]!!
                val parameters = call.receiveParameters()
                val xQueryParams = XQueryParams.fromJson(parameters["q"])!!
                if (germ == "global") {
                    cachingUtility.clearGlobalInfoCache(xQueryParams)
                } else {
                    cachingUtility.clearGermInfo(xQueryParams, GermType.valueOf(germ))
                }
                call.respondRedirect(
                    call.request.headers[HttpHeaders.Referrer] ?: ("/$germ/overview?q=" + parameters["q"])
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
                val q = call.parameters["q"] ?: error(i18n.getString("page.error.missingQ"))
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                    header { +i18n.getString("navigation.hospitalMetrics") }
                    content { drawOverviewTable(overviewContent!!, lastUpdate!!, q) }
                }
            }
            for (germ in GermType.entries) {
                get("$germ/overview") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, germ)
                    val q = call.parameters["q"] ?: error(i18n.getString("page.error.missingQ"))
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: ${i18n.getString("navigation.overview")}" }
                        content { drawOverviewTable(germInfo.overviewEntries!!, germInfo.created!!, q) }
                    }
                }
                get("$germ/list") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, germ)
                    val q = call.parameters["q"] ?: error(i18n.getString("page.error.missingQ"))
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: ${i18n.getString("navigation.list")}" }
                        content { drawCaseList(germInfo.caseList!!, germInfo.created!!, q) }
                    }
                }
                get("$germ/list/csv") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, germ)
                    call.respond(germInfo.caseList!!.toCsv())
                }
            }
            get("MRGN/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, GermType.MRGN)
                val departments = germInfo.caseList!!.groupingBy { it["page.MRGN.caselist.department"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["page.MRGN.caselist.sampleType"]!! }
                    .eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n.getString("page.other.diagrams") }
                    content {
                        script("text/javascript", "/static/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart(i18n.getString("page.MRGN.diagrams.MRGNinDepartments"), departments)
                        drawBarChart(i18n.getString("page.MRGN.diagrams.numberOfSampletypes"), probenart)
                    }
                }

            }
            get("VRE/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, GermType.VRE)
                val department = germInfo.caseList!!.groupingBy { it["page.VRE.caselist.department"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["page.VRE.caselist.sampleType"]!! }
                    .eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n.getString("page.other.diagrams") }
                    content {
                        script("text/javascript", "/static/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart(i18n.getString("page.VRE.diagrams.VREinDepartments"), department)
                        drawBarChart(i18n.getString("page.VRE.diagrams.numberOfSampletypes"), probenart)
                    }
                }
            }
            get("MRSA/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.xQueryParams, GermType.MRSA)
                val department = germInfo.caseList!!.groupingBy { it["page.MRSA.caselist.department"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["page.MRSA.caselist.sampleType"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val importedOrNosocomial = germInfo.caseList!!.groupingBy { it["page.MRSA.caselist.nosocomial"]!! }
                    .eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n.getString("page.other.diagrams") }
                    content {
                        script("text/javascript", "/static/github-com-chartjs-Chart-js/Chart.min.js") {}
                        div(classes = "container") {
                            div(classes = "row") {
                                style = "height: 400px;"
                                div(classes = "col") {
                                    drawBarChart(i18n.getString("page.MRSA.diagrams.MRSAinDepartments"), department)
                                }
                            }
                            div(classes = "row") {
                                style = "height: 400px;"
                                div(classes = "col-6") {
                                    drawBarChart(i18n.getString("page.MRSA.diagrams.numberOfSamples"), probenart)
                                }
                                div(classes = "col-6") {
                                    drawPieChart(
                                        i18n.getString("page.MRSA.diagrams.numberNosocomialAndImported"),
                                        importedOrNosocomial
                                    )
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
                        .map { (key, value) -> key.year to value.overviewEntries!!.find { "page.MRSA.overview.numberOfCases" in it.title }!!.data }
                        .toMap()
                val vreTotalNumberByYear =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.VRE) }
                        .map { (key, value) -> key.year to value.overviewEntries!!.find { "page.VRE.overview.numberOfEFaecalisOverall" in it.title }!!.data }
                        .toMap() //TODO
                val data = mapOf(
                    "3MRGN" to mrgn3TotalNumberByYear,
                    "4MRGN" to mrgn4TotalNumberByYear,
                    "MRSA" to mrsaTotalNumberByYear,
                    "VRE" to vreTotalNumberByYear
                )

                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n.getString("page.other.diagrams") }
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
                val xQueryParams = XQueryParams(params["year"]!!.toInt())
                cachingUtility.cacheProvider.clearCache(xQueryParams)
                call.respondRedirect {
                    path("/statistic")
                    params["q"]?.let { parameters.append("q", it) }
                }
            }
            get("/about") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n.getString("navigation.about") }
                    content {
                        +i18n.getString("page.about.info")
                    }
                }
            }

            staticResources("/static", "static")
        }
        environment.monitor.subscribe(ApplicationStopping) {
            baseXClient.close()
        }
    }
}


private fun changeLanguage(languageSelectValue: String) {
    i18n = when(languageSelectValue) {
        "de" -> ResourceBundle.getBundle("webappMessages", Locale.GERMAN)
        "en" -> ResourceBundle.getBundle("webappMessages", Locale.ENGLISH)
        else -> ResourceBundle.getBundle("webappMessages", Locale.ENGLISH)
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

