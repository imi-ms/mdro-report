package de.uni_muenster.imi.oegd.webapp


import de.uni_muenster.imi.oegd.common.GermType
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.html.*
import kotlinx.html.ButtonType.submit
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.*
import mu.KotlinLogging
import view.*
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.LocalDate

private val log = KotlinLogging.logger { }
private val mutex = Mutex()

/**
 * @param serverMode do not block non-localhost connections
 */
fun application(baseXClient: IBaseXClient, serverMode: Boolean = false): Application.() -> Unit {
    val xqueryparams = AttributeKey<XQueryParams>("XQueryParams")
    return {
        val cachingUtility = CachingUtility(baseXClient.getInfo())
        install(Webjars)
        install(StatusPages) {
            status(HttpStatusCode.NotFound) { call, status ->
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
            intercept(Plugins) {
                val params: XQueryParams? = call.parameters["q"]?.let {
                    //TODO: Remove replace function: Somehow the behaviour of JavaFX client and Firefox is different i guess or the issue is caused by post forms?
                    Json.decodeFromString(it.replace("%22", "\""))
                }
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
                if (call.request.uri.startsWith("/settings/save")) return@intercept
                if (call.request.uri.startsWith("/about")) return@intercept
                if (call.request.uri == "/") return@intercept
                if (call.request.uri.startsWith("/static")) return@intercept
                if (call.request.uri.contains("invalidate-cache")) return@intercept
                if (call.request.uri.startsWith("/statistic")) return@intercept
                val s = call.parameters["q"]
                if (s.isNullOrBlank() || s == "null") {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, s)) {
                        header { +"Anfragekonfiguration fehlt" }
                        content {
                            +"Bitte nutzen Sie die Einstellungsleiste, um die Konfiguration der Anfrage durchzuführen."
                            script(type = "text/javascript") {
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
            post("{germ}/invalidate-cache") {
                val value = call.parameters["germ"]!!
                val parameters = call.receiveParameters()
                val xQueryParams = Json.decodeFromString<XQueryParams>(parameters["q"]!!.replace("%22", "\""))
                if (value == "global") {
                    cachingUtility.clearGlobalInfoCache(xQueryParams)
                } else {
                    val germ = GermType.valueOf(value)
                    cachingUtility.clearGermInfo(xQueryParams, germ)
                }
                call.respondRedirect(call.request.headers["Referer"] ?: ("/$value/overview?q=" + parameters["q"]))
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
                val xQueryParams = call.attributes[xqueryparams]
                val (overviewContent, lastUpdate) = cachingUtility.getOrLoadGlobalInfo(xQueryParams, baseXClient)
                val q = call.parameters["q"] ?: "Query-Parameter 'q' is missing!"
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                    header { +"Krankenhauskennzahlen" }
                    content { drawOverviewTable(overviewContent!!, lastUpdate!!, q) }
                }
            }
            for (germ in GermType.values()) {
                get("$germ/overview") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, germ, baseXClient)
                    val q = call.parameters["q"] ?: "Query-Parameter 'q' is missing!"
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: Übersicht" }
                        content { drawOverviewTable(germInfo.overviewEntries!!, germInfo.created!!, q) }
                    }
                }
                get("$germ/list") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, germ, baseXClient)
                    val q = call.parameters["q"] ?: "Query-Parameter 'q' is missing!"
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, q)) {
                        header { +"$germ: Fallliste" }
                        content { drawCaseList(germInfo.caseList!!, germInfo.created!!, q) }
                    }
                }
            }
            get("MRGN/statistic") {
                val xQueryParams = call.attributes[xqueryparams]
                try {
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.MRGN, baseXClient)
                    val data = germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }.eachCount()
                        .mapValues { it.value.toString() }
                    val data2 =
                        germInfo.caseList!!.groupingBy { it["Probenart"]!! }.eachCount()
                            .mapValues { it.value.toString() }
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"Diagramme" }
                        content {
                            script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                            drawBarChart("MRGN Nachweis in den einzelnen Fachabteilungen", data)
                            drawBarChart("Anzahl der Probenarten", data2)
                        }
                    }
                } catch (e: Exception) {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"Fehler" }
                        content {
                            +"Für die angegebene Jahreszahl konnten aus den Daten keine Diagramme erstellt werden. Versuchen Sie es mit anderen Einstellungen erneut."
                        }
                    }
                }
            }
            get("VRE/statistic") {
                val xQueryParams = call.attributes[xqueryparams]
                try {
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.VRE, baseXClient)
                    val data =
                        germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }.eachCount()
                            .mapValues { it.value.toString() }
                    val data2 =
                        germInfo.caseList!!.groupingBy { it["Probenart"]!! }.eachCount()
                            .mapValues { it.value.toString() }
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"Diagramme" }
                        content {
                            script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                            drawBarChart("VRE Nachweis in den einzelnen Fachabteilungen", data)
                            drawBarChart("Anzahl der Probenarten", data2)
                        }
                    }
                } catch (e: Exception) {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"Fehler" }
                        content {
                            +"Für die angegebene Jahreszahl konnten aus den Daten keine Diagramme erstellt werden. Versuchen Sie es mit anderen Einstellungen erneut."
                        }
                    }
                }
            }
            get("MRSA/statistic") {
                val xQueryParams = call.attributes[xqueryparams]
                try {
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.MRSA, baseXClient)
                    val data = germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }
                        .eachCount().mapValues { it.value.toString() }
                    val data2 = germInfo.caseList!!.groupingBy { it["Probeart"]!! }
                        .eachCount().mapValues { it.value.toString() }
                    val data3 = germInfo.caseList!!.groupingBy { it["nosokomial?"]!! }
                        .eachCount().mapValues { it.value.toString() }
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"Diagramme" }
                        content {
                            script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                            div(classes = "container") {
                                div(classes = "row") {
                                    style = "height: 400px;"
                                    div(classes = "col") {
                                        drawBarChart("MRSA Nachweis in den einzelnen Fachabteilungen", data)
                                    }
                                }
                                div(classes = "row") {
                                    style = "height: 400px;"
                                    div(classes = "col-6") {
                                        drawBarChart("Anzahl der Probenarten", data2)
                                    }
                                    div(classes = "col-6") {
                                        drawPieChart("Anzahl Import/Nosokomial", data3)
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"Fehler" }
                        content {
                            +"Für die angegebene Jahreszahl konnten aus den Daten keine Diagramme erstellt werden. Versuchen Sie es mit anderen Einstellungen erneut."
                        }
                    }
                }
            }
            get("/statistic") {
                val yearsEnabled = call.parameters.getAll("year[]")?.map { it.toInt() } ?: emptyList()
                val xqueryParams = yearsEnabled.map { XQueryParams(it) }
                val mrgn =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.MRGN, baseXClient) }
                val mrgn3 =
                    mrgn.map { (k, v) -> k.year to v.overviewEntries!!.find { it.title.contains("3MRGN") }!!.data }
                        .toMap()
                val mrgn4 =
                    mrgn.map { (k, v) -> k.year to v.overviewEntries!!.find { it.title.contains("4MRGN") }!!.data }
                        .toMap()
                val mrsa =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.MRSA, baseXClient) }
                        .map { (key, value) -> key.year to value.overviewEntries!!.find { it.title.contains("Gesamtanzahl aller") }!!.data }
                        .toMap()
                val vre = xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.VRE, baseXClient) }
                    .map { (key, value) -> key.year to value.overviewEntries!!.find { it.title.contains("Anzahl der gesamten E.faecalis Fälle (resistente und sensible)") }!!.data }
                    .toMap() //TODO

                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Diagramme" }
                    content {
                        if (yearsEnabled.isNotEmpty()) {
                            form(action = "/statistic") {
                                call.parameters["q"]?.let {
                                    hiddenInput(name = "q") { value = it }
                                }
                                for (year in cachingUtility.cacheProvider.getCachedParameters()) {
                                    div(classes = "form-check form-check-inline") {
                                        checkBoxInput(classes = "form-check-input", name = "year[]") {
                                            id = "p${year.year}"
                                            value = "${year.year}"
                                            checked = year.year in yearsEnabled
                                        }
                                        label(classes = "form-check-label") {
                                            htmlFor = "p${year.year}"
                                            +year.year.toString()
                                        }
                                    }
                                }
                                button(type = submit, classes = "btn btn-primary mb-2") { +"OK" }
                            }
                            script("text/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                            div(classes = "container") {
                                div(classes = "row") {
                                    for ((germ, data) in mapOf(
                                        "3MRGN" to mrgn3,
                                        "4MRGN" to mrgn4,
                                        "MRSA" to mrsa,
                                        "VRE" to vre
                                    )) {
                                        div(classes = "col-3") {
                                            style = "height: 400px;"
                                            drawBarChart("Anzahl $germ", data.mapKeys { it.key.toString() })
                                        }
                                    }

                                }
                            }
                        } else {
                            script(type = "text/javascript") {
                                unsafe {
                                    +"window.deleteReport = function (button, xQueryParams) {\n"
                                    +"  button.disabled=true;\n"
                                    +"  var formData = new FormData();\n"
                                    +"  formData.append('toDelete', xQueryParams);\n"
                                    +"  fetch('statistic/deleteReport', {method:'POST', body:  formData})\n"
                                    +"  .then(res => window.location.reload())\n"
                                    +"  return false;\n"
                                    +"}"
                                }
                            }
                            form(classes = "form-inline", method = FormMethod.post, action = "/statistic/create") {
                                input(classes = "form-control b-2 mr-sm-2", name = "year") {
                                    type = InputType.number
                                    min = "2000"
                                    max = LocalDate.now().year.toString()
                                    placeholder = "Jahr"
                                    required = true
                                }
                                if (call.parameters["q"] != null) {
                                    hiddenInput {
                                        name = "q"
                                        value = call.parameters["q"]!!
                                    }
                                }

                                button(type = submit, classes = "btn btn-light btn-mb-2") {
                                    +"Bericht erstellen"
                                }
                            }
                            form(action = "/statistic") {
                                call.parameters["q"]?.let {
                                    hiddenInput(name = "q") { value = it }
                                }
                                for (cache in cachingUtility.cacheProvider.getCachedParameters()
                                    .map { cachingUtility.cacheProvider.getCache(it)!! }) {
                                    val xQueryParams = cache.metadata.xQueryParams
                                    div(classes = "form-check") {
                                        checkBoxInput(classes = "form-check-input", name = "year[]") {
                                            id = "q${xQueryParams.year}"
                                            value = "${xQueryParams.year}"
                                            checked = xQueryParams.year in yearsEnabled
                                        }
                                        label(classes = "form-check-label") {
                                            htmlFor = "q${xQueryParams.year}"
                                            +xQueryParams.year.toString()
                                            val teilberichteZuErstellen = (GermType.values().map { it.germtype } -
                                                    cache.germCache.filter { it.created != null }.map { it.type })
                                            span(classes = "text-muted") {
                                                +"Bericht erstellt: "
                                                +cache.metadata.timeUpdated
                                                if (teilberichteZuErstellen.isNotEmpty()) {
                                                    +", Teilbericht(e) für ${teilberichteZuErstellen.joinToString()} müssen noch erzeugt werden."
                                                }

                                            }
                                        }
                                        button(type = submit, classes = "btn btn-small btn-outline-danger") {
                                            onClick = "window.deleteReport(this,'${Json.encodeToString(xQueryParams)}')"
                                            +"delete"
                                        }
                                    }
                                }

                                button(type = submit, classes = "btn btn-secondary mb-2") {
                                    +"Diagramme erstellen"
                                }
                            }


                        }
                    }
                }
            }
            post("/statistic/create") {
                val params = call.receiveParameters()
                val xQueryParams = XQueryParams(params["year"]?.toInt())
                cachingUtility.getOrLoadGlobalInfo(xQueryParams, baseXClient)
                cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.MRGN, baseXClient)
                cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.MRSA, baseXClient)
                cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.VRE, baseXClient)
                call.respondRedirect("/statistic${params["q"]?.let { "?q=$it" } ?: ""}")
            }
            post("/statistic/deleteReport") {
                val params = call.receiveParameters()
                val xQueryParams = Json.decodeFromString<XQueryParams>(params["toDelete"]!!)
                cachingUtility.cacheProvider.clearCache(xQueryParams)
                call.respondRedirect("/statistic${params["q"].let { "?q=$it" } ?: ""}")
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

private suspend fun CachingUtility.getOrLoadGermInfo(
    xQueryParams: XQueryParams,
    germ: GermType,
    baseXClient: IBaseXClient
): GermInfo {
    if (getGermForGermtype(xQueryParams, germ)?.created == null) {
        if (!CachingUtility.RequestState.isRequestActive(germ)) {
            coroutineScope {
                withContext(Dispatchers.Default) {
                    CachingUtility.RequestState.markRequestActive(germ) //Mark as active while queuing coroutine
                    mutex.withLock {
                        log.info { "Loading $germ-GermInfo from server for $xQueryParams" }
                        val germInfo = model.DataProvider.getGermInfo(baseXClient, germ, xQueryParams)
                        cache(xQueryParams, germInfo)
                        log.info { "Loading done of ${germInfo.type} for $xQueryParams" }
                        CachingUtility.RequestState.markRequestInactive(germ)
                    }
                }
            }
        } else {
            coroutineScope {
                while (CachingUtility.RequestState.isRequestActive(germ)) {
                    delay(1000)
                }
            }
        }
    }
    return this.getGermForGermtype(xQueryParams, germ)!!
}


private suspend fun CachingUtility.getOrLoadGlobalInfo(
    xQueryParams: XQueryParams,
    baseXClient: IBaseXClient,
): GlobalInfo {
    if (getGlobalInfo(xQueryParams)?.created == null) {
        if (!CachingUtility.RequestState.isRequestActive(null)) {
            coroutineScope {
                withContext(Dispatchers.Default) {
                    CachingUtility.RequestState.markRequestActive(null) //Mark as active while queuing coroutine
                    mutex.withLock {
                        log.info { "Loading GlobalInfo from server $xQueryParams" }
                        val overviewContent = model.DataProvider.getGlobalStatistics(baseXClient, xQueryParams)
                        cache(xQueryParams, overviewContent)
                        log.info("Done with Global Overview request")
                        CachingUtility.RequestState.markRequestInactive(null)
                    }
                }
            }
        } else {
            coroutineScope {
                while (CachingUtility.RequestState.isRequestActive(null)) {
                    delay(1000)
                }
            }
        }
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

