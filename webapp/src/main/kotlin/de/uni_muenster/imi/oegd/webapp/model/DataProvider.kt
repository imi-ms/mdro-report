package de.uni_muenster.imi.oegd.webapp.model

import de.uni_muenster.imi.oegd.webapp.parseCsv
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
        return parseCsv(
            mrsaList,
            listOf(
                "page.MRSA.caselist.caseID",
                "page.MRSA.caselist.samplingDate",
                "page.MRSA.caselist.sampleType",
                "page.MRSA.caselist.infection",
                "page.MRSA.caselist.nosocomial",
                "page.MRSA.caselist.sender",
                "page.MRSA.caselist.department",
                "page.MRSA.caselist.spa",
                "page.MRSA.caselist.clustertype"
            )
        )
    }

    suspend fun getMRGNCSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val mrgnList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.MRGN, xQueryParams))
        return parseCsv(
            mrgnList,
            listOf(
                "page.MRGN.caselist.caseID",
                "page.MRGN.caselist.samplingDate",
                "page.MRGN.caselist.sampleType",
                "page.MRGN.caselist.sender",
                "page.MRGN.caselist.department",
                "page.MRGN.caselist.pathogen",
                "page.MRGN.caselist.class",
                "page.MRGN.caselist.piperacillinAndTazobactam",
                "page.MRGN.caselist.cefotaxime",
                "page.MRGN.caselist.cefTAZidime",
                "page.MRGN.caselist.cefepime",
                "page.MRGN.caselist.meropenem",
                "page.MRGN.caselist.imipenem",
                "page.MRGN.caselist.ciprofloxacin"
            )
        )
    }

    suspend fun getVRECSV(baseXClient: IBaseXClient, xQueryParams: XQueryParams): List<Map<String, String>> {
        val vreList = baseXClient.executeXQuery(BaseXQueries.applyParams(BaseXQueries.VRE, xQueryParams))
        return parseCsv(
            vreList,
            listOf(
                "page.VRE.caselist.caseID",
                "page.VRE.caselist.samplingDate",
                "page.VRE.caselist.sampleType",
                "page.VRE.caselist.sender",
                "page.VRE.caselist.department",
                "page.VRE.caselist.pathogen",
                "page.VRE.caselist.linezolid",
                "page.VRE.caselist.tigecylin",
                "page.VRE.caselist.vancomycin",
                "page.VRE.caselist.teicoplanin",
                "page.VRE.caselist.quinupristinAndDalfopristin"
            )
        )
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