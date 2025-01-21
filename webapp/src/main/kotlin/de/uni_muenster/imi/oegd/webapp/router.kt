package de.uni_muenster.imi.oegd.webapp

import de.uni_muenster.imi.oegd.webapp.model.*
import de.uni_muenster.imi.oegd.webapp.view.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.html.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import kotlinx.html.*
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.net.InetAddress
import java.text.MessageFormat
import java.util.*

private val log = KotlinLogging.logger { }

//private val mutex = Mutex()
val queryparams = AttributeKey<Params>("queryParams")
lateinit var i18n: ResourceBundle
lateinit var currentLanguage: Locale
val ApplicationCall.queryParams: Params
    get() = this.attributes[queryparams]

private val OnlyLocalhostPlugin = createRouteScopedPlugin("OnlyLocalhostPlugin") {
    onCall { call ->
        val ip = InetAddress.getByName(call.request.local.remoteHost)
        if (!(ip.isAnyLocalAddress || ip.isLoopbackAddress)) {
            call.respondText(
                text = MessageFormat.format(i18n["page.error.noLocalhost"], ip),
                status = HttpStatusCode.Unauthorized
            )
            //call.finish()
        }
    }
}

private val PutParamsPlugin = createRouteScopedPlugin("PutParamsPlugin") {
    onCall { call ->
        val params = Params.fromJson(call.parameters["q"])
        if (params != null) {
            call.attributes.put(queryparams, params)
        }
    }
}

private val AskForConfigForMostPages = createRouteScopedPlugin("AskForConfigForMostPages") {
    onCall { call ->
        if (call.request.uri.startsWith("/settings/save")) return@onCall
        if (call.request.uri.startsWith("/about")) return@onCall
        if (call.request.uri == "/") return@onCall
        if (call.request.uri.startsWith("/static")) return@onCall
        if (call.request.uri.contains("invalidate-cache")) return@onCall
        if (call.request.uri.startsWith("/statistic")) return@onCall
        if (call.request.uri.startsWith("/changeLanguage")) return@onCall
        val q = call.parameters["q"]
        if (q.isNullOrBlank() || q == "null") {
            call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                header { +i18n["page.missingConfig.heading"] }
                content {
                    +i18n["page.missingConfig.text"]
                    script(type = "text/javascript") {
                        unsafe { +"new bootstrap.Modal($('#settings-modal'), { keyboard: false }).show();" }
                    }
                }
            }
//                this.finish()
        }
    }
}

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
                install(OnlyLocalhostPlugin)
            }
            install(PutParamsPlugin)
            post("/settings/save") {
                val parameters = call.receiveParameters()
                val year = parameters["year"]?.ifBlank { null }
                val caseTypes = parameters.getAll("caseTypes")
                val x = if (year != null && caseTypes != null) Params(
                    XQueryParams(year.toInt()),
                    FilterParams(caseTypes.map { CaseType.valueOf(it) })
                ) else null
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
            install(AskForConfigForMostPages)

            get("/") {
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n["page.welcome.heading"] }
                    content { drawIndex(baseXClient.getInfo()) }
                }
            }
            post("{germ}/invalidate-cache") {
                val germ = call.parameters["germ"]!!
                val parameters = call.receiveParameters()
                val xQueryParams = Params.fromJson(parameters["q"])!!.xquery
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
                val xQueryParams = call.queryParams.xquery
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
                val globalInfoUnfiltered = cachingUtility.getOrLoadGlobalInfo(call.queryParams.xquery)
                val overviewContent = applyFilter(call.queryParams.filter, globalInfoUnfiltered.overviewEntries!!)
                val q = call.parameters["q"] ?: error(i18n.getString("page.error.missingQ"))
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                    header { +i18n["navigation.hospitalMetrics"] }
                    content { drawOverviewTable(null, overviewContent, globalInfoUnfiltered.created!!, q) }
                }
            }
            for (germ in GermType.entries) {
                get("$germ/overview") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.queryParams.xquery, germ)
                    val q = call.parameters["q"] ?: error(i18n.getString("page.error.missingQ"))


                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: ${i18n["navigation.overview"]}" }
                        content {
                            drawOverviewTable(
                                germ,
                                applyFilter(call.queryParams.filter, germInfo.overviewEntries!!),
                                germInfo.created!!,
                                q
                            )
                        }
                    }
                }
                get("$germ/list") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.queryParams.xquery, germ)
                    val q = call.parameters["q"] ?: error(i18n.getString("page.error.missingQ"))
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: ${i18n["navigation.list"]}" }
                        content {
                            drawCaseList(
                                germ,
                                germInfo.caseList!!.filterCaseType(call.queryParams.filter.caseTypes),
                                germInfo.created!!,
                                q
                            )
                        }
                    }
                }
                get("$germ/list/csv") {
                    val germInfo = cachingUtility.getOrLoadGermInfo(call.queryParams.xquery, germ)
                    call.respond(germInfo.caseList!!.toCsv())
                }
            }
            get("MRGN/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.queryParams.xquery, GermType.MRGN)
                val departments = germInfo.caseList!!.groupingBy { it["department"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["sampleType"]!! }
                    .eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n["page.other.diagrams"] }
                    content {
                        script("text/javascript", "/static/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart(i18n.getString("page.MRGN.diagrams.MRGNinDepartments"), departments)
                        drawBarChart(i18n.getString("page.MRGN.diagrams.numberOfSampletypes"), probenart)
                    }
                }

            }
            get("VRE/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.queryParams.xquery, GermType.VRE)
                val department = germInfo.caseList!!.groupingBy { it["department"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["sampleType"]!! }
                    .eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +i18n["page.other.diagrams"] }
                    content {
                        script("text/javascript", "/static/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart(i18n.getString("page.VRE.diagrams.VREinDepartments"), department)
                        drawBarChart(i18n.getString("page.VRE.diagrams.numberOfSampletypes"), probenart)
                    }
                }
            }
            get("MRSA/statistic") {
                val germInfo = cachingUtility.getOrLoadGermInfo(call.queryParams.xquery, GermType.MRSA)
                val department = germInfo.caseList!!.groupingBy { it["department"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val probenart = germInfo.caseList!!.groupingBy { it["sampleType"]!! }
                    .eachCount().mapValues { it.value.toString() }
                val importedOrNosocomial = germInfo.caseList!!.groupingBy { it["nosocomial"]!! }
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
                    mrgnData.map { (k, v) ->
                        k.year to applyFilter(call.queryParams.filter, v.overviewEntries!!)
                            .find { "3MRGN" in it.title }!!.data
                    }.toMap()
                val mrgn4TotalNumberByYear =
                    mrgnData.map { (k, v) ->
                        k.year to applyFilter(
                            call.queryParams.filter,
                            v.overviewEntries!!
                        ).find { "4MRGN" in it.title }!!.data
                    }.toMap()
                val mrsaTotalNumberByYear =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.MRSA) }
                        .map { (key, v) ->
                            key.year to applyFilter(
                                call.queryParams.filter,
                                v.overviewEntries!!
                            ).find { "numberOfCases" in it.title }!!.data
                        }
                        .toMap()
                val vreTotalNumberByYear =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.VRE) }
                        .map { (key, v) ->
                            key.year to applyFilter(
                                call.queryParams.filter,
                                v.overviewEntries!!
                            ).find { "numberOfEFaecalisOverall" in it.title }!!.data
                        }
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
        monitor.subscribe(ApplicationStopping) {
            baseXClient.close()
        }
    }
}

fun applyFilter(filter: FilterParams, overviewEntries: Map<CaseType, List<OverviewEntry>>): List<OverviewEntry> {
    val entries = mutableListOf<OverviewEntry>()
    for (caseType in filter.caseTypes) {
        overviewEntries[caseType]!!.forEach { overviewEntry ->
            val find = entries.find { it.title == overviewEntry.title }
            if (find == null) {
                entries += overviewEntry
            } else {
                find.data = (find.data.toLong() + overviewEntry.data.toLong()).toString()
            }
        }
    }
    return entries
}


private fun changeLanguage(languageSelectValue: String) {
    i18n = when (languageSelectValue) {
        "de" -> ResourceBundle.getBundle("webappMessages", Locale.GERMAN)
        "en" -> ResourceBundle.getBundle("webappMessages", Locale.ENGLISH)
        else -> ResourceBundle.getBundle("webappMessages", Locale.ENGLISH)
    }
}


private suspend fun uploadCache(multipartdata: MultiPartData, cachingUtility: CachingUtility) {
    multipartdata.forEachPart { part ->
        if (part is PartData.FileItem) {
            val newCache = part.provider().readRemaining().readText()
            cachingUtility.uploadExistingCache(newCache)
        }
        part.dispose()
    }

}

private fun List<Map<String, String>>.filterCaseType(caseTypes: List<CaseType>): List<Map<String, String>> {
    val basexNames = caseTypes.flatMap { it.basexName }
    return this.filter { it["caseType"] in basexNames }
}
