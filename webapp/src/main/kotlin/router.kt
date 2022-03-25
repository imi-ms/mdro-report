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
import kotlinx.html.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.time.LocalDate

private val log = KotlinLogging.logger { }

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
                val params: XQueryParams? = call.parameters["q"]?.let {
                    Json.decodeFromString(
                        it.replace(
                            "%22",
                            "\""
                        )
                    )
                } //TODO: Remove replace function
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
                if (call.request.uri.contains("static")) return@intercept
                if (call.request.uri.contains("invalidate-cache")) return@intercept
                if (call.request.uri.startsWith("/statistic")) return@intercept
                val s = call.parameters["q"]
                if (s.isNullOrBlank() || s == "null") {
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, s)) {
                        header { +"Anfragekonfiguration fehlt" }
                        content {
                            +"Bitte nutzen Sie die Einstellungsleiste, um die Konfiguration der Anfrage durchzuführen"
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
            post("{germ}/invalidate-cache") {
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
                    header { +"Krankenhauskennzahlen" }
                    content { drawOverviewTable(overviewContent!!, lastUpdate!!) }
                }
            }
            for (germ in GermType.values()) {
                get("$germ/overview") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, germ, baseXClient)
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"$germ: Übersicht" }
                        content { drawOverviewTable(germInfo.overviewEntries!!, germInfo.created!!) }
                    }
                }
                get("$germ/list") {
                    val xQueryParams = call.attributes[xqueryparams]
                    val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, germ, baseXClient)
                    call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                        header { +"$germ: Fallliste" }
                        content { drawCaseList(germInfo.caseList!!, germInfo.created!!) }
                    }
                }
            }
            get("MRGN/statistic") {
                val xQueryParams = call.attributes[xqueryparams]
                val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.MRGN, baseXClient)
                val data = germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }.eachCount()
                    .mapValues { it.value.toString() }
                val data2 =
                    germInfo.caseList!!.groupingBy { it["Probenart"]!! }.eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Diagramme" }
                    content {
                        script("application/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart("MRGN Nachweis in den einzelnen Fachabteilungen", data)
                        drawBarChart("Anzahl der Probenarten", data2)
                    }
                }
            }
            get("VRE/statistic") {
                val xQueryParams = call.attributes[xqueryparams]
                val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.VRE, baseXClient)
                val data = germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"]!! }.eachCount()
                    .mapValues { it.value.toString() }
                val data2 =
                    germInfo.caseList!!.groupingBy { it["Probenart"]!! }.eachCount().mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Diagramme" }
                    content {
                        script("application/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart("VRE Nachweis in den einzelnen Fachabteilungen", data)
                        drawBarChart("Anzahl der Probenarten", data2)
                    }
                }
            }
            get("MRSA/statistic") {
                val xQueryParams = call.attributes[xqueryparams]
                val germInfo = cachingUtility.getOrLoadGermInfo(xQueryParams, GermType.MRSA, baseXClient)
                val data =
                    germInfo.caseList!!.groupingBy { it["Fachabteilung zum Abnahmezeitpunkt"] ?: "null" }.eachCount()
                        .mapValues { it.value.toString() }
                val data2 = germInfo.caseList!!.groupingBy { it["Probenart"] ?: "null" }.eachCount()
                    .mapValues { it.value.toString() }
                val data3 = germInfo.caseList!!.groupingBy { it["nosokomial?"] ?: "null" }.eachCount().mapKeys {
                    when (it.key) {
                        "true" -> "nosokomial"
                        "false" -> "importiert"
                        else -> "unbekannt"
                    }
                }.mapValues { it.value.toString() }
                call.respondHtmlTemplate(LayoutTemplate(call.request.uri, call.parameters["q"])) {
                    header { +"Diagramme" }
                    content {
                        script("application/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                        drawBarChart("MRSA Nachweis in den einzelnen Fachabteilungen", data)
                        drawBarChart("Anzahl der Probenarten", data2)
                        drawPieChart("Anzahl Import/Nosokomial", data3)
                    }
                }
            }
            get("/statistic") {
                val yearsEnabled = call.parameters.getAll("year[]")?.map { it.toInt() } ?: emptyList()
                val xqueryParams = yearsEnabled.map { XQueryParams(it) }
                val mrgn =
                    xqueryParams.associateWith { cachingUtility.getOrLoadGermInfo(it, GermType.MRGN, baseXClient) }
                val mrgn3 =
                    mrgn.map { (key, value) -> key.year to value.overviewEntries!!.find { it.title.contains("3MRGN") }!!.data }
                        .toMap()
                val mrgn4 =
                    mrgn.map { (key, value) -> key.year to value.overviewEntries!!.find { it.title.contains("4MRGN") }!!.data }
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
                                for (year in cachingUtility.getCachedParameters()) {
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
                                button(type = ButtonType.submit, classes = "btn btn-primary mb-2") { +"OK" }
                            }
                            script("application/javascript", "/webjars/github-com-chartjs-Chart-js/Chart.min.js") {}
                            div(classes = "container") {
                                div(classes = "row") {
                                    for ((germ, data) in mapOf(
                                        "3MRGN" to mrgn3,
                                        "4MRGN" to mrgn4,
                                        "MRSA" to mrsa,
                                        "VRE" to vre
                                    )) {
                                        div(classes = "col-3") {
                                            drawBarChart("Anzahl $germ", data.mapKeys { it.key.toString() })
                                        }
                                    }

                                }
                            }
                        } else {
                            form(action = "/statistic") {
                                call.parameters["q"]?.let {
                                    hiddenInput(name = "q") { value = it }
                                }
                                for (cache in cachingUtility.getCachedParameters()
                                    .map { cachingUtility.getCache(it)!! }) {
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
                                            span(classes = "text-muted") {
                                                +"Bericht erstellt: "
                                                +cache.metadata.timeUpdated
                                                +", zuletzt aktualisiert: "
                                                +cache.metadata.timeUpdated
                                                +", verfügbar: "
                                                +(cache.germCache.filter { it.created != null }
                                                    .joinToString { it.type })
                                            }
                                        }
                                    }
                                }

                                button(
                                    type = ButtonType.submit,
                                    classes = "btn btn-secondary mb-2"
                                ) { +"Diagramme erstellen" }
                            }
                            form(classes = "form-inline", method = FormMethod.post, action = "") {
                                input(classes = "form-control b-2 mr-sm-2", name = "year") {
                                    type = InputType.number
                                    min = "2000"
                                    max = LocalDate.now().year.toString()
                                    placeholder = "Jahr"
                                }
                                button(
                                    type = ButtonType.submit,
                                    classes = "btn btn-light btn-mb-2"
                                ) { +"Bericht erstellen" }
                            }


                        }
                    }
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

