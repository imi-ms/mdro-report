package de.uni_muenster.imi.oegd.webapp.model

import de.uni_muenster.imi.oegd.webapp.parseXmlAttributeOrderPreserving
import de.uni_muenster.imi.oegd.webapp.toGermanDate
import de.uni_muenster.imi.oegd.webapp.transformEntry
import java.time.LocalDateTime

object DataProvider {
    //TODO: Create constructor with BaseXClient?
    suspend fun getGermInfo(basexClient: IBaseXClient, germ: GermType, xQueryParams: XQueryParams): GermInfo {
        when (germ) {
            GermType.MRSA -> {
                val caseList = getMRSACSV(basexClient, xQueryParams)
                val overviewEntry = getMRSAOverview(basexClient, xQueryParams, caseList)
                return GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.MRGN -> {
                val caseList = getMRGNCSV(basexClient, xQueryParams)
                val overviewEntry = getMRGNOverview(basexClient, xQueryParams, caseList)
                return GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.VRE -> {
                val caseList = getVRECSV(basexClient, xQueryParams)
                val overviewEntry = getVREOverview(basexClient, xQueryParams, caseList)
                return GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }
        }
    }


    suspend fun getMRSACSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrsaList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.MRSA, xQueryParams))
        return parseXmlAttributeOrderPreserving(mrsaList)
            .map { it.transformEntry("samplingDate", ::toGermanDate) }
            .map { it.mapKeys { (k, _) -> "page.MRSA.caselist.$k" } }
    }

    suspend fun getMRGNCSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrgnList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.MRGN, xQueryParams))
        val parsed = parseXmlAttributeOrderPreserving(mrgnList)
            .map {
                //Replace MRGN3 -> 3MRGN, MRGN4 -> 4MRGN
                it.transformEntry("class") { v ->
                    v.replace("MRGN3", "3MRGN").replace("MRGN4", "4MRGN")
                }.transformEntry("pathogen") { v ->
                    v.replace("Klebsiella pneumoniae ssp pneumoniae", "Klebsiella pneumoniae")
                }
            }.map { it.transformEntry("samplingDate", ::toGermanDate) }
            .map { it.mapKeys { (k, _) -> "page.MRGN.caselist.$k" } }
        //E-Mail von Zentralstelle IfSG: "Doppelte Fälle sind nur zulässig, wenn es sich um unterschiedliche Erreger und MRGN-Klassifikationen handelt"
        val result = parsed.distinctBy {
            val case = it["page.MRGN.caselist.caseID"]
            val pathogen = it["page.MRGN.caselist.pathogen"]
            val classification = with(it["page.MRGN.caselist.class"]) {
                when {
                    isNullOrBlank() -> null
                    contains("MRGN3") || contains("3MRGN") -> 3
                    contains("MRGN4") || contains("4MRGN") -> 4
                    else -> null
                }
            }
            Triple(case, pathogen, classification)
        }
        return result
    }

    suspend fun getVRECSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val vreList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.VRE, xQueryParams))
        return parseXmlAttributeOrderPreserving(vreList)
            .map { it.transformEntry("samplingDate", ::toGermanDate) }
            .map { it.mapKeys { (k, _) -> "page.VRE.caselist.$k" } }
    }


    //TODO: Übersicht auf Plausibilität checken / checken lassen

    suspend fun getGlobalStatistics(baseXClient: IBaseXClient, xQueryParams: XQueryParams): GlobalInfo {
        return GlobalInfo(
            listOf(
                createBaseXOverviewEntry(
                    "page.hospitalMetrics.cases",
                    BaseXQueries.Fallzahlen,
                    baseXClient,
                    xQueryParams
                ),
                createBaseXOverviewEntry(
                    "page.hospitalMetrics.days",
                    BaseXQueries.Falltage,
                    baseXClient,
                    xQueryParams
                ),
            ), LocalDateTime.now().toString()
        )
    }

    suspend fun getMRSAOverview(
        baseXClient: IBaseXClient,
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        suspend fun entry(name: String, query: String) =
            createBaseXOverviewEntry(name, query, baseXClient, xQueryParams)

        val mrsaTotal = DataProcessor.countMRSATotal(caseList)
        val mrsaNosokomial = DataProcessor.countMRSANosokomial(caseList)
        val mrsaImported = DataProcessor.countMRSAImported(caseList)

        //TODO: Blutkultur-Abfrage hat Vorrang vor den excel sachen => Tabelle mergen.

        return listOf(
            entry("page.MRSA.overview.nasalSwabs", BaseXQueries.NasenRachenAbstriche),
            entry("page.MRSA.overview.bloodSAureus", BaseXQueries.MSSABK),
            entry("page.MRSA.overview.bloodMRSA", BaseXQueries.MRSABK),
            OverviewEntry("page.MRSA.overview.numberOfCases", BaseXQueries.MRSA, "$mrsaTotal"),
            OverviewEntry("page.MRSA.overview.importedMRSA", BaseXQueries.MRSA, "$mrsaImported"),
            OverviewEntry("page.MRSA.overview.nosocomialMRSA", BaseXQueries.MRSA, "$mrsaNosokomial"),
            entry("page.MRSA.overview.inpatientDays", BaseXQueries.FalltageMRSA)
        )
    }

    suspend fun getMRGNOverview(
        baseXClient: IBaseXClient,
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        val mrgn3Cases = DataProcessor.countMRGN3Cases(caseList)
        val mrgn4Cases = DataProcessor.countMRGN4Cases(caseList)

        return listOf(
            OverviewEntry("page.MRGN.overview.numberOf3MRGN", BaseXQueries.MRGN, "$mrgn3Cases"),
            OverviewEntry("page.MRGN.overview.numberOf4MRGN", BaseXQueries.MRGN, "$mrgn4Cases"),
        )
    }

    suspend fun getVREOverview(
        baseXClient: IBaseXClient, xQueryParams: XQueryParams, caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        suspend fun entry(name: String, query: String) =
            createBaseXOverviewEntry(name, query, baseXClient, xQueryParams)
        //TODO E.faecuium total und VRE-E.faecium ist gleich, sollte es aber nicht sein
        //TODO: MRE-Aus Blutkulturen sollte höhere Priorität haben ggü. dem Ort der ersten Abnahme.
        val numEfaecalisResistant = DataProcessor.countVREEfaecalisResistant(caseList)
        val numEfaeciumTotal = DataProcessor.countVREEfaeciumTotal(caseList)
        val numEfaeciumResistant = DataProcessor.countVREEfaeciumResistant(caseList)
        val numOtherCases = DataProcessor.countOtherCases(caseList)

        return listOf(
            entry("page.VRE.overview.numberOfEFaecalisOverall", BaseXQueries.AnzahlEFaecalis),
            OverviewEntry("page.VRE.overview.numberOfVREEFaecalis", BaseXQueries.VRE, "$numEfaecalisResistant"),
            entry("page.VRE.overview.numberOfEFaeciumOverall", BaseXQueries.AnzahlEFaecium),
            OverviewEntry("page.VRE.overview.numberOfVREEFaecium", BaseXQueries.VRE, "$numEfaeciumResistant"),
            OverviewEntry("page.VRE.overview.otherVRE", BaseXQueries.VRE, "$numOtherCases"),
            entry("page.VRE.overview.numberEFaeciumComplete", BaseXQueries.EfaeciumBK),
            entry("page.VRE.overview.numberOfVREEFaeciumBlood", BaseXQueries.VREBK)
        )
    }

    suspend fun createBaseXOverviewEntry(
        name: String,
        query: String,
        baseXClient: IBaseXClient,
        xQueryParams: XQueryParams
    ): OverviewEntry {
        val query2 = BaseXQueries.applyParams(query, xQueryParams)
        val result = baseXClient.executeXQuery(query2)
        return OverviewEntry(name, query2, result)
    }
}