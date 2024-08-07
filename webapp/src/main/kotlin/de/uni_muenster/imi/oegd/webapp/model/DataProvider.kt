package de.uni_muenster.imi.oegd.webapp.model

import de.uni_muenster.imi.oegd.webapp.parseXmlAttributeOrderPreserving
import de.uni_muenster.imi.oegd.webapp.toGermanDate
import de.uni_muenster.imi.oegd.webapp.transformEntry
import java.time.LocalDateTime

class DataProvider(val basexClient: IBaseXClient) {
    suspend fun getGermInfo(germ: GermType, params: XQueryParams): GermInfo {
        return when (germ) {
            GermType.MRSA -> {
                val caseList = getMRSACaselist(params)
                val overviewEntry = getMRSAOverview(params, caseList)
                GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.MRGN -> {
                val caseList = getMRGNCaselist(params)
                val overviewEntry = getMRGNOverview(params, caseList)
                GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.VRE -> {
                val caseList = getVRECaselist(params)
                val overviewEntry = getVREOverview(params, caseList)
                GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }
        }
    }


    suspend fun getMRSACaselist(xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrsaList = basexClient.executeXQuery(BaseXQueries.applyXQueryParams(BaseXQueries.MRSA, xQueryParams))
        return parseXmlAttributeOrderPreserving(mrsaList)
            .map { it.transformEntry("samplingDate", ::toGermanDate) }
//            .map { it.mapKeys { (k, _) -> "page.MRSA.caselist.$k" } }
    }

    suspend fun getMRGNCaselist(xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrgnList = basexClient.executeXQuery(BaseXQueries.applyXQueryParams(BaseXQueries.MRGN, xQueryParams))
        val parsed = parseXmlAttributeOrderPreserving(mrgnList).map {
            //Replace MRGN3 -> 3MRGN, MRGN4 -> 4MRGN
            it.transformEntry("class") { v ->
                v.replace("MRGN3", "3MRGN").replace("MRGN4", "4MRGN")
            }.transformEntry("pathogen") { v ->
                v.replace("Klebsiella pneumoniae ssp pneumoniae", "Klebsiella pneumoniae")
            }.transformEntry("samplingDate", ::toGermanDate)
        }
        //E-Mail von Zentralstelle IfSG: "Doppelte Fälle sind nur zulässig, wenn es sich um unterschiedliche Erreger und MRGN-Klassifikationen handelt"
        val result = parsed.distinctBy {
            val classification = with(it["class"]) {
                when {
                    isNullOrBlank() -> null
                    contains("MRGN3") || contains("3MRGN") -> 3
                    contains("MRGN4") || contains("4MRGN") -> 4
                    else -> null
                }
            }
            Triple(it["caseID"], it["pathogen"], classification)
        }
        return result //.map { it.mapKeys { (k, _) -> "page.MRGN.caselist.$k" } }
    }

    suspend fun getVRECaselist(xQueryParams: XQueryParams): List<Map<String, String>> {
        val vreList = basexClient.executeXQuery(BaseXQueries.applyXQueryParams(BaseXQueries.VRE, xQueryParams))
        return parseXmlAttributeOrderPreserving(vreList)
            .map { it.transformEntry("samplingDate", ::toGermanDate) }
//            .map { it.mapKeys { (k, _) -> "page.VRE.caselist.$k" } }
    }

    //TODO: Übersicht auf Plausibilität checken / checken lassen

    suspend fun getGlobalStatistics(xQueryParams: XQueryParams): GlobalInfo {
        val result = mutableMapOf<CaseType, List<OverviewEntry>>()
        for (caseType in CaseType.entries) {
            val params = Params(xQueryParams, FilterParams(listOf(caseType)))
            result[caseType] = listOf(
                createBaseXOverviewEntry("page.hospitalMetrics.cases", BaseXQueries.Fallzahlen, params),
                createBaseXOverviewEntry("page.hospitalMetrics.days", BaseXQueries.Falltage, params),
            )
        }
        return GlobalInfo(result, LocalDateTime.now().toString())
    }

    suspend fun getMRSAOverview(
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): Map<CaseType, List<OverviewEntry>> {
        val result = mutableMapOf<CaseType, List<OverviewEntry>>()
        for (caseType in CaseType.entries) {
            suspend fun entry(name: String, query: String) =
                createBaseXOverviewEntry(name, query, Params(xQueryParams, FilterParams(listOf(caseType))))

            val filteredCaseList = caseList.filterCaseType(caseType)

            val mrsaTotal = DataProcessor.countMRSATotal(filteredCaseList)
            val mrsaNosokomial = DataProcessor.countMRSANosokomial(filteredCaseList)
            val mrsaImported = DataProcessor.countMRSAImported(filteredCaseList)

            //TODO: Blutkultur-Abfrage hat Vorrang vor den excel sachen => Tabelle mergen.
            result[caseType] = listOf(
                entry("nasalSwabs", BaseXQueries.NasenRachenAbstriche),
                entry("bloodSAureus", BaseXQueries.MSSABK),
                entry("bloodMRSA", BaseXQueries.MRSABK),
                OverviewEntry("numberOfCases", BaseXQueries.MRSA, mrsaTotal),
                OverviewEntry("importedMRSA", BaseXQueries.MRSA, mrsaImported),
                OverviewEntry("nosocomialMRSA", BaseXQueries.MRSA, mrsaNosokomial),
                entry("inpatientDays", BaseXQueries.FalltageMRSA)
            )
        }
        return result

    }

    private fun List<Map<String, String>>.filterCaseType(caseType: CaseType) =
        this.filter { it["caseType"] in caseType.basexName }

    fun getMRGNOverview(
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): Map<CaseType, List<OverviewEntry>> {

        val result = mutableMapOf<CaseType, List<OverviewEntry>>()
        for (caseType in CaseType.entries) {

            val filteredCaseList = caseList.filterCaseType(caseType)

            val mrgn3Cases = DataProcessor.countMRGN3Cases(filteredCaseList)
            val mrgn4Cases = DataProcessor.countMRGN4Cases(filteredCaseList)

            result[caseType] = listOf(
                OverviewEntry("numberOf3MRGN", BaseXQueries.MRGN, mrgn3Cases),
                OverviewEntry("numberOf4MRGN", BaseXQueries.MRGN, mrgn4Cases),
            )
        }
        return result
    }

    suspend fun getVREOverview(
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): Map<CaseType, List<OverviewEntry>> {
        val result = mutableMapOf<CaseType, List<OverviewEntry>>()
        for (caseType in CaseType.entries) {
            suspend fun entry(name: String, query: String) =
                createBaseXOverviewEntry(name, query, Params(xQueryParams, FilterParams(listOf(caseType))))

            val filteredCaseList = caseList.filterCaseType(caseType)


            //TODO E.faecuium total und VRE-E.faecium ist gleich, sollte es aber nicht sein
            //TODO: MRE-Aus Blutkulturen sollte höhere Priorität haben ggü. dem Ort der ersten Abnahme.
            val numEfaecalisResistant = DataProcessor.countVREEfaecalisResistant(filteredCaseList)
            val numEfaeciumTotal = DataProcessor.countVREEfaeciumTotal(filteredCaseList)
            val numEfaeciumResistant = DataProcessor.countVREEfaeciumResistant(filteredCaseList)
            val numOtherCases = DataProcessor.countOtherCases(filteredCaseList)

            result[caseType] = listOf(
                entry("numberOfEFaecalisOverall", BaseXQueries.AnzahlEFaecalis),
                OverviewEntry("numberOfVREEFaecalis", BaseXQueries.VRE, numEfaecalisResistant),
                entry("numberOfEFaeciumOverall", BaseXQueries.AnzahlEFaecium),
                OverviewEntry("numberOfVREEFaecium", BaseXQueries.VRE, numEfaeciumResistant),
                OverviewEntry("otherVRE", BaseXQueries.VRE, numOtherCases),
                entry("numberEFaeciumComplete", BaseXQueries.EfaeciumBK),
                entry("numberOfVREEFaeciumBlood", BaseXQueries.VREBK)
            )
        }
        return result
    }

    suspend fun createBaseXOverviewEntry(name: String, query: String, params: Params): OverviewEntry {
        val result = basexClient.executeXQuery(BaseXQueries.applyParams(query, params))
        return OverviewEntry(name, BaseXQueries.applyParams(query, params), result)
    }
}