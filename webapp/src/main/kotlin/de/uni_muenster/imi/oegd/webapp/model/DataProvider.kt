package de.uni_muenster.imi.oegd.webapp.model

import de.uni_muenster.imi.oegd.webapp.parseXmlAttributeOrderPreserving
import de.uni_muenster.imi.oegd.webapp.toGermanDate
import de.uni_muenster.imi.oegd.webapp.transformEntry
import java.time.LocalDateTime

class DataProvider(val basexClient: IBaseXClient) {
    //TODO: Create constructor with BaseXClient?
    suspend fun getGermInfo(germ: GermType, xQueryParams: XQueryParams): GermInfo {
        return when (germ) {
            GermType.MRSA -> {
                val caseList = getMRSACaselist(xQueryParams)
                val overviewEntry = getMRSAOverview(xQueryParams, caseList)
                GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.MRGN -> {
                val caseList = getMRGNCaselist(xQueryParams)
                val overviewEntry = getMRGNOverview(xQueryParams, caseList)
                GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }

            GermType.VRE -> {
                val caseList = getVRECaselist(xQueryParams)
                val overviewEntry = getVREOverview(xQueryParams, caseList)
                GermInfo(germ.germtype, overviewEntry, caseList, LocalDateTime.now().toString())
            }
        }
    }


    suspend fun getMRSACaselist(xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrsaList = basexClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.MRSA, xQueryParams))
        return parseXmlAttributeOrderPreserving(mrsaList)
            .map { it.transformEntry("samplingDate", ::toGermanDate) }
//            .map { it.mapKeys { (k, _) -> "page.MRSA.caselist.$k" } }
    }

    suspend fun getMRGNCaselist(xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrgnList = basexClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.MRGN, xQueryParams))
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
        val vreList = basexClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.VRE, xQueryParams))
        return parseXmlAttributeOrderPreserving(vreList)
            .map { it.transformEntry("samplingDate", ::toGermanDate) }
//            .map { it.mapKeys { (k, _) -> "page.VRE.caselist.$k" } }
    }


    //TODO: Übersicht auf Plausibilität checken / checken lassen

    suspend fun getGlobalStatistics(xQueryParams: XQueryParams): GlobalInfo {
        return GlobalInfo(
            listOf(
                createBaseXOverviewEntry("page.hospitalMetrics.cases", BaseXQueries.Fallzahlen, xQueryParams),
                createBaseXOverviewEntry("page.hospitalMetrics.days", BaseXQueries.Falltage, xQueryParams),
            ), LocalDateTime.now().toString()
        )
    }

    suspend fun getMRSAOverview(
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        suspend fun entry(name: String, query: String) = createBaseXOverviewEntry(name, query, xQueryParams)

        val mrsaTotal = DataProcessor.countMRSATotal(caseList)
        val mrsaNosokomial = DataProcessor.countMRSANosokomial(caseList)
        val mrsaImported = DataProcessor.countMRSAImported(caseList)

        //TODO: Blutkultur-Abfrage hat Vorrang vor den excel sachen => Tabelle mergen.

        return listOf(
            entry("nasalSwabs", BaseXQueries.NasenRachenAbstriche),
            entry("bloodSAureus", BaseXQueries.MSSABK),
            entry("bloodMRSA", BaseXQueries.MRSABK),
            OverviewEntry("numberOfCases", BaseXQueries.MRSA, "$mrsaTotal"),
            OverviewEntry("importedMRSA", BaseXQueries.MRSA, "$mrsaImported"),
            OverviewEntry("nosocomialMRSA", BaseXQueries.MRSA, "$mrsaNosokomial"),
            entry("inpatientDays", BaseXQueries.FalltageMRSA)
        )
    }

    suspend fun getMRGNOverview(
        xQueryParams: XQueryParams,
        caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        val mrgn3Cases = DataProcessor.countMRGN3Cases(caseList)
        val mrgn4Cases = DataProcessor.countMRGN4Cases(caseList)

        return listOf(
            OverviewEntry("numberOf3MRGN", BaseXQueries.MRGN, "$mrgn3Cases"),
            OverviewEntry("numberOf4MRGN", BaseXQueries.MRGN, "$mrgn4Cases"),
        )
    }

    suspend fun getVREOverview(
        xQueryParams: XQueryParams, caseList: List<Map<String, String>>
    ): List<OverviewEntry> {
        suspend fun entry(name: String, query: String) =
            createBaseXOverviewEntry(name, query, xQueryParams)
        //TODO E.faecuium total und VRE-E.faecium ist gleich, sollte es aber nicht sein
        //TODO: MRE-Aus Blutkulturen sollte höhere Priorität haben ggü. dem Ort der ersten Abnahme.
        val numEfaecalisResistant = DataProcessor.countVREEfaecalisResistant(caseList)
        val numEfaeciumTotal = DataProcessor.countVREEfaeciumTotal(caseList)
        val numEfaeciumResistant = DataProcessor.countVREEfaeciumResistant(caseList)
        val numOtherCases = DataProcessor.countOtherCases(caseList)

        return listOf(
            entry("numberOfEFaecalisOverall", BaseXQueries.AnzahlEFaecalis),
            OverviewEntry("numberOfVREEFaecalis", BaseXQueries.VRE, "$numEfaecalisResistant"),
            entry("numberOfEFaeciumOverall", BaseXQueries.AnzahlEFaecium),
            OverviewEntry("numberOfVREEFaecium", BaseXQueries.VRE, "$numEfaeciumResistant"),
            OverviewEntry("otherVRE", BaseXQueries.VRE, "$numOtherCases"),
            entry("numberEFaeciumComplete", BaseXQueries.EfaeciumBK),
            entry("numberOfVREEFaeciumBlood", BaseXQueries.VREBK)
        )
    }

    suspend fun createBaseXOverviewEntry(name: String, query: String, xQueryParams: XQueryParams): OverviewEntry {
        val query2 = BaseXQueries.applyParams(query, xQueryParams)
        val result = basexClient.executeXQuery(query2)
        return OverviewEntry(name, query2, result)
    }
}